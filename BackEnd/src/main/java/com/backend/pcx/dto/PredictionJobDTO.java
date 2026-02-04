package com.backend.pcx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionJobDTO {
    private Long id;
    private Long segmentId;
    private String segmentName;
    private Long baseTime;
    private Integer horizonSteps;
    private String predictorType;
    private Long costMs;
    private Long createdAt;
    private List<PredictionPointDTO> points;
}
