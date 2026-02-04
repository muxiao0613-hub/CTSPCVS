package com.backend.pcx.predictor;

import com.backend.pcx.entity.CongestionLevel;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class BaselinePredictor implements TrafficPredictor {
    
    @Value("${traffic.prediction.free-speed-threshold:40}")
    private Double freeSpeedThreshold;
    
    @Value("${traffic.prediction.flowing-speed-threshold:25}")
    private Double flowingSpeedThreshold;
    
    @Value("${traffic.prediction.prediction-window-size:6}")
    private Integer windowSize;

    @Override
    public List<PredictionResult> predict(Long segmentId, Long baseTime, Integer horizonSteps, List<FileBasedSpeedRepository.SpeedDataPoint> historicalData) {
        List<PredictionResult> results = new ArrayList<>();
        
        if (historicalData == null || historicalData.isEmpty()) {
            for (int i = 1; i <= horizonSteps; i++) {
                Long ts = baseTime + (i * 10L * 60 * 1000);
                results.add(new PredictionResult(ts, 30.0, CongestionLevel.fromSpeed(30.0, freeSpeedThreshold, flowingSpeedThreshold)));
            }
            return results;
        }
        
        List<FileBasedSpeedRepository.SpeedDataPoint> sortedData = new ArrayList<>(historicalData);
        sortedData.sort(Comparator.comparing(FileBasedSpeedRepository.SpeedDataPoint::getTs));
        
        for (int i = 1; i <= horizonSteps; i++) {
            Long ts = baseTime + (i * 10L * 60 * 1000);
            
            Double predictedSpeed = calculateMovingAverage(sortedData);
            
            predictedSpeed = applyPeakHourAdjustment(predictedSpeed, ts);
            
            CongestionLevel level = CongestionLevel.fromSpeed(predictedSpeed, freeSpeedThreshold, flowingSpeedThreshold);
            
            results.add(new PredictionResult(ts, predictedSpeed, level));
        }
        
        return results;
    }

    private Double calculateMovingAverage(List<FileBasedSpeedRepository.SpeedDataPoint> data) {
        int size = Math.min(windowSize, data.size());
        if (size == 0) {
            return 30.0;
        }
        
        double sum = 0.0;
        int count = 0;
        
        for (int i = data.size() - size; i < data.size(); i++) {
            sum += data.get(i).getSpeed();
            count++;
        }
        
        return sum / count;
    }

    private Double applyPeakHourAdjustment(Double speed, Long ts) {
        long hour = (ts / (60 * 60 * 1000)) % 24;
        
        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
            return speed * 0.8;
        } else if (hour >= 22 || hour <= 5) {
            return speed * 1.2;
        }
        
        return speed;
    }

    @Override
    public String getPredictorType() {
        return "BASELINE";
    }
}
