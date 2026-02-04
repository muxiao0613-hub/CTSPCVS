package com.backend.pcx.service;

import com.backend.pcx.dto.CongestedSegment;
import com.backend.pcx.dto.CongestionLevelDistribution;
import com.backend.pcx.dto.DashboardSummary;
import com.backend.pcx.dto.RegionCongestion;
import com.backend.pcx.entity.CongestionLevel;
import com.backend.pcx.entity.RoadSegment;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import com.backend.pcx.repository.RoadSegmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    private final RoadSegmentRepository roadSegmentRepository;
    private final FileBasedSpeedRepository fileBasedSpeedRepository;
    
    @Value("${traffic.prediction.free-speed-threshold:40}")
    private Double freeSpeedThreshold;
    
    @Value("${traffic.prediction.flowing-speed-threshold:25}")
    private Double flowingSpeedThreshold;

    public DashboardService(RoadSegmentRepository roadSegmentRepository,
                             FileBasedSpeedRepository fileBasedSpeedRepository) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.fileBasedSpeedRepository = fileBasedSpeedRepository;
    }

    public DashboardSummary getSummary() {
        List<RoadSegment> allSegments = roadSegmentRepository.findAll();
        
        Map<Long, Double> segmentAvgSpeeds = new HashMap<>();
        for (RoadSegment segment : allSegments) {
            Integer roadId = segment.getRoadId();
            List<FileBasedSpeedRepository.SpeedDataPoint> data = 
                    fileBasedSpeedRepository.getSpeedData(roadId, null, null, false);
            
            if (!data.isEmpty()) {
                double avgSpeed = data.stream()
                        .mapToDouble(FileBasedSpeedRepository.SpeedDataPoint::getSpeed)
                        .average()
                        .orElse(0.0);
                segmentAvgSpeeds.put(segment.getId(), avgSpeed);
            }
        }
        
        Double todayAvgSpeed = segmentAvgSpeeds.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        long congestedCount = segmentAvgSpeeds.values().stream()
                .filter(speed -> speed < flowingSpeedThreshold)
                .count();
        
        CongestedSegment mostCongested = segmentAvgSpeeds.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(entry -> {
                    RoadSegment segment = allSegments.stream()
                            .filter(s -> s.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (segment == null) return null;
                    CongestionLevel level = CongestionLevel.fromSpeed(
                            entry.getValue(), freeSpeedThreshold, flowingSpeedThreshold);
                    return new CongestedSegment(
                            segment.getId(),
                            segment.getRoadId(),
                            segment.getName(),
                            segment.getRegion(),
                            entry.getValue(),
                            level
                    );
                })
                .orElse(null);
        
        List<CongestedSegment> topCongested = segmentAvgSpeeds.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .map(entry -> {
                    RoadSegment segment = allSegments.stream()
                            .filter(s -> s.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (segment == null) return null;
                    CongestionLevel level = CongestionLevel.fromSpeed(
                            entry.getValue(), freeSpeedThreshold, flowingSpeedThreshold);
                    return new CongestedSegment(
                            segment.getId(),
                            segment.getRoadId(),
                            segment.getName(),
                            segment.getRegion(),
                            entry.getValue(),
                            level
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        CongestionLevelDistribution distribution = new CongestionLevelDistribution(0, 0, 0);
        
        Map<String, RegionCongestion> regionMap = new HashMap<>();
        for (Map.Entry<Long, Double> entry : segmentAvgSpeeds.entrySet()) {
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
            
            RoadSegment segment = allSegments.stream()
                    .filter(s -> s.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (segment != null && segment.getRegion() != null) {
                String region = segment.getRegion();
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
}
