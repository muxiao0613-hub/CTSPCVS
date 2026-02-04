package com.backend.pcx.service;

import com.backend.pcx.dto.RoadSegmentDTO;
import com.backend.pcx.entity.RoadSegment;
import com.backend.pcx.entity.RoadAlias;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import com.backend.pcx.repository.RoadAliasRepository;
import com.backend.pcx.repository.RoadSegmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoadSegmentService {
    
    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadAliasRepository roadAliasRepository;
    private final FileBasedSpeedRepository fileBasedSpeedRepository;

    public RoadSegmentService(RoadSegmentRepository roadSegmentRepository,
                             RoadAliasRepository roadAliasRepository,
                             FileBasedSpeedRepository fileBasedSpeedRepository) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadAliasRepository = roadAliasRepository;
        this.fileBasedSpeedRepository = fileBasedSpeedRepository;
    }

    public Page<RoadSegmentDTO> searchSegments(String keyword, String region, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("roadId").ascending());
        Page<RoadSegment> segments;
        
        if (keyword != null) {
            segments = roadSegmentRepository.search(keyword, region, pageable);
        } else {
            segments = roadSegmentRepository.findAll(pageable);
        }
        
        return segments.map(this::toDTO);
    }

    public RoadSegmentDTO getSegmentById(Long id) {
        Optional<RoadSegment> segment = roadSegmentRepository.findById(id);
        return segment.map(this::toDTO).orElse(null);
    }

    public RoadSegmentDTO getSegmentByRoadId(Integer roadId) {
        return roadSegmentRepository.findByRoadId(roadId)
                .map(this::toDTO)
                .orElse(null);
    }

    public List<RoadSegmentDTO> getAllSegments() {
        return roadSegmentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<RoadSegmentDTO> getSegmentsFromDataSources() {
        List<FileBasedSpeedRepository.DataSource> dataSources = fileBasedSpeedRepository.getDataSources();
        List<Integer> roadIds = new java.util.ArrayList<>();
        
        for (FileBasedSpeedRepository.DataSource source : dataSources) {
            if (source.getTotalRoads() != null) {
                for (int i = 1; i <= source.getTotalRoads(); i++) {
                    if (!roadIds.contains(i)) {
                        roadIds.add(i);
                    }
                }
            }
        }
        
        return roadIds.stream()
                .map(roadId -> {
                    Optional<RoadSegment> segmentOpt = roadSegmentRepository.findByRoadId(roadId);
                    if (segmentOpt.isPresent()) {
                        return toDTO(segmentOpt.get());
                    } else {
                        RoadSegment newSegment = new RoadSegment();
                        newSegment.setRoadId(roadId);
                        newSegment.setName(null);
                        newSegment.setRegion(null);
                        newSegment = roadSegmentRepository.save(newSegment);
                        return toDTO(newSegment);
                    }
                })
                .collect(Collectors.toList());
    }

    private RoadSegmentDTO toDTO(RoadSegment segment) {
        String displayName = segment.getName();
        if (displayName == null || displayName.isEmpty()) {
            Optional<RoadAlias> aliasOpt = roadAliasRepository.findByRoadId(segment.getRoadId());
            if (aliasOpt.isPresent() && aliasOpt.get().getAlias() != null) {
                displayName = aliasOpt.get().getAlias();
            } else {
                displayName = "Road #" + segment.getRoadId();
            }
        }
        
        return new RoadSegmentDTO(
                segment.getId(),
                segment.getRoadId(),
                displayName,
                segment.getRegion()
        );
    }
}
