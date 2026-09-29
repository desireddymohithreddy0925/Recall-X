# RECALL-X: Implementation Plan (v3)

This plan takes the repository from its current scaffold to the build we demo on 3 October. It covers the project structure, configuration, every code change and fix, tests, the demo and the submission.

It replaces v2 (the plan inside `recallx-code-pack.zip`) and the build sections of the MVP documentation. Where this plan says nothing, the MVP documentation still applies, in particular its data model (section 7), retain templates (section 8) and UI details (section 12).

Priority labels: **Must** means the demo depends on it. **Should** means build it if time allows. **Stretch** means only after every Must works.

---

## 0. Build status (29 September)

**Done and verified**

- Phases 1 to 7 are built: the restructure, configuration, every backend change in section 7, the seed changes, the frontend (Ask, Deploy check, Dashboard, New incident), CI, the smoke test and the docs (README, `docs/HINDSIGHT.md`, `docs/DEMO.md`).
- `./mvnw test`: 52 tests pass, with Hindsight and the model mocked.
- Liquibase creates the schema and loads the seed on a real MySQL 8.0.44.
- `tools/smoke_test.py` against the running app with no Hindsight or Groq key: 27 checks pass and 0 fail, with one warning (no Groq key). This covers the resilience rows of section 14.
- `npm run build` passes. The Vite dev server proxies `/api` to the backend.

- Phase 0 against real Hindsight Cloud: every check passed (`docs/SPIKE_RESULTS.md`). Its values are in `.env`, and the quiet demo key is now `management.endpoint.health.show-details`.
- `tools/smoke_test.py` with real Hindsight and Gemini: 33 checks pass, 0 fail, 0 warnings. A deploy check takes about 9 seconds and an Ask about 7.

**Still to do**

1. Look through every screen in a browser at 125% zoom.
2. Move from the throwaway test database to your own MySQL (README, "Run it locally").
3. Watch Hindsight credit use: each reflect call used about 45,000 tokens in testing.
4. Stretch: the MCP tool (`mcp/`, `examples/`).
5. Section 16 updates to the PRD and the MVP documentation.
6. Content: the articles, posts and video (section 15).

**Where the build differs from this plan**

| Plan says | Built as | Why |
|---|---|---|
| `config/WebConfig` for CORS | CORS is in `SecurityConfig` | Spring Security handles CORS preflight; one place is simpler |
| `store/IdAllocator` | `DeploymentStore.insert` and `WarningStore.insertWithNextId` are synchronized and commit before returning | Same guarantee, fewer classes |
| `service/RecordService.describe` | `RecordLookup.describe` | It is one query per table; no service needed |
| `tools/requirements.txt` | Not needed | All three tools use only the Python standard library |
| Retain path fixed | `HINDSIGHT_RETAIN_PATH`, default `/memories` | The Cloud cURL docs and the API reference disagree; Phase 0 check 1 decides |
| Section 8: `/api/admin/memory/*` in `MemoryAdminController` | `AdminController` under `/api/admin` | Holds sync, check and the demo reset |
| Not in the plan | `GET /api/config-keys` | The New incident form picks from canonical keys |
| Request booleans (`dryRun`, `nothingNewLearned`) | Nullable `Boolean` | Spring Boot 4 uses Jackson 3, which rejects a missing primitive field with a 400 |
| Citations from `cited_ids` plus the summary | Citations from the IDs in the summary | In real runs `cited_ids` listed nearly every record reflect read |
| Failed fixes from reflect's `failed_attempts` | From MySQL, for the warning's evidence incidents | Reflect mixed in attempts from unrelated incidents |
| Show reflect's `prior_false_positive` | Only if it names a false positive on the same key | Reflect mentioned WARN-42 on every change |
| Memory-off model on Groq, falling back to `qwen/qwen3-32b` | Groq `openai/gpt-oss-120b`, falling back to `qwen/qwen3.8-27b` on 429 or 503; any OpenAI-compatible API works (Gemini was tested) | `qwen3-32b` was retired from Groq. Asked "What went wrong in payment-service in the last three months?" 8 times each, `gpt-oss-120b` invented incidents 6 times, `qwen3.8-27b` 2 times and `gemini-3.5-flash-lite` 8 times. `gpt-oss-120b` stays as the main model: it is the strongest of the three, so the comparison isn't against a weak model, and it was the fastest |

---

## 1. Where we start

| Area | Today |
|---|---|
| Backend | Stub services return hard-coded data. Nothing calls Hindsight or an LLM. |
| Build | `pom.xml` carries unused Spring AI, pgvector, PostgreSQL, WebSocket and Testcontainers dependencies. It has `liquibase-core` but not Spring Boot 4's Liquibase starter, so migrations never run on startup. |
| Frontend | Two pages with data hard-coded in the components. The Risk Analysis page fakes a Hindsight call with a timer and shows "Querying Hindsight Memory Layer...". |
| Repository | No `.gitignore`. `target/`, `frontend/dist/` and 9,347 files of `frontend/node_modules/` are committed. |
| Code pack | `recallx-code-pack.zip` implements v2: the Hindsight client, Decision Guard, the seed data and three pages. It has not been compiled and is not in the repository. |

---

## 2. What this plan changes, and why

| Change | Why |
|---|---|
| Recall results are matched to records through their `document_id`, not by finding "INC-18" in the fact text. | Every recall result carries `document_id`, and we set it to the record ID. This removes the riskiest assumption, A-4. |
| The recall check sends a score floor (`min_scores.reranker`), and its query describes the change itself. | Recall always returns its best matches. Without a floor, every key "has history". |
| The warning schema gains `matches_history`. A memory-only warning needs an incident that both recall and reflect agree on. | This stops weak or invented matches from becoming warnings. |
| The demo's logging change becomes a memory-only **History match** citing INC-13. A new, unrelated key provides the quiet moment. | INC-13's root cause is debug logging filling a disk, so the logging change can't stay quiet. It becomes the one warning in the demo that only Hindsight could find. |
| New result: **Previously judged safe.** It appears when the same key was changed to the same value before and that warning was marked a false positive. | A verdict now changes what the engineer sees, not just one sentence. The demo shows this live with the retry change and WARN-42. |
| Decision Guard severity comes from the decision's linked incident. | Taking the worst incident that reflect happened to mention can jump to SEV1 on a passing reference. |
| Reflect sends `include.facts`, and its `based_on` field is parsed into memories, mental models and directives. | `based_on` is only returned when requested. The UI builds a "Sources" line from it. |
| Patterns come from Hindsight observations, shown with their source records. | This shows Hindsight's own consolidation, with citations that have been checked. |
| Every citation shows its date and age. | It makes the backdated, time-aware memory visible. |
| The keys in one check are processed in parallel, and warning IDs are assigned afterwards. | Four reflect calls in a row won't fit a demo slot, and parallel inserts would collide on IDs. |
| The repository is split into `backend/`, `frontend/`, `tools/`, `mcp/`, `examples/` and `docs/`. | Java, JavaScript, Python and TypeScript no longer share the root folder. |
| One `.env` file is read by the backend, Docker Compose and the tools. | Spring Boot doesn't read `.env` on its own, and `source .env` doesn't work in PowerShell. |
| Admin endpoints need a token, and there's a demo reset endpoint. | Without a token, anyone with a hosted link could reseed and spend credits. The demo must also run three times cleanly. |
| Each Ask panel names the model that produced it. | The two sides run on different models, so say so instead of claiming only memory differs. |

---

## 3. The organisers' answers (29 September)

| Question | Answer | Effect on this plan |
|---|---|---|
| May we bring code written before 3 October? | Yes. They expect teams to arrive with some functionality already built. | Phases 1 to 7 run before 3 October, starting from the code pack. The day is for fixes, rehearsal and content (section 12.3 becomes the fallback schedule). |
| Does judging need a hosted link? | No. | Run locally for the live demo. No hosting work. |
| Video: one per team or one per member? | One per team (content guide). | Articles and posts are per member; see section 15. |

---

## 4. Phase 0: Hindsight test (before the event, about 1 hour)

This is allowed whatever the organisers say. The script is written and lives outside the repository in `Desktop/recallx-spike/`, with a README and an `.env.example`. Move it to `tools/hindsight_spike.py` only if pre-written code is allowed. It uses a throwaway bank, `recallx-spike`, never `recallx-acme`.

**Retain** six records, rendered with the MVP documentation's templates and sent with `async: false`: INC-13, INC-18, INC-31, ADR-7, ADR-9 and WARN-42. Each has a `document_id`, a backdated `timestamp`, a `context` and `metadata`.

| # | Check | What it decides |
|---|---|---|
| 1 | Retain accepts `document_id` and `metadata`, and recall returns them. | Whether the recall check can match on `document_id`. The fallback is a regex over the fact text. |
| 2 | Seconds from retain until recall returns INC-18. | The seed deadline on the day. |
| 3 | `scores.reranker` of the top INC, ADR or WARN hit for six changes: the pool-size change, the logging change, the retry change, and three quiet candidates:<br>`management.endpoint.health.show-details: never -> always`<br>`server.compression.enabled: false -> true`<br>`spring.mvc.problemdetails.enabled: false -> true` | `HINDSIGHT_MIN_RERANKER`: set it between the logging score and the best quiet score. The quiet demo key is the candidate with the lowest score. |
| 4 | Reflect with `"include": {"facts": {}}` returns `based_on` with `memories`, `mental_models` and `directives`. Also check whether reflect accepts a `context` field. | The reflect request shape. |
| 5 | Reflect latency at `mid` and `high`, using the warning schema. | `RECALLX_CHECK_BUDGET`. Use `mid` if `high` takes more than 12 seconds. |
| 6 | Recall with `types: ["observation"]` and `"include": {"source_facts": {}}` returns observations with `source_fact_ids`. Also, how long after retain do they appear? If the bank config shows `enable_observations` or `enable_auto_consolidation` as false, turn them on. | Whether the patterns panel stays a Must. |
| 7 | The API paths for creating a mental model and directives, and whether a document can be deleted. | Whether bank setup runs through the API or the Cloud UI, and how the demo reset behaves. |

If check 3 can't separate the logging change from the quiet candidates, keep the floor low. The `matches_history` field and the agreement rule in section 7.3 then do the filtering.

Write the results into `docs/SPIKE_RESULTS.md` and put the chosen values in `.env`.

---

## 5. Target project structure

```
recall-x/
├── .github/workflows/ci.yml          backend tests, frontend build, banned-word check
├── .env.example                      every setting, no secrets; copy to .env
├── .gitattributes                    LF line endings for mvnw and shell scripts
├── .gitignore
├── docker-compose.yml                MySQL 8 for local development (optional)
├── README.md
├── docs/
│   ├── IMPLEMENTATION_PLAN.md        this plan
│   ├── HINDSIGHT.md                  how RECALL-X uses Hindsight (also pasted into the README)
│   ├── DEMO.md                       demo scripts, reset steps, bank settings, fallback plan
│   ├── SPIKE_RESULTS.md              Phase 0 measurements
│   └── images/                       architecture diagram, screenshots
├── backend/
│   ├── pom.xml  mvnw  mvnw.cmd  .mvn/
│   └── src/
│       ├── main/java/com/recallx/recallx/
│       │   ├── RecallXApplication.java
│       │   ├── api/            AskController, DeploymentCheckController, WarningController,
│       │   │                   StatsController, PatternsController, RecordController,
│       │   │                   ConfigKeyController, IncidentController, AdminController,
│       │   │                   HealthController, ApiExceptionHandler
│       │   ├── common/         BadRequestException, NotFoundException, ConflictException
│       │   ├── config/         RecallxProperties, SecurityConfig (with CORS), AdminTokenFilter, StartupChecks
│       │   ├── guard/          DiffParser, DecisionGuardService, WarningSchema,
│       │   │                   CheckResponse, KeyResult, WarningView, ClearedView
│       │   ├── llm/            BaselineLlmClient, Prompts
│       │   ├── memory/         MemoryTemplates, MemorySyncService, BankSetup, RecordIds
│       │   │   └── hindsight/  HindsightClient, RetainItem, RecallRequest, RecallHit, RecallResponse,
│       │   │                   ReflectAnswer, BasedOn, Sources, MemoryUnavailableException
│       │   ├── service/        AskService, VerdictService, StatsService, PatternService,
│       │   │                   IncidentCaptureService, DemoResetService
│       │   └── store/          JDBC stores (RecordLookup, WarningStore, ResetStore, ...), record types
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/changelog/
│       │       ├── db.changelog-master.yaml
│       │       └── changes/    001-schema.sql, 002-seed-data.sql
│       └── test/java/com/recallx/recallx/     unit and client contract tests (section 11)
├── frontend/
│   ├── .env.example                  VITE_BACKEND_URL
│   ├── index.html  package.json  package-lock.json  vite.config.js
│   ├── tailwind.config.js  postcss.config.js
│   └── src/
│       ├── main.jsx  App.jsx  api.js  format.js  index.css
│       ├── pages/        Ask.jsx, DeployCheck.jsx, Dashboard.jsx, NewIncident.jsx
│       └── components/   RecordChip, SourcesLine, WarningCard, ClearedCard, VerdictButtons,
│                         PrecisionChart, StatusNotes (SimulatedBadge, MemoryStatusBanner)
├── tools/
│   ├── gen_seed.py                   writes backend/.../002-seed-data.sql
│   ├── hindsight_spike.py            Phase 0 checks
│   └── smoke_test.py                 runs the acceptance checklist against a running app
├── mcp/                              stretch
│   ├── package.json  tsconfig.json
│   └── server.ts                     the check_change tool
└── examples/acme-payment-service/    stretch: the repository Claude Code edits in the MCP demo
    ├── src/main/resources/application.properties
    ├── CLAUDE.md
    └── .mcp.json
```

### 5.1 Removed

- **Stop tracking:** `target/`, `frontend/dist/`, `frontend/node_modules/`, `.DS_Store` and `.vscode/`. They're ignored from now on.
- **Delete:** `HELP.md`, the eight `generate_*.py` scripts, and `src/main/resources/application.yaml` (a duplicate of `application.yml`).
- **Delete the backend stubs:** the `agent/`, `ai/`, `controller/`, `demo/`, `dto/`, `entity/`, `integration/`, `memory/`, `repository/`, `service/` and `security/` packages. They're replaced by the structure above.
- **Delete** all of `src/test/`.
- **Delete** `db/changelog/changes/001-initial-schema.yaml` (the unused `system_health` table) and `002-domain-model.yaml`.

### 5.2 Commands

```bash
git rm -r --cached target frontend/dist frontend/node_modules .DS_Store .vscode
git rm HELP.md generate_*.py src/main/resources/application.yaml
mkdir backend
git mv pom.xml mvnw mvnw.cmd .mvn src backend/
git rm -r backend/src/test
cd backend/src/main/java/com/recallx/recallx
git rm -r agent ai controller demo dto entity integration memory repository service security
cd -
git rm backend/src/main/resources/db/changelog/changes/001-initial-schema.yaml \
       backend/src/main/resources/db/changelog/changes/002-domain-model.yaml
git update-index --chmod=+x backend/mvnw
```

Then add the files from section 6 and commit. Delete the local `target/` folder; it's rebuilt under `backend/target/`.

---

## 6. Configuration

### 6.1 `.env.example` (repository root)

Copy it to `.env` and fill in the keys. Use one `KEY=value` per line. **Don't put quotes around values, and don't write comments at the end of a line.** The backend reads this file as a properties file, where a `#` after a value becomes part of the value.

```properties
# Copy to .env and fill in the keys. Never commit .env.
# Read by the backend (spring.config.import), docker compose and tools/*.py.

# ---- Database (MySQL 8) ----
DB_URL=jdbc:mysql://localhost:3306/recallx?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=recallx
DB_PASSWORD=change-me
# Only used by docker compose
DB_ROOT_PASSWORD=change-me-root

# ---- Hindsight Cloud (apply promo code MEMHACK99 in Billing) ----
HINDSIGHT_URL=https://api.hindsight.vectorize.io
HINDSIGHT_API_KEY=
HINDSIGHT_BANK_ID=recallx-acme
# Retain path under /v1/default/banks/{bank}: /memories or /memories/retain. Set from Phase 0, check 1.
HINDSIGHT_RETAIN_PATH=/memories
# Recall score floor (0 to 1) for "this key has history". Set from Phase 0, check 3.
HINDSIGHT_MIN_RERANKER=0.3
# Reflect budgets: low, mid or high. Set the check budget from Phase 0, check 5.
RECALLX_ASK_BUDGET=mid
RECALLX_CHECK_BUDGET=high

# ---- Model for the memory-off answer (any OpenAI-compatible API) ----
LLM_BASE_URL=https://api.groq.com/openai/v1
LLM_API_KEY=
LLM_MODEL=openai/gpt-oss-120b
# Used once if the main model returns 429 (rate limited)
LLM_FALLBACK_MODEL=qwen/qwen3.8-27b

# ---- App ----
SERVER_PORT=8080
# Comma-separated origins allowed to call /api
CORS_ALLOWED_ORIGINS=http://localhost:5173
# Required in the X-Admin-Token header for /api/admin/**. If empty, admin endpoints refuse every request.
RECALLX_ADMIN_TOKEN=change-me-admin
# true = retain the seed into Hindsight on startup. Otherwise call POST /api/admin/memory/sync.
SEED_MEMORY_ON_STARTUP=false

# ---- MCP server (stretch) ----
RECALLX_URL=http://localhost:8080
```

### 6.2 `frontend/.env.example`

```properties
# Where the Vite dev server proxies /api. Not a secret; API keys never reach the browser.
VITE_BACKEND_URL=http://localhost:8080
```

### 6.3 How each part reads its settings

| Part | How it reads `.env` |
|---|---|
| Backend | `spring.config.import` loads `.env` from the working directory and from its parent, as a properties file. So `./mvnw spring-boot:run` inside `backend/` picks up the root `.env`, and so does an IDE run from the root. Real environment variables still take priority, which is what a hosting platform uses. |
| Docker Compose | Reads the root `.env` automatically. |
| `tools/*.py` | A small built-in parser reads the root `.env` (standard library only). |
| MCP server | `RECALLX_URL` is passed in through `examples/acme-payment-service/.mcp.json`. |
| Frontend | Vite reads `frontend/.env`, which holds only `VITE_BACKEND_URL`. |

### 6.4 `backend/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: recall-x
  config:
    import:
      - optional:file:.env[.properties]
      - optional:file:../.env[.properties]
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

server:
  port: ${SERVER_PORT:8080}

recallx:
  default-service: payment-service
  seed-memory-on-startup: ${SEED_MEMORY_ON_STARTUP:false}
  admin-token: ${RECALLX_ADMIN_TOKEN:}
  cors-allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
  hindsight:
    base-url: ${HINDSIGHT_URL:https://api.hindsight.vectorize.io}
    api-key: ${HINDSIGHT_API_KEY:}
    bank-id: ${HINDSIGHT_BANK_ID:recallx-acme}
    min-reranker: ${HINDSIGHT_MIN_RERANKER:0.3}
    ask-budget: ${RECALLX_ASK_BUDGET:mid}
    check-budget: ${RECALLX_CHECK_BUDGET:high}
    connect-timeout: 10s
    read-timeout: 60s
  llm:
    base-url: ${LLM_BASE_URL:https://api.groq.com/openai/v1}
    api-key: ${LLM_API_KEY:}
    model: ${LLM_MODEL:openai/gpt-oss-120b}
    fallback-model: ${LLM_FALLBACK_MODEL:qwen/qwen3.8-27b}

management:
  endpoints:
    web:
      exposure:
        include: health,info

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

`RecallxSettings` (from the code pack) becomes `RecallxProperties`, a `@ConfigurationProperties("recallx")` record. If `HINDSIGHT_API_KEY` or `LLM_API_KEY` is empty, the app logs a warning and starts anyway, and the UI shows its "unavailable" states.

### 6.5 `backend/pom.xml`

- **Remove:** the Spring AI BOM, the Spring milestone repository, `spring-boot-starter-data-jpa`, `spring-boot-starter-websocket`, `spring-ai-pgvector-store-spring-boot-starter`, both `postgresql` entries, `lombok` (nothing uses it), `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `spring-security-test`, and the whole `maven-compiler-plugin` block, which only exists for Lombok.
- **Replace:** `liquibase-core` with `spring-boot-starter-liquibase`. Spring Boot 4 moved the Liquibase auto-configuration into its own module, so the library alone never runs migrations. Also replace `spring-boot-starter-web` with `spring-boot-starter-webmvc`, its Spring Boot 4 name.
- **Add:** `spring-boot-starter-jdbc`, for `JdbcTemplate`.
- **Keep:** `spring-boot-starter-actuator`, `-security`, `-validation`, `springdoc-openapi-starter-webmvc-ui` 3.1.0, `mysql-connector-j`, `spring-boot-devtools` (optional) and `spring-boot-starter-test`.
- **Check on first start:** the log shows Liquibase applying `001-schema.sql`, and the `DATABASECHANGELOG` table exists.

### 6.6 `.gitignore` and `.gitattributes`

```gitignore
# Build output
target/
frontend/dist/
mcp/dist/
# Dependencies
node_modules/
tools/.venv/
__pycache__/
# Secrets
.env
frontend/.env
# Editors and OS
.idea/
*.iml
.vscode/
.DS_Store
Thumbs.db
*.log
```

```gitattributes
* text=auto
mvnw text eol=lf
*.sh text eol=lf
*.cmd text eol=crlf
```

### 6.7 `docker-compose.yml` (optional; a local MySQL install works too)

```yaml
services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_DATABASE: recallx
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      TZ: UTC
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
volumes:
  mysql-data:
```

---

## 7. Backend changes

Start from the code pack if it's allowed. Otherwise, write these from the MVP documentation plus the rules below.

### 7.1 `HindsightClient` (Must)

- **Constructor:** take an injected `RestClient.Builder`, so tests can bind `MockRestServiceServer` to it. Timeouts come from `RecallxProperties`.
- **`recall(RecallRequest)`:**
  - Sends `query`, `types`, `budget` and `max_tokens`, plus `min_scores: {"reranker": x}` and `include: {"source_facts": {}}` when they're set.
  - Parses these fields from each result: `id`, `text`, `type`, **`document_id`**, `metadata`, `occurred_start`, **`scores.reranker`**, `scores.final` and `source_fact_ids`.
  - Also parses the top-level `source_facts`.
- **`reflect(query, budget, schema)`:**
  - Always sends `include: {"facts": {}}`. Drop `context` unless Phase 0 check 4 confirms reflect accepts it.
  - Parses `text`, `structured_output`, `structured_output_error` and `usage`.
  - Parses `based_on` into a `BasedOn` record with `memories[]`, `mentalModels[]` and `directives[]`. Today it's turned into a string with `toString()`.
- **`retain`:** the request shape doesn't change. Phase 0 check 1 confirms that `document_id` and `metadata` are accepted. Use the path Phase 0 found working: the Cloud cURL examples use `POST .../memories`, while the API reference uses `POST .../memories/retain`.
- **`createMentalModel`, `upsertDirective`, `deleteDocument`:** add these only if Phase 0 check 7 finds the endpoints. Otherwise set these up in the Cloud UI.
- **Logging:** log every call with the operation, the document ID or a query hash, the duration and the HTTP status. For reflect, also log `usage.total_tokens`.
- **Errors:** 401, 402 (out of credits), 404, 429, any 5xx and timeouts all become `MemoryUnavailableException`, with the status in the message.

### 7.2 Bank setup and memory sync (Must)

`MemorySyncService.syncAll()` does four things in order:

1. It sets the bank's mission and a disposition of skepticism 4, literalism 4 and empathy 2, using the call Phase 0 found working.
   - The API reference documents `PATCH /v1/default/banks/{bank}` with flat fields: `reflect_mission`, `disposition_skepticism`, `disposition_literalism` and `disposition_empathy`.
   - The code pack's `PUT` with a nested `disposition` object may be rejected.
   - The bank itself is created by the first retain.
   > Mission: *I am the engineering memory for Acme Pay's payment platform. I prioritise root causes, fixes that failed, and the reasons behind configuration decisions.*
2. It sets up the directives and the mental model. The documented calls are `POST .../directives` with `{name, content}` and `POST .../mental-models` with `{name, source_query, trigger}`. Phase 0 confirms they work on Cloud; if they don't, set them once in the Cloud UI. `docs/DEMO.md` holds the exact text. Leave the directives untagged, so they apply to every reflect call:
   1. Cite the record ID (INC-, ADR-, DEP- or WARN-) for every claim.
   2. If memory holds no relevant record, say so. Never invent an incident, decision, ID or number.
   3. Mention any fix that was tried before and failed.
   4. If an earlier warning on a similar change was marked a false positive, mention it and its reason.
   5. Describe similarity to past events. Never predict that a change will fail.
   6. If a decision has been superseded, use the newer one.
3. It retains records in batches of 10 with `async: true`, but only rows whose `retained_at` is empty, then sets `retained_at`. The set of records retained is the same as in MVP section 14: 15 incidents, 5 decisions, 22 warning outcomes, and the 27 deployments that change a canonical key.
4. **Mental model (Should):**
   - ID `payment-config-rules`, name "payment-service configuration rules".
   - Source query: "Which payment-service configuration values were set deliberately, what incident led to each, and what failed before?"
   - Trigger: `refresh_after_consolidation: true`.

### 7.3 Deployment check rules (Must)

For each changed key, running in parallel as described in 7.7:

1. **Decision:** look up the newest ACTIVE decision on the key in MySQL.
2. **Cleared:** look up the newest FALSE_POSITIVE warning on the same key whose deployment changed it to the same new value, also in MySQL.
3. **Recall check.** This runs only if there's neither a decision nor a cleared verdict.
   - **Query:** `"{service}: change {key} from {old} to {new}"`. Describe the change only, and leave out generic words like "incidents" or "failed fixes", which match every incident.
   - **Request:** `types: ["world", "experience"]`, `budget: "mid"`, `max_tokens: 2048`, and `min_scores.reranker` set to `HINDSIGHT_MIN_RERANKER`.
   - **`recalled`:** the result `document_id`s that exist in MySQL and are INC, ADR or WARN records.
   - If `recalled` is empty, the result is **NO_HISTORY** and reflect is never called. If memory is down, the result is **MEMORY_UNAVAILABLE**.
4. **Reflect.** Send the warning query with the schema in 7.4, using budget `RECALLX_CHECK_BUDGET`. Retry once on `structured_output_error`.
   - If memory is down and there's a decision or a cleared verdict, build the text from MySQL.
   - If memory is down and there's neither, the result is MEMORY_UNAVAILABLE.
5. **Outcome:**
   - A cleared verdict gives **PREVIOUSLY_CLEARED** (7.5).
   - Otherwise, a decision gives a **WARNING** of kind `DECISION_GUARD`, with `foundBy: "DECISION"`.
   - Otherwise, the result is a **WARNING** of kind `HISTORY` with `foundBy: "MEMORY"`. This happens only if `matches_history` is true **and** at least one incident appears both in `recalled` and in reflect's valid cited IDs. Otherwise the result is NO_HISTORY.
6. **Cited records shown:** the decision, its linked incident, and every ID reflect cited that exists in MySQL. Invented IDs are dropped, as before.

This changes FR-302. An active decision now always produces a *card*: either a warning, or a "previously judged safe" note that still names the decision. Section 16 lists the PRD update.

### 7.4 Warning schema (Must)

```json
{
  "type": "object",
  "properties": {
    "matches_history": { "type": "boolean", "description": "true only if a cited record involves the same setting, component or failure mode as this change" },
    "summary": { "type": "string", "description": "2-3 sentences, citing IDs" },
    "cited_ids": { "type": "array", "items": { "type": "string" }, "description": "Every INC-, ADR-, DEP- or WARN- ID used" },
    "failed_attempts": { "type": "array", "items": { "type": "string" }, "description": "Fixes that were tried before and did not work" },
    "prior_false_positive": { "type": "string", "description": "An earlier similar warning marked false positive, and why; empty if none" },
    "recommendation": { "type": "string", "description": "One sentence on what to check before shipping" }
  },
  "required": ["matches_history", "summary", "cited_ids", "failed_attempts", "recommendation"]
}
```

When `matches_history` is false, don't pull IDs out of the summary text. Reflect often names an incident while explaining why it doesn't apply.

### 7.5 Previously judged safe (Must, with a fallback)

- **Trigger:** MySQL has a FALSE_POSITIVE warning on the same key, and that warning's deployment set the key to the same new value. The exact match keeps it predictable; broader matching is v1 work.
- **Card:** it shows the earlier warning's ID and date, the engineer's reason from MySQL, reflect's wording, and any decision that still applies. For example:
  > Previously judged safe in WARN-42 (16 Jun): idempotency is on. ADR-9 still applies: retries above 3 need idempotency. Check that `payment.retry.idempotency-enabled` is still true.
- **Not saved as a warning:** the card has no verdict buttons and doesn't affect precision. The deployment change is still saved.
- **Without memory:** the same card is built from MySQL alone.
- **Fallback if this slips:** the Decision Guard warning still mentions WARN-42 through directive 4.

### 7.6 Severity (Must)

- **DECISION_GUARD:** the severity of the decision's linked incident. If there's no linked incident, show no severity.
- **HISTORY:** the worst severity among incidents that are both in `recalled` and cited by reflect.
- Severity always comes from MySQL, never from the model.

### 7.7 Parallel keys and IDs (Must)

- Run `checkOne` for each key on a bounded executor with `CompletableFuture`: 4 threads, or 2 if Hindsight rate-limits. Keep results in the diff's order.
- Allocate the deployment ID before the keys run. Assign warning IDs after every key finishes, in diff order, through `WarningStore.insertWithNextId`: synchronized, with `MAX + 1` read and the row committed before the lock is released.
- The deployment is still retained after the response is sent, as before.

### 7.8 Ask (Must)

- Both sides run in parallel.
- `llm/Prompts.java` holds the shared wording, so NFR-03 can be reviewed in one file:
  - `ROLE` = "You are an on-call assistant for Acme Pay's payment-service."
  - `FORMAT` = "Answer in at most 5 short bullet points."
  - **Memory off (Groq):** the system message is `ROLE + FORMAT`, and the user message is the question.
  - **Memory on (reflect,** with budget `RECALLX_ASK_BUDGET` and `include.facts`**):** `ROLE`, then the question, then "Use this team's history: which past incidents match, which fixes failed, what resolved them, and which decisions apply. Cite the record ID for each point.", then `FORMAT`.
- Each side's response gains a `model` label ("openai/gpt-oss-120b" or "Hindsight reflect"). The memory side also gains `citedRecords` and `sources`.
- If Groq returns 429, retry once with `LLM_FALLBACK_MODEL`, and label the answer with the model actually used.
- If memory fails, the right panel shows "Memory unavailable". It never shows the memory-off answer instead.
- The question must be 1 to 500 characters, or the request gets a 400.

### 7.9 Patterns (Must if Phase 0 check 6 passes, otherwise Should)

- Recall with `types: ["observation"]`, `budget: "low"`, `include.source_facts`, and the query "recurring failure patterns in payment-service".
- Return up to 5 observations. Each one carries its text plus its record IDs: the `document_id`s of its source facts that exist in MySQL. An observation with no valid source is shown without chips.
- Cache the result for 60 seconds.

### 7.10 Records and citations (Must)

- `RecordLookup.describe(ids)` returns `{id, type, date, severity?, title}` from MySQL:
  - incident: `started_at`, severity, and the first sentence of the symptom
  - decision: `decided_at` and the decision
  - deployment: `deployed_at` and the version
  - warning: `created_at` and the key
- Ask, warnings and patterns all use it, which lets every chip show a date and an age.
- **`GET /api/records/{id}` (Should):** one record's summary, for a chip popover (FR-204).

### 7.11 Verdicts and stats (Must, unchanged from the MVP documentation)

- A false positive without a reason gets a 400, and a second verdict on the same warning gets a 409.
- The verdict is retained with `document_id` set to the warning ID.
- Precision is grouped by **month** everywhere.

### 7.12 Admin endpoints (Must)

All of these need the `X-Admin-Token` header.

- **`POST /api/admin/memory/sync`:** as described in the MVP documentation.
- **`GET /api/admin/memory/check?q=`:** returns document IDs together with their reranker scores. Used in Phase 0 and before the demo.
- **`POST /api/admin/demo/reset`:**
  - Deletes every row with `seeded = 0`, in foreign-key order: warnings, then the attempts and keys of unseeded incidents, then those incidents, then deployment changes, then deployments.
  - The next run gets DEP-2026-061 and WARN-55 again. Retaining the same `document_id` replaces the earlier memory, so reruns don't pile up in Hindsight.
  - Also delete those documents with `DELETE .../documents/{document_id}`, which is documented; Phase 0 check 7 confirms it works on Cloud.
  - Rehearse with the same verdict the live demo uses.

### 7.13 Security, CORS, errors and limits (Must)

- **`SecurityConfig`:** `/api/admin/**` requires `X-Admin-Token` equal to `RECALLX_ADMIN_TOKEN`, compared in constant time. If the token isn't set, every admin request is refused. Everything else is open, since the MVP has no sign-in. CSRF is off because it's a JSON API with no cookies.
- **CORS:** `/api/**` allows the origins in `CORS_ALLOWED_ORIGINS`.
- **Error body:** `{error, detail}`.

| Status | `error` | When |
|---|---|---|
| 400 | `bad_request` | Invalid input |
| 404 | `not_found` | Unknown record |
| 409 | `conflict` | Second verdict on a warning |
| 503 | `memory_unavailable` | Hindsight failed. Never replaced by the memory-off answer. |

  An LLM failure on Ask's left side is reported inside the 200 response.
- **Input limits:** a question is at most 500 characters. A diff is at most 20 lines, each at most 300 characters. A verdict reason is at most 500 characters. Incident text fields are at most 2,000 characters.

---

## 8. API contract

| Method | Path | Change from the MVP documentation |
|---|---|---|
| POST | `/api/ask` | Each side gains `model`. The memory side gains `citedRecords` and `sources`. |
| POST | `/api/deployments/check` | New status `PREVIOUSLY_CLEARED`. Warnings gain `foundBy` and `citedRecords`. Keys run in parallel. |
| POST | `/api/warnings/{id}/verdict` | Unchanged. |
| GET | `/api/warnings` | Unchanged. |
| GET | `/api/stats` | Precision is monthly. |
| GET | `/api/patterns` | Each pattern returns `{text, records[]}`. |
| GET | `/api/records/{id}` | New (Should). |
| POST | `/api/incidents` | Unchanged (Should). |
| POST | `/api/admin/memory/sync` | Needs the admin token. |
| GET | `/api/admin/memory/check?q=` | Needs the admin token. Returns scores. |
| POST | `/api/admin/demo/reset` | New. Needs the admin token. |

`POST /api/ask` response:

```json
{
  "withoutMemory": { "model": "openai/gpt-oss-120b", "text": "...", "error": null },
  "withMemory": {
    "model": "Hindsight reflect",
    "text": "...",
    "citedRecords": [
      { "id": "INC-18", "type": "INCIDENT", "date": "2026-03-12", "severity": "SEV2", "title": "Payment API requests timed out" }
    ],
    "sources": { "memories": 6, "mentalModels": ["payment-config-rules"], "directives": ["Cite record IDs"] },
    "error": null
  }
}
```

`POST /api/deployments/check` response for the demo change:

```json
{
  "deploymentId": "DEP-2026-061",
  "results": [
    { "key": "spring.datasource.hikari.maximum-pool-size", "oldValue": "20", "newValue": "60", "status": "WARNING",
      "warning": { "id": "WARN-55", "kind": "DECISION_GUARD", "foundBy": "DECISION", "severity": "SEV2",
                   "summary": "...", "citedRecords": [], "failedAttempts": ["Restarted payment-service pods (INC-18)"],
                   "priorFalsePositive": "", "recommendation": "...", "status": "PENDING", "sources": {} } },
    { "key": "logging.level.com.acmepay", "oldValue": "INFO", "newValue": "DEBUG", "status": "WARNING",
      "warning": { "id": "WARN-56", "kind": "HISTORY", "foundBy": "MEMORY", "severity": "SEV3", "summary": "...cites INC-13..." } },
    { "key": "payment.retry.max-attempts", "oldValue": "3", "newValue": "4", "status": "PREVIOUSLY_CLEARED",
      "cleared": { "warningId": "WARN-42", "date": "2026-06-16", "reason": "...", "decisionId": "ADR-9", "summary": "..." } },
    { "key": "<quiet key from Phase 0>", "oldValue": "...", "newValue": "...", "status": "NO_HISTORY" }
  ]
}
```

---

## 9. Seed data changes (`tools/gen_seed.py`)

- **Paths:** the output goes to `backend/src/main/resources/db/changelog/changes/002-seed-data.sql`. The schema becomes `001-schema.sql`: the code pack's `002-mvp-schema.sql` plus a `seeded TINYINT(1) NOT NULL DEFAULT 0` column on `deployment`, `incident`, `architecture_decision` and `warning`.
- **`seeded`:** every seeded row gets `seeded = 1`.
- **Story:** no changes. INC-13 stays, and is now a demo record. Confirm that WARN-42's deployment, DEP-2026-041, sets `payment.retry.max-attempts` to 4, because the "previously judged safe" rule matches on it.
- **Demo change:** one constant, used by the frontend's "Load demo change" button and by `smoke_test.py`:
  ```
  spring.datasource.hikari.maximum-pool-size: 20 -> 60
  logging.level.com.acmepay: INFO -> DEBUG
  payment.retry.max-attempts: 3 -> 4
  <quiet key chosen in Phase 0>
  ```
- **Precision table:** unchanged (April to September: 25, 33, 67, 67, 75, 80).
- **SQL files:** keep the `--liquibase formatted sql` header.

---

## 10. Frontend changes

- **`api.js`:** turns `{error, detail}` responses into thrown errors that carry the code. The UI never calls admin endpoints.
- **`vite.config.js`:** reads the proxy target from `VITE_BACKEND_URL` using `loadEnv`.
- **Ask:**
  - Two `AnswerPanel`s side by side, each labelled with its model.
  - Demo chips: "The payment API is timing out. What should I do?" and, for the video, "What went wrong in payment-service in the last three months?"
  - `RecordChip`s showing the date and age, for example "INC-18 · 12 Mar · 7 months ago". `format.js` computes the age.
  - A `SourcesLine` under the memory answer, and a `MemoryStatusBanner` on a 503.
- **DeployCheck** (replaces `RiskAnalysis.jsx` and its fake timer entirely):
  - A version field, a diff box and a "Load demo change" button that fills in the four lines.
  - One result per key: a `WarningCard` (with a "Found by" label, a severity badge in text, record chips, failed fixes, a recommendation and `VerdictButtons`), a `ClearedCard`, a grey `NoHistoryRow`, or a "Memory unavailable" row.
  - The button reads "Checking history..." while it runs.
- **Dashboard:**
  - Count tiles.
  - `PrecisionChart`: monthly, subtitled "Simulated history to 25 Sep; live verdicts after".
  - `PatternsPanel`: "Patterns Hindsight noticed", with record chips.
  - A recent-warnings table.
- **NewIncident (Should):** the form from MVP section 12. It's missing from the code pack.
- **Everywhere:** `SimulatedBadge` on every page. Text is 16 px, 18 px in the answer panels, and severity is always written out, never shown by colour alone.

---

## 11. Tests and CI

### 11.1 Unit and contract tests (JUnit 5, Mockito, AssertJ)

| Test class | Cases |
|---|---|
| `DiffParserTest` | A valid diff parses. Blank lines are skipped. A bad line gets a 400 naming its line number. More than 20 lines gets a 400. |
| `RecordIdsTest` | ID extraction and `isEvidence`. |
| `MemoryTemplatesTest` | Every template includes the record ID, its date and canonical key names. |
| `DecisionGuardServiceTest` | Existing cases: memory down keeps the Decision Guard warning, invented IDs are dropped, partial citations are handled. New cases: 1) the recall check reads `document_id`, not text; 2) empty recall gives NO_HISTORY without calling reflect; 3) `matches_history: false` gives NO_HISTORY even when the summary names INC-18; 4) a HISTORY warning needs an incident that was both recalled and cited; 5) Decision Guard severity comes from the linked incident even when reflect also cites a SEV1; 6) the same key and value with an earlier false positive gives PREVIOUSLY_CLEARED and saves no warning; 7) a cleared verdict with memory down gives the MySQL text; 8) four keys in parallel get distinct WARN IDs in diff order; 9) memory down with no decision gives MEMORY_UNAVAILABLE. |
| `AskServiceTest` | With memory down, `withMemory.error` is set and the memory-off answer is unaffected. `citedRecords` contains only MySQL IDs. A Groq 429 falls back to the second model. |
| `VerdictServiceTest` | A false positive without a reason gets a 400, a second verdict gets a 409, and the retain uses the warning ID as its `document_id`. |
| `PatternServiceTest` | Observation source facts map to record IDs that have been checked against MySQL. |
| `HindsightClientTest` | Uses `MockRestServiceServer`. Request bodies: `min_scores`, `include.facts`, `include.source_facts`, `document_id` and `timestamp`. Parsing: `document_id`, `scores.reranker`, each part of `based_on`, and `source_facts`. Error mapping: 401, 402, 500 and timeouts all become `MemoryUnavailableException`. |
| `AdminTokenFilterTest` | An admin request without the token is refused. With no token configured, every admin request is refused. |

### 11.2 Smoke test (`tools/smoke_test.py`)

It calls the reset endpoint, then runs the checklist in section 14 against the running app and prints pass or fail for each check. Run it before every rehearsal.

### 11.3 CI (`.github/workflows/ci.yml`)

On every push:

1. `cd backend && ./mvnw -B test`
2. `cd frontend && npm ci && npm run build`
3. The banned-word check. The pattern is written so this file doesn't match itself:
   `! grep -rniE 'hack[a]thon' --exclude-dir=node_modules --exclude-dir=target --exclude-dir=.git .`

---

## 12. Build schedule, roles and cut order

### 12.1 Roles

| Role | Owns |
|---|---|
| **A. Memory and backend** | `memory/`, `guard/`, `llm/`, `AskService`, `PatternService`, the Ask, check, warning, patterns and admin controllers, and `mcp/` |
| **B. Frontend** | Everything under `frontend/` |
| **C. Data, database and tests** | The restructure and configuration (section 5 and section 6), `tools/`, the Liquibase changesets, `store/`, `StatsService`, `DemoResetService`, the tests and CI |
| **D. Demo and content** | Bank settings in the Cloud UI, `docs/DEMO.md`, `docs/HINDSIGHT.md`, the README, screenshots, the video, articles and posts, and `examples/` |

With 2 or 3 people, merge C into A and D into B.

### 12.2 Before 3 October (whatever the organisers say)

- [ ] Send the questions in section 3.
- [ ] Create a Hindsight Cloud account, apply `MEMHACK99` in Billing and create an API key. Create a Groq API key.
- [ ] Run Phase 0 and fill in `docs/SPIKE_RESULTS.md`.
- [ ] On every laptop: JDK 21, MySQL 8 or Docker, Node 20+, Python 3.11+ and Git. Run `./mvnw dependency:go-offline` and `npm ci`.
- [ ] Each member drafts an article and a post (MVP section 19).

### 12.3 On the day (8 hours)

| Time | A | B | C | D |
|---|---|---|---|---|
| 0:00–0:30 | `HindsightClient` changes | App skeleton against the example JSON in section 8 | Restructure, config files, database up | Keys in `.env`; bank mission, disposition and directives in the Cloud UI |
| 0:30–1:00 | Sync and bank setup | Ask page | Schema, seed, `seeded` column | `docs/DEMO.md` |
| **1:00** | **Start the seed sync** | | | |
| 1:00–3:00 | Check rules (sections 7.3 to 7.7) | DeployCheck page and cards | Stores, records, stats, reset | README, `docs/HINDSIGHT.md` |
| **3:00** | **Checkpoint: APIs live** | | | |
| 3:00–5:00 | Ask, patterns, verdict retain | Dashboard, wiring, error states | Unit and contract tests | Screenshots, demo script |
| **5:00** | **Checkpoint: feature freeze** | | | |
| 5:00–6:00 | Stretch only if every checkpoint passed, otherwise fixes | Polish | Smoke test, CI | Rehearse |
| 6:00–7:00 | Fixes only | Fixes only | Run the smoke test between rehearsals | Three full runs, then record the backup video |
| **7:00** | **Demo lock: no new code** | | | |
| 7:00–8:00 | Submit | | | Upload the video, publish posts |

| Checkpoint | Done when | If it's missed |
|---|---|---|
| 1:30 Memory seeded | `/api/admin/memory/check` returns INC-18, ADR-7 and INC-13 | Cut the background incidents to five. Never reseed twice. |
| 3:00 APIs live | The demo change returns all four statuses, and `/api/ask` returns both answers | The frontend stays on mock data while A and C pair on the blocker. |
| 5:00 Feature freeze | Every Must works end to end in the browser | Follow the cut order below. |
| 7:00 Demo lock | Three clean runs, and the backup video is recorded | Fix only what breaks the demo. |

### 12.4 Cut order

When time runs short, cut in this order, and don't reopen anything that was cut:

1. The MCP tool and `examples/`
2. The mental model
3. The NewIncident page
4. The time-question chip
5. The recent-warnings table
6. The record popover
7. The patterns panel's source chips (show the observations without them)

**Never cut:** memory seeded into Hindsight, Ask with memory on and off and its model labels, Decision Guard, the INC-13 memory match, the quiet row, the WARN-42 "previously judged safe" card (or its fallback in 7.5), verdicts with the precision chart, the reset endpoint, and everything in section 13's honesty rules.

---

## 13. Demo script (60 seconds)

Before the demo, run the reset and warm-up question. Open the app on the Dashboard, with the browser at 125% zoom.

| Time | On screen | Say |
|---|---|---|
| 0–7s | Dashboard: patterns panel and chart | "Six months of simulated history for a payments team. RECALL-X has noticed on its own: pool-size changes keep causing connection errors." |
| 7–22s | Ask: the demo question, both panels | "A plain model says restart the service. With memory: restarting already failed twice, in INC-18 and INC-31. The cause was the connection pool, and ADR-7 says why it's 20." |
| 22–47s | DeployCheck: "Load demo change", then Check | "Four changes, four answers. The pool size hits a deliberate decision, ADR-7. Debug logging has no decision on file, but Hindsight remembered INC-13, when it filled a disk. The retry change was flagged in June and judged safe, so RECALL-X says so instead of crying wolf. And the last change? Nothing relevant, so it stays quiet." |
| 47–54s | Click Useful on WARN-55 | "Every verdict goes back into memory. That's how WARN-42 taught it." |
| 54–60s | Stay on the cards | "RECALL-X remembers what failed, why things are the way they are, and its own mistakes." |

**Honesty rules:**
- Call the history simulated.
- Present the precision chart as seeded history, not as proof that the software improved.
- If asked "is the comparison rigged?", answer: "same role, question and format; the left is Groq, the right is Hindsight's reflect, and the labels say so".
- Only mention the MCP tool if it's built.

For the 3-minute video, follow MVP section 16, and add the time question and a shot of the new WARN-55 memory in the Cloud UI.

---

## 14. Acceptance checklist

`tools/smoke_test.py` automates the API rows. Run the whole list at the 5:00 and 7:00 checkpoints.

| Area | Test | Expected |
|---|---|---|
| Memory | `/api/admin/memory/check?q=payment-service maximum-pool-size` | Returns the document IDs INC-18 and ADR-7 |
| Memory | A second sync | No duplicate documents in the Cloud UI |
| Ask | The demo question | The right panel cites INC-18 (with a date chip) and names the failed restart. The left panel cites no IDs. Both show model labels, and the Sources line counts the memories used. |
| Ask | "How do I handle an expired gateway certificate?" | Cites INC-11 or says there's no matching history. Never shows an ID that isn't in MySQL. |
| Check | The pool-size line | WARNING, `DECISION_GUARD`, found by the decision, citing ADR-7 and INC-18, SEV2, with the restart among the failed fixes |
| Check | The logging line | WARNING, `HISTORY`, found by memory, citing INC-13, SEV3 |
| Check | The retry line | PREVIOUSLY_CLEARED, citing WARN-42 and its reason and naming ADR-9, with no verdict buttons |
| Check | The quiet key | NO_HISTORY |
| Check | `payment.settlement.cron: 0 30 1 * * * -> 0 30 23 * * *` | `DECISION_GUARD`, citing ADR-11 and INC-33, SEV2 |
| Check | `feature.flag.x: off -> on` | NO_HISTORY |
| Check | A line without `->`, or 21 lines | 400, naming the line |
| Verdicts | False positive without a reason / a second verdict | 400 / 409 |
| Verdicts | Mark Useful | 200, and a `[retain] WARN-55` log line |
| Dashboard | After a reset | Counts read 15, 5, 60 and 22. Precision reads 25, 33, 67, 67, 75, 80. At least one pattern has chips (if Phase 0 check 6 passed). |
| Resilience | Wrong Hindsight key | Pool: a warning built from MySQL. Logging and quiet key: MEMORY_UNAVAILABLE. Retry: PREVIOUSLY_CLEARED built from MySQL. Ask: "Memory unavailable" on the right. |
| Resilience | Wrong Groq key | The left panel shows an error, and the right panel still works. |
| Security | An admin call without the token | Refused |
| Repo | `git ls-files` | No `node_modules`, `target`, `dist` or `.env`. CI is green, and the banned-word check passes. |
| Demo | The 60-second script | Three clean runs in a row with a reset between them. The backup video is recorded. |

---

## 15. Submission checklist

The content guide has each member run its prompts in Claude Code or Codex **inside this repository**, so the AI reads the code to write their article. A clear README and `docs/HINDSIGHT.md` therefore directly improve every member's article. Don't commit the guide's prompts to the repository: they contain the banned word and would fail the CI check.

**Repository and demo**
- [ ] A public GitHub repository. The README follows MVP section 19, plus "How RECALL-X uses Hindsight" from `docs/HINDSIGHT.md`, a "What's simulated" section, and setup steps for both bash and PowerShell.
- [ ] The live demo (no hosted link needed).

**Per member**
- [ ] An article of 800 to 1,500 words, public and linkable on Medium, Dev.to, Hashnode, Substack or LinkedIn Articles.
  - Its title is about the idea or result, has no more than 10 words, and mentions Hindsight.
  - It includes at least one real code snippet, one before/after example, one honest lesson or limitation, and screenshots or an architecture diagram.
  - It links to https://github.com/vectorize-io/hindsight, https://hindsight.vectorize.io/ and https://vectorize.io/what-is-agent-memory, and tags Code.in.
- [ ] The article is submitted as a link post to r/llmdevs, r/sideproject, r/aiagents or r/aimemory.
- [ ] A LinkedIn post under 800 characters.
  - The project's GitHub link goes in the post itself, and 3 to 5 hashtags go on the last line.
  - The article URL goes in the first comment, and the Hindsight repository link in another comment. Tag Code.in.

**Per team**
- [ ] One video: 2 to 5 minutes, at least 1080p, public on YouTube, with a thumbnail. It covers an intro, the agent struggling without memory, a live demo showing retain and recall, and one takeaway.

**Everywhere**
- [ ] The banned word appears nowhere: not in the repository, the articles, the posts, the hashtags or the video. One mention disqualifies that piece.

---

## 16. Updates needed in the other documents

| Document | Update |
|---|---|
| PRD | FR-302: "always produces a card: a warning, or a previously-judged-safe note". FR-403: accepted by the WARN-42 card. NFR-03: "same role and format; the model is named on screen". J2: the four-line demo change. J3: "or stays quiet" becomes "is shown as previously judged safe". |
| MVP documentation | Precision is monthly, not weekly (overview, F5, US-6). The directives list becomes the six in section 7.2. The demo change, the acceptance checks, and the answer to "is the comparison rigged?" all change. |

---

## 17. Risks (new or changed; MVP section 18 still applies)

| Risk | Mitigation |
|---|---|
| The score floor doesn't separate the logging change from the quiet keys | Rely on `matches_history` and the recall-and-reflect agreement rule. Pick the quiet key with the lowest score. |
| Observations don't appear within the day | The patterns panel shows "No patterns yet" and drops out of the script. |
| Parallel reflect calls hit Hindsight's rate limits | Drop the executor to 2 threads, or the check budget to `mid`. |
| The Liquibase starter name differs in Spring Boot 4.1 | The symptom is that the `DATABASECHANGELOG` table is missing. Check the Spring Boot 4.1 documentation for the Liquibase module. |
| Reflect ignores directive 4 | The previously-judged-safe card is built from MySQL either way. |
| Rehearsal verdicts pollute memory | Reset between runs and use the same verdict each time. Delete the documents if Phase 0 check 7 found a way to. |
| `.env` values with quotes or end-of-line comments break the backend | Section 6.1's format rule; `.env.example` shows the format. |
