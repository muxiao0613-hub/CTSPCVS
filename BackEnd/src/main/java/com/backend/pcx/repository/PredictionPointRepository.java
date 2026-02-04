package com.backend.pcx.repository;

import com.backend.pcx.entity.PredictionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionPointRepository extends JpaRepository<PredictionPoint, Long> {
    List<PredictionPoint> findByJobIdOrderByTsAsc(Long jobId);
}
