package com.backend.pcx.repository;

import com.backend.pcx.entity.SegmentStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SegmentStatisticsRepository extends JpaRepository<SegmentStatistics, Long> {
    Optional<SegmentStatistics> findByRoadId(Integer roadId);
}
