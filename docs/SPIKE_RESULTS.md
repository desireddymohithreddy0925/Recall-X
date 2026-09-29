# Phase 0 results: Hindsight test

Run: 2026-09-29 15:19:38 · bank `recallx-spike` · https://api.hindsight.vectorize.io

## Values for `.env`

```properties
HINDSIGHT_RETAIN_PATH=/memories
HINDSIGHT_MIN_RERANKER=0.332
RECALLX_CHECK_BUDGET=high
```

Quiet demo key: `management.endpoint.health.show-details`. The demo change and `tools/smoke_test.py` now use it.

## What changed because of these results

- **`.env`:** `HINDSIGHT_MIN_RERANKER=0.3` rather than 0.332. The logging change's top match (INC-13) scored only 0.41, so this gives it more headroom; the quiet key scored 0.03, and the other candidates at most 0.25.
- **Record IDs:** only 62% of recalled facts still contained their record ID in the text, so matching on `document_id` (not a regex over the text) was necessary.
- **Citations:** reflect's `cited_ids` listed nearly every record it read, while the summary named the right ones. Citations now come from the IDs in the summary.
- **Earlier false positives:** reflect mentioned WARN-42 on every change, not just the retry change. The note is now kept only when it names a false positive on the same key, checked in MySQL.
- **Failed fixes:** reflect mixed in attempts from other incidents. Failed fixes now come from MySQL for the incidents the warning is based on.
- **Cost:** each reflect call used about 45,000 tokens. Watch usage in the Hindsight Cloud UI; `RECALLX_ASK_BUDGET` and `RECALLX_CHECK_BUDGET` can drop to `low` if credits run short.

## Checks

| # | Check | Result |
|---|---|---|
| 1 | Retain path | `/v1/default/banks/{bank_id}/memories` (HTTP 200, 5.13 s) |
| 1 | Retain accepts `document_id` and `metadata` | yes |
| 1 | Recall returns `document_id` / `metadata` / `scores` | yes / yes / yes |
| 1 | Share of recalled facts that still contain a record ID | 62% |
| 2 | Seconds from retain until INC-18 is recallable | 2.7 |
| 3 | Score floor separates the logging change from the quiet keys | yes |
| 3 | Logging change's top match is INC-13 | yes |
| 3 | `min_scores` accepted; hits above the floor for quiet key / logging | HTTP 200; 0 / 1 |
| 4 | Reflect returns `based_on` with `include.facts` | yes (shape `{"facts": {}}`, keys ['directives', 'memories', 'mental_models']) |
| 4 | Reflect accepts `context` | yes |
| 4 | Ask demo question cites INC-18 and mentions the restart | yes |
| 5 | Reflect `pool-mid` latency | 13.23 s (HTTP 200, 46171 tokens) |
| 5 | Reflect `pool-high` latency | 8.82 s (HTTP 200, 47668 tokens) |
| 5 | Reflect `logging-mid` latency | 9.88 s (HTTP 200, 44689 tokens) |
| 5 | Reflect `retry-mid` latency | 9.63 s (HTTP 200, 45151 tokens) |
| 5 | Reflect `quiet-mid` latency | 11.4 s (HTTP 200, 45269 tokens) |
| 5 | Logging change: `matches_history` true and cites INC-13 | yes / yes |
| 5 | Quiet key: `matches_history` false | yes |
| 5 | Retry change mentions WARN-42 (directive 4) | yes |
| 6 | Observations after seeding | 8 (after 0.6 s; source_fact_ids yes; source documents ['ADR-7', 'ADR-9', 'INC-13', 'INC-18', 'INC-29', 'INC-31', 'SPIKE-TEMP', 'WARN-42']) |
| 7 | Bank create: PUT nested (code pack) / POST | HTTP 200 / not tried |
| 7 | Bank settings: PATCH bank flat / PATCH config flat / PATCH config `updates` | HTTP 200 / 422 / 200 |
| 7 | Config values after setup | `{'reflect_mission': "I am the engineering memory for Acme Pay's payment platform. I prioritise root causes, fixes that failed, and the reasons behind configuration decisions.", 'disposition_skepticism': 4, 'disposition_literalism': 4, 'disposition_empathy': 2, 'enable_observations': True, 'enable_auto_consolidation': True}` |
| 7 | Directives: list / create | HTTP 200 / [200, 200, 200, 200, 200, 200] |
| 7 | Mental model: create / list | HTTP 200 / 200 |
| 7 | Documents: list / delete / get after delete | HTTP 200 / 200 / 404 |

## Recall scores (check 3)

| Change | Top evidence | Reranker | Hits | Expected |
|---|---|---|---|---|
| `spring.datasource.hikari.maximum-pool-size` | ADR-7 | 0.8814109 | 24 | INC-18 / ADR-7 |
| `logging.level.com.acmepay` | INC-13 | 0.41335875 | 24 | INC-13 |
| `payment.retry.max-attempts` | ADR-9 | 0.96295285 | 24 | WARN-42 / ADR-9 |
| `management.endpoint.health.show-details` | INC-31 | 0.029873092 | 24 | nothing |
| `server.compression.enabled` | ADR-9 | 0.2501793 | 24 | nothing |
| `spring.mvc.problemdetails.enabled` | ADR-9 | 0.12951279 | 24 | nothing |

## Ask answer (check 4)

```
- Check for HikariPool connection exhaustion: If logs show connection timeouts, ensure `spring.datasource.hikari.maximum-pool-size` is 20 per replica to stay under the 150 MySQL limit (**INC-18**, **ADR-7**). Note: restarting pods and increasing client timeouts failed to resolve this in the past (**INC-18**).
- Validate `spring.datasource.hikari.connection-timeout`: Ensure this is set to 30000ms; lowering it to 5000ms caused connectivity issues during peak traffic (**INC-31**). Pod restarts were ineffective here as well (**INC-31**).
- Verify idempotency for retries: If you are seeing duplicat
```

## Sample observations (check 6)

- Incident INC-29 occurred in the payment-service on 2026-04-02, where customers were charged twice due to repeated charge requests. The root cause was DEP-2026-0
- Incident INC-31 occurred in the payment-service on 2026-03-12 due to HikariPool connection exhaustion, caused by DEP-2026-017 increasing the maximum-pool-size t
- Incident INC-31 occurred in the payment-service on 2026-05-19 due to connection timeouts at peak traffic. The root cause was DEP-2026-035 (v2.9.0), which lowere

Raw requests and responses are in `spike_raw.json`.
