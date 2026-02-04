package com.backend.pcx.service;

import com.backend.pcx.dto.CongestedSegment;
import com.backend.pcx.dto.CongestionLevelDistribution;
import com.backend.pcx.dto.DashboardSummary;
import com.backend.pcx.dto.RegionCongestion;
import com.backend.pcx.entity.CongestionLevel;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    
    private final FileBasedSpeedRepository fileBasedSpeedRepository;
    
    @Value("${traffic.prediction.free-speed-threshold:40}")
    private Double freeSpeedThreshold;
    
    @Value("${traffic.prediction.flowing-speed-threshold:25}")
    private Double flowingSpeedThreshold;

    public DashboardService(FileBasedSpeedRepository fileBasedSpeedRepository) {
        this.fileBasedSpeedRepository = fileBasedSpeedRepository;
    }

    public DashboardSummary getSummary() {
        logger.info("开始获取仪表盘数据");
        
        Set<Integer> allRoadIds = fileBasedSpeedRepository.getAllRoadIds();
        logger.info("找到 {} 个路段", allRoadIds.size());
        
        Map<Integer, Double> segmentAvgSpeeds = new HashMap<>();
        for (Integer roadId : allRoadIds) {
            List<FileBasedSpeedRepository.SpeedDataPoint> data = 
                    fileBasedSpeedRepository.getSpeedData(roadId, null, null, false);
            
            if (!data.isEmpty()) {
                double avgSpeed = data.stream()
                        .mapToDouble(FileBasedSpeedRepository.SpeedDataPoint::getSpeed)
                        .average()
                        .orElse(0.0);
                segmentAvgSpeeds.put(roadId, avgSpeed);
            }
        }
        
        logger.info("成功计算 {} 个路段的平均速度", segmentAvgSpeeds.size());
        
        Double todayAvgSpeed = segmentAvgSpeeds.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        logger.info("全部数据均速: {}", todayAvgSpeed);
        
        long congestedCount = segmentAvgSpeeds.values().stream()
                .filter(speed -> speed < flowingSpeedThreshold)
                .count();
        
        logger.info("拥堵路段数: {}", congestedCount);
        
        CongestedSegment mostCongested = segmentAvgSpeeds.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(entry -> {
                    Integer roadId = entry.getKey();
                    CongestionLevel level = CongestionLevel.fromSpeed(
                            entry.getValue(), freeSpeedThreshold, flowingSpeedThreshold);
                    return new CongestedSegment(
                            null,
                            roadId,
                            "路段" + roadId,
                            getRegionByRoadId(roadId),
                            entry.getValue(),
                            level
                    );
                })
                .orElse(null);
        
        List<CongestedSegment> topCongested = segmentAvgSpeeds.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .map(entry -> {
                    Integer roadId = entry.getKey();
                    CongestionLevel level = CongestionLevel.fromSpeed(
                            entry.getValue(), freeSpeedThreshold, flowingSpeedThreshold);
                    return new CongestedSegment(
                            null,
                            roadId,
                            "路段" + roadId,
                            getRegionByRoadId(roadId),
                            entry.getValue(),
                            level
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        CongestionLevelDistribution distribution = new CongestionLevelDistribution(0, 0, 0);
        
        Map<String, RegionCongestion> regionMap = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : segmentAvgSpeeds.entrySet()) {
            CongestionLevel level = CongestionLevel.fromSpeed(
                    entry.getValue(), freeSpeedThreshold, flowingSpeedThreshold);
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
            
            String region = getRegionByRoadId(entry.getKey());
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
        
        return new DashboardSummary(
                todayAvgSpeed,
                (int) congestedCount,
                mostCongested,
                regionCongestions,
                topCongested,
                distribution
        );
    }
    
    private String getRegionByRoadId(Integer roadId) {
        if (roadId <= 50) {
            return "朝阳区";
        } else if (roadId <= 100) {
            return "海淀区";
        } else if (roadId <= 150) {
            return "西城区";
        } else if (roadId <= 200) {
            return "东城区";
        } else {
            return "丰台区";
        }
    }
}
