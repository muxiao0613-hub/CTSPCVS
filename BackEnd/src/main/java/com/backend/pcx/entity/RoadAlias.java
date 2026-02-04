package com.backend.pcx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "road_alias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadAlias {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer roadId;

    @Column
    private String alias;
}
