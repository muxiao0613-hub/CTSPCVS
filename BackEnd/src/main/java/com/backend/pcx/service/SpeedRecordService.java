package com.backend.pcx.service;

import com.backend.pcx.dto.SpeedRecordDTO;
import com.backend.pcx.entity.RoadSegment;
import com.backend.pcx.entity.RoadAlias;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import com.backend.pcx.repository.RoadAliasRepository;
import com.backend.pcx.repository.RoadSegmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SpeedRecordService {
    
    private final FileBasedSpeedRepository fileBasedSpeedRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadAliasRepository roadAliasRepository;

    public SpeedRecordService(FileBasedSpeedRepository fileBasedSpeedRepository,
                             RoadSegmentRepository roadSegmentRepository,
                             RoadAliasRepository roadAliasRepository) {
        this.fileBasedSpeedRepository = fileBasedSpeedRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadAliasRepository = roadAliasRepository;
    }

    public List<SpeedRecordDTO> getSpeedRecords(Long segmentId, Long from, Long to) {
        Optional<RoadSegment> segmentOpt = roadSegmentRepository.findById(segmentId);
        if (segmentOpt.isEmpty()) {
            return List.of();
        }
        
        RoadSegment segment = segmentOpt.get();
        Integer roadId = segment.getRoadId();
        
        List<FileBasedSpeedRepository.SpeedDataPoint> data = 
                fileBasedSpeedRepository.getSpeedData(roadId, from, to, false);
        
        return data.stream()
                .map(p -> toDTO(p, segment))
                .collect(Collectors.toList());
    }

    public List<SpeedRecordDTO> getRecentSpeedRecords(Long segmentId, Long fromTs) {
        Optional<RoadSegment> segmentOpt = roadSegmentRepository.findById(segmentId);
        if (segmentOpt.isEmpty()) {
            return List.of();
        }
        
        RoadSegment segment = segmentOpt.get();
        Integer roadId = segment.getRoadId();
        
        List<FileBasedSpeedRepository.SpeedDataPoint> data = 
                fileBasedSpeedRepository.getSpeedData(roadId, fromTs, null, false);
        
        return data.stream()
                .map(p -> toDTO(p, segment))
                .collect(Collectors.toList());
    }
    
    public List<SpeedRecordDTO> getRecentSpeedRecordsByLimit(Long segmentId, int limit) {
        Optional<RoadSegment> segmentOpt = roadSegmentRepository.findById(segmentId);
        if (segmentOpt.isEmpty()) {
            return List.of();
        }
        
        RoadSegment segment = segmentOpt.get();
        Integer roadId = segment.getRoadId();
        
        List<FileBasedSpeedRepository.SpeedDataPoint> data = 
                fileBasedSpeedRepository.getRecentSpeedData(roadId, limit);
        
        return data.stream()
                .map(p -> toDTO(p, segment))
                .collect(Collectors.toList());
    }

    private SpeedRecordDTO toDTO(FileBasedSpeedRepository.SpeedDataPoint point, RoadSegment segment) {
        String displayName = segment.getName();
        if (displayName == null || displayName.isEmpty()) {
            Optional<RoadAlias> aliasOpt = roadAliasRepository.findByRoadId(segment.getRoadId());
            if (aliasOpt.isPresent() && aliasOpt.get().getAlias() != null) {
                displayName = aliasOpt.get().getAlias();
            } else {
                displayName = "Road #" + segment.getRoadId();
            }
        }
        
        return new SpeedRecordDTO(
                null,
                segment.getId(),
                displayName,
                point.getTs(),
                point.getSpeed()
        );
    }
}
