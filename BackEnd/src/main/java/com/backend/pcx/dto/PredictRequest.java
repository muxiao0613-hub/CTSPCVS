package com.backend.pcx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictRequest {
    private Long segmentId;
    private Long baseTime;
    private Integer horizonSteps;
}
