package com.backend.pcx.repository;

import com.backend.pcx.entity.PredictionJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionJobRepository extends JpaRepository<PredictionJob, Long> {
    Page<PredictionJob> findBySegmentIdOrderByCreatedAtDesc(Long segmentId, Pageable pageable);
    
    Page<PredictionJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<PredictionJob> findTop10BySegmentIdOrderByCreatedAtDesc(Long segmentId);
}
