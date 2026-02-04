package com.backend.pcx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadSegmentDTO {
    private Long id;
    private Integer roadId;
    private String name;
    private String region;
}
