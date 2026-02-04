package com.backend.pcx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prediction_job")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private RoadSegment segment;

    @Column(nullable = false)
    private Long baseTime;

    @Column(nullable = false)
    private Integer horizonSteps;

    @Column(nullable = false)
    private String predictorType;

    @Column
    private Long costMs;

    @Column(nullable = false)
    private Long createdAt;
}
