package com.backend.pcx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeedRecordDTO {
    private Long id;
    private Long segmentId;
    private String segmentName;
    private Long ts;
    private Double speed;
}
