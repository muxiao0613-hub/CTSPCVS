package com.backend.pcx.predictor;

import com.backend.pcx.repository.FileBasedSpeedRepository;

import java.util.List;

public interface TrafficPredictor {
    List<PredictionResult> predict(Long segmentId, Long baseTime, Integer horizonSteps, List<FileBasedSpeedRepository.SpeedDataPoint> historicalData);
    
    String getPredictorType();
}
