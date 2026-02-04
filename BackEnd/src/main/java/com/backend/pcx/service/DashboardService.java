package com.backend.pcx.service;

import com.backend.pcx.dto.CongestedSegment;
import com.backend.pcx.dto.CongestionLevelDistribution;
import com.backend.pcx.dto.DashboardSummary;
import com.backend.pcx.dto.RegionCongestion;
import com.backend.pcx.entity.CongestionLevel;
import com.backend.pcx.entity.SegmentStatistics;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import com.backend.pcx.repository.SegmentStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    private final FileBasedSpeedRepository fileBasedSpeedRepository;
    private final SegmentStatisticsRepository segmentStatisticsRepository;
    
    @Value("${traffic.prediction.free-speed-threshold:40}")
    private Double freeSpeedThreshold;
    
    @Value("${traffic.prediction.flowing-speed-threshold:25}")
    private Double flowingSpeedThreshold;

    public DashboardService(FileBasedSpeedRepository fileBasedSpeedRepository,
                         SegmentStatisticsRepository segmentStatisticsRepository) {
        this.fileBasedSpeedRepository = fileBasedSpeedRepository;
        this.segmentStatisticsRepository = segmentStatisticsRepository;
    }

    public DashboardSummary getSummary() {
        logger.info("开始获取仪表盘数据");
        
        List<SegmentStatistics> allStats = segmentStatisticsRepository.findAll();
        
        if (allStats.isEmpty()) {
            logger.warn("数据库中没有统计数据，需要先导入数据");
            return new DashboardSummary(
                    0.0,
                    0,
                    null,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new CongestionLevelDistribution(0, 0, 0)
            );
        }
        
        logger.info("从数据库加载 {} 个路段的统计数据", allStats.size());
        
        Map<Integer, Double> segmentAvgSpeeds = new HashMap<>();
        for (SegmentStatistics stats : allStats) {
            segmentAvgSpeeds.put(stats.getRoadId(), stats.getAvgSpeed());
        }
        
        Double todayAvgSpeed = segmentAvgSpeeds.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        logger.info("全部数据均速: {}", todayAvgSpeed);
        
        long congestedCount = segmentAvgSpeeds.values().stream()
                .filter(speed -> speed < flowingSpeedThreshold)
                .count();
        
        logger.info("拥堵路段数: {}", congestedCount);
        
        CongestedSegment mostCongested = allStats.stream()
                .min(Comparator.comparing(SegmentStatistics::getAvgSpeed))
                .map(stats -> {
                    CongestionLevel level = CongestionLevel.valueOf(stats.getCongestionLevel());
                    return new CongestedSegment(
                            null,
                            stats.getRoadId(),
                            stats.getName(),
                            stats.getRegion(),
                            stats.getAvgSpeed(),
                            level
                    );
                })
                .orElse(null);
        
        List<CongestedSegment> topCongested = allStats.stream()
                .sorted(Comparator.comparing(SegmentStatistics::getAvgSpeed))
                .limit(5)
                .map(stats -> {
                    CongestionLevel level = CongestionLevel.valueOf(stats.getCongestionLevel());
                    return new CongestedSegment(
                            null,
                            stats.getRoadId(),
                            stats.getName(),
                            stats.getRegion(),
                            stats.getAvgSpeed(),
                            level
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        CongestionLevelDistribution distribution = new CongestionLevelDistribution(0, 0, 0);
        
        Map<String, RegionCongestion> regionMap = new HashMap<>();
        for (SegmentStatistics stats : allStats) {
            CongestionLevel level = CongestionLevel.valueOf(stats.getCongestionLevel());
            switch (level) {
                case FREE:
                    distribution.setFreeCount(distribution.getFreeCount() + 1);
                    break;
                case FLOWING:
                    distribution.setFlowingCount(distribution.getFlowingCount() + 1);
                    break;
                case CONGESTED:
                    distribution.setCongestedCount(distribution.getCongestedCount() + 1);
                    break;
            }
            
            String region = stats.getRegion();
            RegionCongestion regionCongestion = regionMap.get(region);
            if (regionCongestion == null) {
                regionCongestion = new RegionCongestion(region, 0, 0, 0);
                regionMap.put(region, regionCongestion);
            }
            switch (level) {
                case FREE:
                    regionCongestion.setFreeCount(regionCongestion.getFreeCount() + 1);
                    break;
                case FLOWING:
                    regionCongestion.setFlowingCount(regionCongestion.getFlowingCount() + 1);
                    break;
                case CONGESTED:
                    regionCongestion.setCongestedCount(regionCongestion.getCongestedCount() + 1);
                    break;
            }
        }
        
        List<RegionCongestion> regionCongestions = new ArrayList<>(regionMap.values());
        
        DashboardSummary result = new DashboardSummary(
                todayAvgSpeed,
                (int) congestedCount,
                mostCongested,
                regionCongestions,
                topCongested,
                distribution
        );
        
        return result;
    }
}
