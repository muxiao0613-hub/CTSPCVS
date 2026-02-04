package com.backend.pcx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "speed_record", indexes = {
    @Index(name = "idx_segment_ts", columnList = "segment_id, ts")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeedRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private RoadSegment segment;

    @Column(nullable = false)
    private Long ts;

    @Column(nullable = false)
    private Double speed;
}
