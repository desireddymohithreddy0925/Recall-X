package com.recallx.recallx.repository;

import com.recallx.recallx.entity.ArchitectureDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchitectureDecisionRepository extends JpaRepository<ArchitectureDecision, Long> {
}
