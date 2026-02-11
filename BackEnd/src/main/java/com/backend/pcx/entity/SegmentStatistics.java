package com.backend.pcx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "segment_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SegmentStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer roadId;

    @Column
    private String name;

    @Column
    private String region;

    @Column(nullable = false)
    private Double avgSpeed;

    @Column
    private Integer dataPointCount;

    @Column
    private String congestionLevel;

    @Column(nullable = false)
    private Long createdAt;

    @Column
    private Long updatedAt;
}
