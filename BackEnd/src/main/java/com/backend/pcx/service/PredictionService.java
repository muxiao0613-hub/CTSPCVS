package com.backend.pcx.service;

import com.backend.pcx.dto.PredictRequest;
import com.backend.pcx.dto.PredictionJobDTO;
import com.backend.pcx.dto.PredictionPointDTO;
import com.backend.pcx.entity.PredictionJob;
import com.backend.pcx.entity.PredictionPoint;
import com.backend.pcx.entity.RoadSegment;
import com.backend.pcx.predictor.BaselinePredictor;
import com.backend.pcx.predictor.PredictionResult;
import com.backend.pcx.predictor.TrafficPredictor;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import com.backend.pcx.repository.PredictionJobRepository;
import com.backend.pcx.repository.PredictionPointRepository;
import com.backend.pcx.repository.RoadSegmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PredictionService {
    
    private final PredictionJobRepository predictionJobRepository;
    private final PredictionPointRepository predictionPointRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final FileBasedSpeedRepository fileBasedSpeedRepository;
    private final BaselinePredictor baselinePredictor;
    
    @Value("${traffic.prediction.free-speed-threshold:40}")
    private Double freeSpeedThreshold;
    
    @Value("${traffic.prediction.flowing-speed-threshold:25}")
    private Double flowingSpeedThreshold;
    
    @Value("${traffic.prediction.prediction-window-size:6}")
    private Integer predictionWindowSize;

    public PredictionService(PredictionJobRepository predictionJobRepository,
                             PredictionPointRepository predictionPointRepository,
                             RoadSegmentRepository roadSegmentRepository,
                             FileBasedSpeedRepository fileBasedSpeedRepository,
                             BaselinePredictor baselinePredictor) {
        this.predictionJobRepository = predictionJobRepository;
        this.predictionPointRepository = predictionPointRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.fileBasedSpeedRepository = fileBasedSpeedRepository;
        this.baselinePredictor = baselinePredictor;
    }

    @Transactional
    public PredictionJobDTO predict(PredictRequest request) {
        RoadSegment segment = roadSegmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new IllegalArgumentException("Segment not found"));
        
        TrafficPredictor predictor = baselinePredictor;
        
        Integer roadId = segment.getRoadId();
        
        Long baseTime = request.getBaseTime();
        if (baseTime == null) {
            List<FileBasedSpeedRepository.SpeedDataPoint> recentData = 
                    fileBasedSpeedRepository.getRecentSpeedData(roadId, 1);
            if (recentData.isEmpty()) {
                throw new IllegalArgumentException("No data available for this road");
            }
            baseTime = recentData.get(recentData.size() - 1).getTs();
        }
        
        Long historicalFrom = baseTime - (predictionWindowSize * 10 * 60 * 1000L);
        List<FileBasedSpeedRepository.SpeedDataPoint> historicalData = 
                fileBasedSpeedRepository.getSpeedData(roadId, historicalFrom, baseTime, false);
        
        if (historicalData.size() < predictionWindowSize) {
            throw new IllegalArgumentException("Not enough historical data for prediction");
        }
        
        long startTime = System.currentTimeMillis();
        List<PredictionResult> results = predictor.predict(
                segment.getId(),
                baseTime,
                request.getHorizonSteps(),
                historicalData
        );
        long costMs = System.currentTimeMillis() - startTime;
        
        PredictionJob job = new PredictionJob();
        job.setSegment(segment);
        job.setBaseTime(baseTime);
        job.setHorizonSteps(request.getHorizonSteps());
        job.setPredictorType(predictor.getPredictorType());
        job.setCostMs(costMs);
        job.setCreatedAt(System.currentTimeMillis());
        job = predictionJobRepository.save(job);
        
        final PredictionJob savedJob = job;
        List<PredictionPoint> points = results.stream().map(result -> {
            PredictionPoint point = new PredictionPoint();
            point.setJob(savedJob);
            point.setTs(result.getTs());
            point.setPredictedSpeed(result.getPredictedSpeed());
            point.setCongestionLevel(result.getCongestionLevel());
            return point;
        }).collect(Collectors.toList());
        
        predictionPointRepository.saveAll(points);
        
        return toDTO(job, points);
    }

    public Page<PredictionJobDTO> getPredictionJobs(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return predictionJobRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(job -> {
                    List<PredictionPoint> points = predictionPointRepository.findByJobIdOrderByTsAsc(job.getId());
                    return toDTO(job, points);
                });
    }

    public Page<PredictionJobDTO> getPredictionJobsBySegment(Long segmentId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return predictionJobRepository.findBySegmentIdOrderByCreatedAtDesc(segmentId, pageable)
                .map(job -> {
                    List<PredictionPoint> points = predictionPointRepository.findByJobIdOrderByTsAsc(job.getId());
                    return toDTO(job, points);
                });
    }

    public PredictionJobDTO getPredictionJob(Long jobId) {
        PredictionJob job = predictionJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return null;
        }
        List<PredictionPoint> points = predictionPointRepository.findByJobIdOrderByTsAsc(jobId);
        return toDTO(job, points);
    }

    private PredictionJobDTO toDTO(PredictionJob job, List<PredictionPoint> points) {
        List<PredictionPointDTO> pointDTOs = points.stream().map(point ->
                new PredictionPointDTO(
                        point.getId(),
                        point.getTs(),
                        point.getPredictedSpeed(),
                        point.getCongestionLevel()
                )
        ).collect(Collectors.toList());
        
        String segmentName = job.getSegment().getName();
        if (segmentName == null || segmentName.isEmpty()) {
            segmentName = "Road #" + job.getSegment().getRoadId();
        }
        
        return new PredictionJobDTO(
                job.getId(),
                job.getSegment().getId(),
                segmentName,
                job.getBaseTime(),
                job.getHorizonSteps(),
                job.getPredictorType(),
                job.getCostMs(),
                job.getCreatedAt(),
                pointDTOs
        );
    }
}
