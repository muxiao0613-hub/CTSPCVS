package com.backend.pcx.repository;

import com.backend.pcx.entity.RoadAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoadAliasRepository extends JpaRepository<RoadAlias, Long> {
    Optional<RoadAlias> findByRoadId(Integer roadId);
}
