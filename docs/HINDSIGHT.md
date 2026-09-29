# How RECALL-X uses Hindsight

RECALL-X is an engineering memory for a payments team. It remembers three things: what failed, why the system is set up the way it is, and its own mistakes. [Hindsight](https://github.com/vectorize-io/hindsight) is where that memory lives and where the reasoning over it happens.

MySQL stays the system of record. Every record is also retained in Hindsight, and every record ID Hindsight hands back is checked against MySQL before anyone sees it. The rule that ties the two together: **the code decides with recall; people read reflect.**

Every Hindsight call (retain, recall, reflect, and the bank setup) goes through one class, [`HindsightClient`](../backend/src/main/java/com/recallx/recallx/memory/hindsight/HindsightClient.java), over Hindsight's REST API, since there is no Java SDK.

## One memory bank, shaped for this job

All records go into one bank (`HINDSIGHT_BANK_ID`, default `recallx-acme`). [`BankSetup`](../backend/src/main/java/com/recallx/recallx/memory/BankSetup.java) configures it on every sync; each step is safe to repeat:

- **Mission:** *I am the engineering memory for Acme Pay's payment platform. I prioritise root causes, fixes that failed, and the reasons behind configuration decisions.*
- **Disposition:** skepticism 4 (don't over-warn), literalism 4 (be exact about config values), empathy 2.
- **Six directives**, untagged so they apply to every reflect call:
  1. Cite the record ID (INC-, ADR-, DEP- or WARN-) for every claim.
  2. If memory holds no relevant record, say so. Never invent an incident, decision, ID or number.
  3. Mention any fix that was tried before and failed.
  4. If an earlier warning on a similar change was marked a false positive, mention it and its reason.
  5. Describe similarity to past events. Never predict that a change will fail.
  6. If a decision has been superseded, use the newer one.
- **One mental model**, `payment-config-rules`: *"Which payment-service configuration values were set deliberately, what incident led to each, and what failed before?"* It refreshes after consolidation, and reflect reads it before anything else.

## Retain: what goes into memory

| Record | When it is retained | Text (shortened) |
|---|---|---|
| Incident | Seed, or `POST /api/incidents` | `[INCIDENT INC-18] ... Attempt 1: Restarted payment-service pods. Outcome: FAILED ...` |
| Decision | Seed | `[DECISION ADR-7] ... Decision: Keep maximum-pool-size at 20 per replica ...` |
| Deployment | Seed, and after every live check | `[DEPLOYMENT DEP-2026-017] ... Changes: spring.datasource.hikari.maximum-pool-size 20 -> 50.` |
| Warning outcome | When an engineer marks a warning | `[WARNING OUTCOME WARN-42] ... Engineer verdict: FALSE POSITIVE ...` |

Each item carries:

- **`document_id` = the record ID.** Re-sending a record replaces it instead of duplicating it, and every recall result can be traced straight back to a MySQL row.
- **`timestamp` = when the event happened**, not when it was loaded. Six months of history arrive in minutes, but "three months ago" still means three months ago.
- **`context`** (the record type) and **`metadata`** (type, ref, service).

Failed attempts are written out as first-class lines, because the dead ends are what the next engineer needs and what normal write-ups drop. Causes are written in plain words ("DEP-2026-017 raised the pool from 20 to 50, which caused INC-18"), because Hindsight's entity graph links things that are mentioned together rather than storing typed "caused by" edges. Config keys always use their canonical names, so the graph connects every record that touches the same setting.

Code: [`MemoryTemplates`](../backend/src/main/java/com/recallx/recallx/memory/MemoryTemplates.java), [`MemorySyncService`](../backend/src/main/java/com/recallx/recallx/memory/MemorySyncService.java).

## Recall: evidence the code can check

When a change touches a key that no active decision governs, RECALL-X first asks recall whether memory holds anything relevant:

```json
POST /v1/default/banks/recallx-acme/memories/recall
{
  "query": "payment-service: change logging.level.com.acmepay from INFO to DEBUG",
  "types": ["world", "experience"],
  "budget": "mid",
  "max_tokens": 2048,
  "min_scores": { "reranker": 0.3 }
}
```

- The query describes the change itself. Generic words like "incidents" or "failed fixes" would match every incident.
- `min_scores.reranker` drops weak matches inside Hindsight. The floor (`HINDSIGHT_MIN_RERANKER`) is set by measuring real scores for the demo changes and for unrelated keys with `tools/hindsight_spike.py`.
- Each result's `document_id` names the record it came from. Only incident, decision and warning IDs that exist in MySQL count; a past deployment alone is not history.

Nothing left means **no relevant history**, and RECALL-X stays quiet without calling reflect at all.

Recall with `types: ["observation"]` and `include.source_facts` feeds the dashboard's "Patterns Hindsight noticed": beliefs Hindsight consolidated on its own from several memories, shown with the records they were built from.

## Reflect: the words people read

Reflect writes everything people read: the "with memory" answer on the Ask page, and the wording of each warning. For warnings it returns structured output:

```json
{
  "matches_history": true,
  "summary": "Turning on DEBUG logging resembles INC-13, when debug logging left on filled the log volume.",
  "cited_ids": ["INC-13"],
  "failed_attempts": [],
  "prior_false_positive": "",
  "recommendation": "Set log rotation before raising the log level."
}
```

Reflect's words are used; its lists are checked. Testing against real Hindsight showed that `cited_ids` lists nearly every record reflect read, while the summary names the ones that matter, so citations come from the IDs in the summary. Failed fixes come from MySQL, for the incidents the warning is based on. An "earlier false positive" note is kept only when it names a false positive on the same setting.

A warning found by memory alone is only shown when reflect says the history matches **and** at least one incident appears in both recall's results and reflect's summary. Every request asks for `based_on`, so each answer shows its sources: how many memories reflect used, the mental model, and the directives it applied.

Code: [`DecisionGuardService`](../backend/src/main/java/com/recallx/recallx/guard/DecisionGuardService.java), [`AskService`](../backend/src/main/java/com/recallx/recallx/service/AskService.java).

## The learning loop

1. A warning is shown with the records it is based on.
2. The engineer marks it Useful, False positive (a reason is required) or Ignore.
3. The verdict is saved in MySQL and retained in Hindsight as a warning outcome, under the warning's own ID.
4. Directive 4 makes reflect mention an earlier false positive on a similar change.
5. When the same change (same key, same new value) comes up again and its newest verdict was a false positive, the card becomes **"Previously judged safe"**: it names the earlier warning, the engineer's reason, and any decision that still applies, instead of raising the same alarm.
6. The dashboard tracks precision per month: useful ÷ (useful + false positive).

In the demo, WARN-42 (June) flagged raising `payment.retry.max-attempts` from 3 to 4, and an engineer marked it a false positive because idempotency was on. Checking the same change again gets the "Previously judged safe" card.

## Safeguards

- **Never invent history.** Every cited ID is checked against MySQL; invented IDs are dropped, and a warning with no real reference is not shown.
- **Decision Guard doesn't depend on AI.** An active decision on a changed key always produces a warning from a database lookup, even if Hindsight or the model is down; reflect only words it.
- **Severity comes from the database.** A Decision Guard warning takes the severity of the decision's linked incident; a memory-only warning takes the worst incident that recall and reflect agree on.
- **Failures are said out loud.** If Hindsight is unreachable, keys without a decision show "memory unavailable" (never "no history"), and the Ask page shows "Memory unavailable" instead of the memory-free answer.
- **Honest comparison.** The Ask page's two answers get the same role, question and format limit ([`Prompts`](../backend/src/main/java/com/recallx/recallx/llm/Prompts.java)). They run on different models (the memory-off side on Groq or Gemini, the memory side on Hindsight's reflect), and each panel names its model.
