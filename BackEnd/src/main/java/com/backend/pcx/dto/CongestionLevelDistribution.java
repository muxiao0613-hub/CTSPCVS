package com.backend.pcx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CongestionLevelDistribution {
    private Integer freeCount;
    private Integer flowingCount;
    private Integer congestedCount;
}
