package com.backend.pcx.dto;

import com.backend.pcx.entity.ImportJob.ImportStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobDTO {
    private Long id;
    private String filename;
    private ImportStatus status;
    private Integer totalRows;
    private Integer successRows;
    private Integer failRows;
    private String errorMessage;
    private Long createdAt;
    private Long finishedAt;
}
