package com.backend.pcx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prediction_point")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private PredictionJob job;

    @Column(nullable = false)
    private Long ts;

    @Column(nullable = false)
    private Double predictedSpeed;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CongestionLevel congestionLevel;
}
