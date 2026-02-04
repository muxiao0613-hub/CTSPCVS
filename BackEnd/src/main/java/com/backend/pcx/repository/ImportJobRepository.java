package com.backend.pcx.repository;

import com.backend.pcx.entity.ImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    Page<ImportJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
