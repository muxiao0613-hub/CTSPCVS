package com.backend.pcx.dto;

import com.backend.pcx.entity.CongestionLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CongestedSegment {
    private Long id;
    private Integer roadId;
    private String name;
    private String region;
    private Double avgSpeed;
    private CongestionLevel congestionLevel;
}
