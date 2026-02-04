package com.backend.pcx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private Double todayAvgSpeed;
    private Integer congestedSegments;
    private CongestedSegment mostCongestedSegment;
    private List<RegionCongestion> regionCongestions;
    private List<CongestedSegment> topCongestedSegments;
    private CongestionLevelDistribution congestionLevelDistribution;
}
