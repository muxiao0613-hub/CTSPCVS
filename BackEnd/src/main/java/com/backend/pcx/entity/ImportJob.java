package com.backend.pcx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "import_job")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportStatus status;

    @Column
    private Integer totalRows;

    @Column
    private Integer successRows;

    @Column
    private Integer failRows;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private Long createdAt;

    @Column
    private Long finishedAt;

    public enum ImportStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
