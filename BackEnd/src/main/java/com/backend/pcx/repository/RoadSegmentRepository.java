package com.backend.pcx.repository;

import com.backend.pcx.entity.RoadSegment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {
    Optional<RoadSegment> findByRoadId(Integer roadId);
    
    @Query("SELECT s FROM RoadSegment s WHERE " +
           "(:keyword IS NULL OR s.name LIKE %:keyword% OR CAST(s.roadId AS string) LIKE %:keyword%) AND " +
           "(:region IS NULL OR s.region = :region)")
    Page<RoadSegment> search(@Param("keyword") String keyword, @Param("region") String region, Pageable pageable);
}
