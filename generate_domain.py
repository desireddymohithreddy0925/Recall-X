import os

base_pkg = "src/main/java/com/recallx/recallx"
entities = ["Organization", "User", "SystemApp", "Incident", "Deployment", "EngineeringExperience", "ArchitectureDecision", "TroubleshootingAttempt", "Lesson", "PreventiveAction", "Pattern"]

os.makedirs(f"{base_pkg}/entity", exist_ok=True)
os.makedirs(f"{base_pkg}/repository", exist_ok=True)
os.makedirs(f"{base_pkg}/dto", exist_ok=True)
os.makedirs(f"{base_pkg}/service", exist_ok=True)

entity_template = """package com.recallx.recallx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "{table_name}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {entity_name} {{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    // TODO: Add relationships
}}
"""

repo_template = """package com.recallx.recallx.repository;

import com.recallx.recallx.entity.{entity_name};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface {entity_name}Repository extends JpaRepository<{entity_name}, Long> {{
}}
"""

for entity in entities:
    table_name = "".join(['_' + c.lower() if c.isupper() else c for c in entity]).lstrip('_')
    
    with open(f"{base_pkg}/entity/{entity}.java", "w") as f:
        f.write(entity_template.format(entity_name=entity, table_name=table_name))
        
    with open(f"{base_pkg}/repository/{entity}Repository.java", "w") as f:
        f.write(repo_template.format(entity_name=entity))

print("Scaffolded entities and repositories.")
