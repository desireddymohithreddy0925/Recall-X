package com.recallx.recallx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "engineering_experience")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngineeringExperience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    // TODO: Add relationships
}
