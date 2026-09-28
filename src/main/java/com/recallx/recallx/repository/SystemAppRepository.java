package com.recallx.recallx.repository;

import com.recallx.recallx.entity.SystemApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemAppRepository extends JpaRepository<SystemApp, Long> {
}
