--liquibase formatted sql

--changeset recallx:001-schema
--comment: The MVP schema. The database is the system of record; Hindsight holds the same records as memory. seeded = 1 marks the simulated history; POST /api/admin/demo/reset deletes every row with seeded = 0.
CREATE TABLE service_app (
    id       VARCHAR(64) NOT NULL PRIMARY KEY,
    replicas INT         NOT NULL
);

CREATE TABLE config_key (
    key_name      VARCHAR(191) NOT NULL PRIMARY KEY,
    service_id    VARCHAR(64)  NOT NULL,
    current_value VARCHAR(255) NOT NULL,
    description   VARCHAR(500),
    CONSTRAINT fk_config_key_service FOREIGN KEY (service_id) REFERENCES service_app (id)
);

CREATE TABLE deployment (
    id          VARCHAR(32) NOT NULL PRIMARY KEY,
    service_id  VARCHAR(64) NOT NULL,
    version     VARCHAR(32) NOT NULL,
    deployed_at DATETIME(3) NOT NULL,
    retained_at DATETIME(3) NULL,
    seeded      TINYINT(1)  NOT NULL DEFAULT 0,
    CONSTRAINT fk_deployment_service FOREIGN KEY (service_id) REFERENCES service_app (id)
);

CREATE TABLE deployment_change (
    deployment_id VARCHAR(32)  NOT NULL,
    key_name      VARCHAR(191) NOT NULL,
    old_value     VARCHAR(255) NOT NULL,
    new_value     VARCHAR(255) NOT NULL,
    PRIMARY KEY (deployment_id, key_name),
    CONSTRAINT fk_change_deployment FOREIGN KEY (deployment_id) REFERENCES deployment (id)
);

CREATE TABLE incident (
    id          VARCHAR(32) NOT NULL PRIMARY KEY,
    service_id  VARCHAR(64) NOT NULL,
    severity    VARCHAR(8)  NOT NULL,
    started_at  DATETIME(3) NOT NULL,
    resolved_at DATETIME(3) NULL,
    symptom     TEXT        NOT NULL,
    root_cause  TEXT        NULL,
    lesson      TEXT        NULL,
    caused_by   VARCHAR(32) NULL,
    retained_at DATETIME(3) NULL,
    seeded      TINYINT(1)  NOT NULL DEFAULT 0,
    CONSTRAINT ck_incident_severity CHECK (severity IN ('SEV1', 'SEV2', 'SEV3')),
    CONSTRAINT fk_incident_service FOREIGN KEY (service_id) REFERENCES service_app (id),
    CONSTRAINT fk_incident_deployment FOREIGN KEY (caused_by) REFERENCES deployment (id)
);

CREATE TABLE incident_key (
    incident_id VARCHAR(32)  NOT NULL,
    key_name    VARCHAR(191) NOT NULL,
    PRIMARY KEY (incident_id, key_name),
    CONSTRAINT fk_incident_key_incident FOREIGN KEY (incident_id) REFERENCES incident (id),
    CONSTRAINT fk_incident_key_key FOREIGN KEY (key_name) REFERENCES config_key (key_name)
);

CREATE TABLE troubleshooting_attempt (
    incident_id VARCHAR(32)  NOT NULL,
    seq         INT          NOT NULL,
    action      VARCHAR(500) NOT NULL,
    outcome     VARCHAR(16)  NOT NULL,
    note        VARCHAR(500) NULL,
    PRIMARY KEY (incident_id, seq),
    CONSTRAINT ck_attempt_outcome CHECK (outcome IN ('FAILED', 'PARTIAL', 'RESOLVED')),
    CONSTRAINT fk_attempt_incident FOREIGN KEY (incident_id) REFERENCES incident (id)
);

CREATE TABLE architecture_decision (
    id                 VARCHAR(32)  NOT NULL PRIMARY KEY,
    service_id         VARCHAR(64)  NOT NULL,
    key_name           VARCHAR(191) NOT NULL,
    decided_at         DATETIME(3)  NOT NULL,
    decision           TEXT         NOT NULL,
    reason             TEXT         NOT NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    linked_incident_id VARCHAR(32)  NULL,
    retained_at        DATETIME(3)  NULL,
    seeded             TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT ck_decision_status CHECK (status IN ('ACTIVE', 'SUPERSEDED')),
    CONSTRAINT fk_decision_service FOREIGN KEY (service_id) REFERENCES service_app (id),
    CONSTRAINT fk_decision_key FOREIGN KEY (key_name) REFERENCES config_key (key_name),
    CONSTRAINT fk_decision_incident FOREIGN KEY (linked_incident_id) REFERENCES incident (id)
);
CREATE INDEX ix_decision_key_status ON architecture_decision (key_name, status);

CREATE TABLE warning (
    id                   VARCHAR(32)   NOT NULL PRIMARY KEY,
    deployment_id        VARCHAR(32)   NOT NULL,
    key_name             VARCHAR(191)  NOT NULL,
    created_at           DATETIME(3)   NOT NULL,
    kind                 VARCHAR(16)   NOT NULL,
    severity             VARCHAR(8)    NULL,
    summary              TEXT          NOT NULL,
    cited_refs           VARCHAR(1000) NOT NULL,
    failed_attempts      TEXT          NULL,
    prior_false_positive TEXT          NULL,
    recommendation       TEXT          NULL,
    status               VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    verdict_reason       TEXT          NULL,
    decided_at           DATETIME(3)   NULL,
    retained_at          DATETIME(3)   NULL,
    seeded               TINYINT(1)    NOT NULL DEFAULT 0,
    CONSTRAINT ck_warning_kind CHECK (kind IN ('DECISION_GUARD', 'HISTORY')),
    CONSTRAINT ck_warning_status CHECK (status IN ('PENDING', 'USEFUL', 'FALSE_POSITIVE', 'IGNORED')),
    CONSTRAINT fk_warning_deployment FOREIGN KEY (deployment_id) REFERENCES deployment (id)
);
CREATE INDEX ix_warning_created ON warning (created_at);
