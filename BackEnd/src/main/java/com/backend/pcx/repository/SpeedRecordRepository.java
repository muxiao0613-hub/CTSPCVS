package com.backend.pcx.repository;

import com.backend.pcx.entity.SpeedRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeedRecordRepository extends JpaRepository<SpeedRecord, Long> {
    List<SpeedRecord> findBySegmentIdOrderByTsAsc(Long segmentId);

    @Query("SELECT sr FROM SpeedRecord sr WHERE sr.segment.id = :segmentId AND sr.ts >= :fromTs AND sr.ts <= :toTs ORDER BY sr.ts ASC")
    List<SpeedRecord> findBySegmentIdAndTimeRange(@Param("segmentId") Long segmentId, 
                                                   @Param("fromTs") Long fromTs, 
                                                   @Param("toTs") Long toTs);

    @Query("SELECT sr FROM SpeedRecord sr WHERE sr.segment.id = :segmentId AND sr.ts >= :fromTs ORDER BY sr.ts ASC")
    Page<SpeedRecord> findBySegmentIdAfter(@Param("segmentId") Long segmentId, @Param("fromTs") Long fromTs, Pageable pageable);

    @Query("SELECT sr FROM SpeedRecord sr WHERE sr.segment.id = :segmentId AND sr.ts >= :fromTs ORDER BY sr.ts ASC")
    List<SpeedRecord> findRecentBySegmentId(@Param("segmentId") Long segmentId, @Param("fromTs") Long fromTs);

    void deleteBySegmentId(Long segmentId);
}
