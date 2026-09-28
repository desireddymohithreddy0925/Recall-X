# RECALL-X

RECALL-X is an AI-powered organizational memory platform designed to transform engineering experiences into persistent organizational knowledge. It focuses on remembering incidents, deployments, and architectural decisions, helping engineers connect past fixes and failures to present challenges.

## Architecture Overview

**Backend:**
- Java 21, Spring Boot
- Spring Data JPA, Spring Security, Spring Web
- MySQL with Liquibase for schema migrations
- Clean Architecture (controllers, services, repositories, dtos, entities)

**Frontend:**
- React, Vite, Tailwind CSS

**AI & Memory:**
- Hindsight as the persistent organizational memory layer
- Claude API / Anthropic SDK

## Prerequisites

- Java 21
- Maven
- MySQL Server

## Environment Variables

Copy `.env.example` to your system environment or IDE configuration. Do NOT commit real secrets to the repository.

See `.env.example` for details.

## Running Locally

To run the Spring Boot application locally:
```bash
./mvnw spring-boot:run
```

## Running Tests

To run unit and integration tests:
```bash
./mvnw test
```

## Endpoints

- **Actuator Health:** `http://localhost:8080/actuator/health`
- **Application Health:** `http://localhost:8080/api/health`
- **Swagger/OpenAPI UI:** `http://localhost:8080/swagger-ui.html`
