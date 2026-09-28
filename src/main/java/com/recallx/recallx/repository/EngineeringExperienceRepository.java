package com.recallx.recallx.repository;

import com.recallx.recallx.entity.EngineeringExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EngineeringExperienceRepository extends JpaRepository<EngineeringExperience, Long> {
}
