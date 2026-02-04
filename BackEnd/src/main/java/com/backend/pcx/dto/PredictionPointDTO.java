package com.backend.pcx.dto;

import com.backend.pcx.entity.CongestionLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionPointDTO {
    private Long id;
    private Long ts;
    private Double predictedSpeed;
    private CongestionLevel congestionLevel;
}
