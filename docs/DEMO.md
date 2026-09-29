# Demo runbook

How to get RECALL-X ready for the live demo, run it, and recover when something goes wrong. The script itself is also in the implementation plan, section 13.

## Before the demo

1. **Start everything** (README, "Run it locally"): MySQL, then the backend, then the frontend.
2. **Memory is seeded and searchable.** Run this, then wait until `recordIds` includes `INC-18`, `ADR-7` and `INC-13`:
   ```
   GET /api/admin/memory/check?q=payment-service maximum-pool-size        (X-Admin-Token header)
   GET /api/admin/memory/check?q=debug logging filled the log volume
   ```
3. **Run the smoke test:** `python tools/smoke_test.py`. It must show 0 failed. It resets the demo when it finishes.
4. **Warm up:** ask the demo question once on the Ask page, so the first live call isn't the slowest. Then reset (below).
5. **Browser:** zoom to 125% for the projector, close other tabs, and turn off notifications. Open the Dashboard.

## The 60-second script

| Time | On screen | Say |
|---|---|---|
| 0–7s | Dashboard: patterns panel and chart | "Six months of simulated history for a payments team. RECALL-X has noticed on its own: pool-size changes keep causing connection errors." |
| 7–22s | Ask: the demo question, both panels | "A plain model says restart the service. With memory: restarting already failed twice, in INC-18 and INC-31. The cause was the connection pool, and ADR-7 says why it's 20." |
| 22–47s | Deploy check: "Load demo change", then Check | "Four changes, four answers. The pool size hits a deliberate decision, ADR-7. Debug logging has no decision on file, but Hindsight remembered INC-13, when it filled a disk. The retry change was flagged in June and judged safe, so RECALL-X says so instead of crying wolf. And the last change? Nothing relevant, so it stays quiet." |
| 47–54s | Click **Useful** on WARN-55 | "Every verdict goes back into memory. That's how WARN-42 taught it." |
| 54–60s | Stay on the cards | "RECALL-X remembers what failed, why things are the way they are, and its own mistakes." |

**Honesty rules**
- Call the history simulated.
- The precision chart is seeded history. Don't present it as proof that the software improved.
- "Is the comparison rigged?" Answer: same role, question and format; the left side is Groq, the right is Hindsight's reflect, and each panel says which.
- Only mention the MCP tool if it's built and working.

## Reset between runs

```bash
curl -X POST -H "X-Admin-Token: <token>" localhost:8080/api/admin/demo/reset
# PowerShell:
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/admin/demo/reset -Headers @{ 'X-Admin-Token' = '<token>' }
```

This deletes everything created after the seed and removes those documents from memory, so the next run gets DEP-2026-061 and WARN-55 again. In rehearsals, always mark WARN-55 **Useful**, as in the live demo.

## If something goes wrong

| Symptom | What to do |
|---|---|
| The memory panel says "Memory unavailable" | Check `HINDSIGHT_API_KEY`, the network, and Hindsight credits (a 402 in the backend log means out of credits). Decision Guard still works from the database, so carry on with the pool-size card and say so. |
| The logging line says "no relevant history" | The score floor is too high for this bank. Lower `HINDSIGHT_MIN_RERANKER` in `.env` and restart the backend. |
| The quiet line shows a warning | The floor is too low, or the quiet key has history. Rerun `tools/hindsight_spike.py`, check 3, and use the key it picks. |
| Reflect is slow | Set `RECALLX_CHECK_BUDGET=mid` and restart the backend. |
| The venue network fails | Switch to the phone hotspot. If that fails too, play the backup video. |

## Bank settings by hand (only if the sync couldn't set them)

The sync report (`POST /api/admin/memory/sync`) lists each setup step. If one says "NOT set", enter it in the Hindsight Cloud UI for bank `recallx-acme`:

- **Mission:** I am the engineering memory for Acme Pay's payment platform. I prioritise root causes, fixes that failed, and the reasons behind configuration decisions.
- **Disposition:** skepticism 4, literalism 4, empathy 2.
- **Directives** (untagged):
  1. Cite the record ID (INC-, ADR-, DEP- or WARN-) for every claim.
  2. If memory holds no relevant record, say so. Never invent an incident, decision, ID or number.
  3. Mention any fix that was tried before and failed.
  4. If an earlier warning on a similar change was marked a false positive, mention it and its reason.
  5. Describe similarity to past events. Never predict that a change will fail.
  6. If a decision has been superseded, use the newer one.
- **Mental model** `payment-config-rules`, with the source query: Which payment-service configuration values were set deliberately, what incident led to each, and what failed before? Refresh it after consolidation.
