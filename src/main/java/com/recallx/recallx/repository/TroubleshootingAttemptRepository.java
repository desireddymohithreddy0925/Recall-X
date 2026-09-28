package com.recallx.recallx.repository;

import com.recallx.recallx.entity.TroubleshootingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TroubleshootingAttemptRepository extends JpaRepository<TroubleshootingAttempt, Long> {
}
