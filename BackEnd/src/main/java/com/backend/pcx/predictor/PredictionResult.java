package com.backend.pcx.predictor;

import com.backend.pcx.entity.CongestionLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult {
    private Long ts;
    private Double predictedSpeed;
    private CongestionLevel congestionLevel;
}
