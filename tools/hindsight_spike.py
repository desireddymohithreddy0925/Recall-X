#!/usr/bin/env python3
"""
RECALL-X Phase 0: Hindsight test (implementation plan, section 4).

Checks the Hindsight Cloud behaviour the RECALL-X design depends on, using a throwaway
bank (never recallx-acme). Standard library only; needs Python 3.9 or newer.

    python tools/hindsight_spike.py                 # full run, about 10 minutes
    python tools/hindsight_spike.py --quick         # skip the wait for observations
    python tools/hindsight_spike.py --delete-bank   # also delete the throwaway bank at the end

Reads HINDSIGHT_URL, HINDSIGHT_API_KEY and SPIKE_BANK_ID from the repository's .env (or a .env next to this file,
or the environment). Writes docs/SPIKE_RESULTS.md and tools/spike_raw.json (the raw file is git-ignored).
"""
import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ID_RE = re.compile(r"\b(INC-\d+|ADR-\d+|DEP-\d{4}-\d{3}|WARN-\d+)\b")
EVIDENCE_PREFIXES = ("INC-", "ADR-", "WARN-")
BANK = "/v1/default/banks/{bank}"

SERVICE = "payment-service"
POOL = "spring.datasource.hikari.maximum-pool-size"
CONN = "spring.datasource.hikari.connection-timeout"
RETRY = "payment.retry.max-attempts"
IDEM = "payment.retry.idempotency-enabled"
LOG = "logging.level.com.acmepay"

MISSION = ("I am the engineering memory for Acme Pay's payment platform. I prioritise root causes, "
           "fixes that failed, and the reasons behind configuration decisions.")

DIRECTIVES = [
    ("Cite record IDs", "Cite the record ID (INC-, ADR-, DEP- or WARN-) for every claim."),
    ("Never invent history", "If memory holds no relevant record, say so. Never invent an incident, decision, ID or number."),
    ("Name failed fixes", "Mention any fix that was tried before and failed."),
    ("Mention false positives", "If an earlier warning on a similar change was marked a false positive, mention it and its reason."),
    ("Similarity, not prediction", "Describe similarity to past events. Never predict that a change will fail."),
    ("Newest decision wins", "If a decision has been superseded, use the newer one."),
]

WARNING_SCHEMA = {
    "type": "object",
    "properties": {
        "matches_history": {"type": "boolean", "description": "true only if a cited record involves the same setting, component or failure mode as this change"},
        "summary": {"type": "string", "description": "2-3 sentences, citing IDs"},
        "cited_ids": {"type": "array", "items": {"type": "string"}, "description": "Every INC-, ADR-, DEP- or WARN- ID used"},
        "failed_attempts": {"type": "array", "items": {"type": "string"}, "description": "Fixes that were tried before and did not work"},
        "prior_false_positive": {"type": "string", "description": "An earlier similar warning marked false positive, and why; empty if none"},
        "recommendation": {"type": "string", "description": "One sentence on what to check before shipping"},
    },
    "required": ["matches_history", "summary", "cited_ids", "failed_attempts", "recommendation"],
}


# ---------------------------------------------------------------- records (same wording as the seed)

def incident(rid, date, sev, symptom, cause, keys, attempts, lesson):
    lines = [f"[INCIDENT {rid}] service={SERVICE} date={date} severity={sev}",
             f"Symptom: {symptom}", f"Root cause: {cause}"]
    if keys:
        lines.append("Config keys involved: " + ", ".join(keys))
    for i, (action, outcome, note) in enumerate(attempts, 1):
        lines.append(f"Attempt {i}: {action}. Outcome: {outcome}." + (f" {note}" if note else ""))
    lines.append(f"Lesson: {lesson}")
    return "\n".join(lines)


def decision(rid, date, key, text, reason, linked):
    return "\n".join([f"[DECISION {rid}] service={SERVICE} date={date} status=ACTIVE key={key}",
                      f"Decision: {text}", f"Reason: {reason}", f"Linked incident: {linked}"])


RECORDS = [
    dict(id="INC-13", type="incident", ts="2026-03-07T14:10:00Z", content=incident(
        "INC-13", "2026-03-07", "SEV3",
        "Pods restarted with No space left on device. The log volume was full.",
        "Debug logging left on in the refund module filled the node's log volume.", [],
        [("Deleted old log files by hand", "PARTIAL", "Disk filled again within an hour"),
         ("Turned the refund module back to INFO and set log rotation to 500 MB", "RESOLVED", "")],
        "Always set log rotation, and turn debug logging off after an investigation.")),
    dict(id="INC-18", type="incident", ts="2026-03-12T09:40:00Z", content=incident(
        "INC-18", "2026-03-12", "SEV2",
        "Payment API requests timed out. Logs: HikariPool-1 - Connection is not available, request timed out after 30000ms.",
        f"DEP-2026-017 (v2.4.0) raised {POOL} from 20 to 50. 4 replicas x 50 = 200 connections, above MySQL "
        "max_connections of 150, so MySQL rejected connections with error 1040 Too many connections.", [POOL],
        [("Restarted payment-service pods", "FAILED", "Timeouts returned within 10 minutes"),
         ("Raised the client timeout to 60s", "PARTIAL", "Fewer errors, root cause unchanged"),
         ("Reverted maximum-pool-size to 20", "RESOLVED", "")],
        "Replicas x maximum-pool-size must stay below MySQL max_connections.")),
    dict(id="INC-29", type="incident", ts="2026-04-02T13:05:00Z", content=incident(
        "INC-29", "2026-04-02", "SEV1",
        "Customers were charged twice. Gateway logs showed repeated charge requests for the same order.",
        f"DEP-2026-024 (v2.7.0) raised {RETRY} from 3 to 5 while {IDEM} was false, so retries after gateway "
        "timeouts created duplicate charges.", [RETRY, IDEM],
        [("Lowered the gateway timeout to 5000", "FAILED", "Duplicate charges continued"),
         ("Reverted max-attempts to 3", "RESOLVED", "Duplicates stopped. Affected orders refunded")],
        "Retries must be idempotent. Never raise retries without idempotency keys.")),
    dict(id="INC-31", type="incident", ts="2026-05-19T12:15:00Z", content=incident(
        "INC-31", "2026-05-19", "SEV2",
        "HikariPool-1 - Connection is not available, request timed out after 5000ms at peak traffic.",
        f"DEP-2026-035 (v2.9.0) lowered {CONN} from 30000 to 5000, too short for peak-time connection waits.", [CONN],
        [("Restarted payment-service pods", "FAILED", "Errors returned at the next traffic peak"),
         ("Reverted connection-timeout to 30000", "RESOLVED", "")],
        "A restart does not fix connection pool configuration. Check recent pool setting changes first.")),
    dict(id="ADR-7", type="decision", ts="2026-03-15T11:00:00Z", content=decision(
        "ADR-7", "2026-03-15", POOL, "Keep maximum-pool-size at 20 per replica.",
        "After INC-18, replicas x pool size must stay below MySQL max_connections of 150, with headroom for ledger-service.",
        "INC-18")),
    dict(id="ADR-9", type="decision", ts="2026-04-06T11:00:00Z", content=decision(
        "ADR-9", "2026-04-06", RETRY, f"Retries above 3 require {IDEM} to be true.",
        "INC-29: retries without idempotency caused duplicate charges.", "INC-29")),
    dict(id="WARN-42", type="warning_outcome", ts="2026-06-16T10:30:00Z", content="\n".join([
        f"[WARNING OUTCOME WARN-42] deployment=DEP-2026-041 date=2026-06-16 key={RETRY}",
        f"RECALL-X warned: Changing {RETRY} from 3 to 4 resembles INC-29, where extra retries caused duplicate "
        "charges. ADR-9 governs this setting.",
        f"Engineer verdict: FALSE POSITIVE: {IDEM} is true, so the INC-29 duplicate-charge risk did not apply."])),
    dict(id="SPIKE-TEMP", type="spike", ts="2026-09-01T00:00:00Z",
         content="[SPIKE TEMP] This record exists only to test document deletion. It is not part of any history."),
]

# Changes scored in check 3. "gate" = goes through the recall check in the real app.
CHANGES = [
    dict(label="pool", key=POOL, old="20", new="60", gate=False, expect="INC-18 / ADR-7"),
    dict(label="logging", key=LOG, old="INFO", new="DEBUG", gate=True, expect="INC-13"),
    dict(label="retry", key=RETRY, old="3", new="4", gate=False, expect="WARN-42 / ADR-9"),
    dict(label="quiet-1", key="management.endpoint.health.show-details", old="never", new="always", gate=True, expect="nothing"),
    dict(label="quiet-2", key="server.compression.enabled", old="false", new="true", gate=True, expect="nothing"),
    dict(label="quiet-3", key="spring.mvc.problemdetails.enabled", old="false", new="true", gate=True, expect="nothing"),
]
QUIET = [c for c in CHANGES if c["label"].startswith("quiet")]


def change_query(c):
    return f"{SERVICE}: change {c['key']} from {c['old']} to {c['new']}"


def warning_prompt(c, decision_text=None):
    parts = [f"A deployment to {SERVICE} changes {c['key']} from {c['old']} to {c['new']}."]
    if decision_text:
        parts.append(f"This key is governed by {decision_text}")
    parts.append("Using this team's history, explain whether this change resembles past incidents or undoes a past "
                 "decision. Name any fixes that failed before, and any earlier warning on a similar change that was "
                 "marked a false positive, with its reason. Cite IDs for every claim. Describe similarity; do not "
                 "predict that anything will fail.")
    return "\n".join(parts)


ASK_PROMPT = ("You are an on-call assistant for Acme Pay's payment-service.\n"
              "Question: The payment API is timing out. What should I do?\n"
              "Use this team's history: which past incidents match, which fixes failed, what resolved them, and which "
              "decisions apply. Cite the record ID for each point.\n"
              "Answer in at most 5 short bullet points.")


# ---------------------------------------------------------------- HTTP

class Response:
    def __init__(self, status, data, text, seconds):
        self.status, self.data, self.text, self.seconds = status, data, text, seconds

    @property
    def ok(self):
        return 200 <= self.status < 300

    def brief(self):
        return f"HTTP {self.status}" + ("" if self.ok else f" {self.text[:160]!r}")


class Hindsight:
    def __init__(self, base_url, api_key, bank):
        self.base, self.key, self.bank = base_url.rstrip("/"), api_key, bank
        self.exchanges = []

    def call(self, method, path, body=None, timeout=180):
        path = path.format(bank=self.bank)
        data = None if body is None else json.dumps(body).encode("utf-8")
        req = urllib.request.Request(self.base + path, data=data, method=method)
        req.add_header("Authorization", "Bearer " + self.key)
        req.add_header("Accept", "application/json")
        if data is not None:
            req.add_header("Content-Type", "application/json")
        start = time.monotonic()
        try:
            with urllib.request.urlopen(req, timeout=timeout) as res:
                status, text = res.status, res.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            status, text = e.code, e.read().decode("utf-8", "replace")
        except (urllib.error.URLError, TimeoutError, OSError) as e:
            status, text = 0, f"{type(e).__name__}: {e}"
        seconds = round(time.monotonic() - start, 2)
        try:
            payload = json.loads(text) if text else None
        except json.JSONDecodeError:
            payload = None
        self.exchanges.append({"method": method, "path": path, "request": body, "status": status,
                               "seconds": seconds, "response": payload if payload is not None else text[:4000]})
        return Response(status, payload, text, seconds)

    def recall(self, query, types=("world", "experience"), budget="mid", max_tokens=2048, **extra):
        body = {"query": query, "types": list(types), "budget": budget, "max_tokens": max_tokens}
        body.update(extra)
        return self.call("POST", BANK + "/memories/recall", body)

    def reflect(self, query, budget="mid", **extra):
        body = {"query": query, "budget": budget}
        body.update(extra)
        return self.call("POST", BANK + "/reflect", body)


def results_of(res):
    if res.ok and isinstance(res.data, dict) and isinstance(res.data.get("results"), list):
        return res.data["results"]
    return []


def record_id(hit):
    """The record a recall hit came from: document_id if Hindsight returns it, else an ID in the text."""
    doc = hit.get("document_id")
    if doc:
        return doc, "document_id"
    found = ID_RE.findall(hit.get("text") or "")
    return (found[0], "text") if found else (None, None)


def reranker(hit):
    scores = hit.get("scores") or {}
    value = scores.get("reranker")
    if value is None:
        value = scores.get("final")
    return value


def log(msg):
    print(msg, flush=True)


# ---------------------------------------------------------------- checks

def setup_bank(hs, out):
    log("[setup] Creating the throwaway bank")
    r = hs.call("PUT", BANK, {"name": "RECALL-X spike", "mission": MISSION,
                              "disposition": {"skepticism": 4, "literalism": 4, "empathy": 2}})
    out["bank_put_nested"] = r.status
    if not r.ok:
        r2 = hs.call("POST", BANK, {})
        out["bank_post_create"] = r2.status
    log(f"        PUT with nested disposition (code pack shape): HTTP {out['bank_put_nested']}"
        + (f"; POST create: HTTP {out.get('bank_post_create')}" if "bank_post_create" in out else ""))


def check_retain(hs, out):
    log("[1/7] Retaining the test records (async: false)")
    items = [{"content": r["content"], "context": r["type"], "timestamp": r["ts"], "document_id": r["id"],
              "metadata": {"type": r["type"], "ref": r["id"], "service": SERVICE}} for r in RECORDS]
    body = {"items": items, "async": False}
    for path in (BANK + "/memories", BANK + "/memories/retain"):
        r = hs.call("POST", path, body, timeout=600)
        if r.status not in (404, 405):
            break
    out["retain_path"] = path.replace("{bank}", "{bank_id}")
    out["retain_status"] = r.status
    out["retain_seconds"] = r.seconds
    out["retain_usage"] = (r.data or {}).get("usage") if isinstance(r.data, dict) else None
    log(f"        {out['retain_path']}: {r.brief()} in {r.seconds}s")
    if r.ok:
        out["retain_accepts_document_id_and_metadata"] = True
        return True
    if r.status in (400, 422):
        # Find out which field was rejected.
        for drop in ("metadata", "document_id"):
            trimmed = [{k: v for k, v in item.items() if k != drop} for item in items]
            r2 = hs.call("POST", path, {"items": trimmed, "async": False}, timeout=600)
            out[f"retain_without_{drop}"] = r2.status
            if r2.ok:
                out["retain_accepts_document_id_and_metadata"] = False
                out["retain_rejected_field"] = drop
                log(f"        Retain works without {drop}: that field is rejected")
                return True
    out["retain_accepts_document_id_and_metadata"] = False
    return False


def configure_bank(hs, out):
    log("[setup] Bank mission, disposition, directives, mental model")
    flat = {"reflect_mission": MISSION, "disposition_skepticism": 4, "disposition_literalism": 4,
            "disposition_empathy": 2}
    out["bank_patch_flat"] = hs.call("PATCH", BANK, flat).status
    out["config_patch_flat"] = hs.call("PATCH", BANK + "/config", flat).status
    if out["config_patch_flat"] not in range(200, 300):
        out["config_patch_updates"] = hs.call("PATCH", BANK + "/config", {"updates": flat}).status
    cfg = hs.call("GET", BANK + "/config")
    out["config_get"] = cfg.status
    if cfg.ok and isinstance(cfg.data, dict):
        source = cfg.data.get("config", cfg.data)
        out["config_values"] = {k: source.get(k) for k in (
            "reflect_mission", "disposition_skepticism", "disposition_literalism", "disposition_empathy",
            "enable_observations", "enable_auto_consolidation") if k in source}
    log(f"        PATCH bank (flat): HTTP {out['bank_patch_flat']}; PATCH config: HTTP {out['config_patch_flat']}"
        + (f"; PATCH config with 'updates': HTTP {out['config_patch_updates']}" if "config_patch_updates" in out else "")
        + f"; GET config: HTTP {out['config_get']}")
    if out.get("config_values"):
        log(f"        config: {out['config_values']}")

    existing = hs.call("GET", BANK + "/directives")
    out["directives_list"] = existing.status
    names = set()
    if existing.ok:
        rows = existing.data.get("directives", existing.data.get("items", [])) if isinstance(existing.data, dict) else existing.data
        names = {d.get("name") for d in (rows or []) if isinstance(d, dict)}
    statuses = []
    for name, content in DIRECTIVES:
        if name in names:
            statuses.append("exists")
            continue
        statuses.append(hs.call("POST", BANK + "/directives", {"name": name, "content": content}).status)
    out["directives_create"] = statuses
    log(f"        directives: list HTTP {existing.status}; create {statuses}")

    mm = {"id": "payment-config-rules", "name": "payment-service configuration rules",
          "source_query": "Which payment-service configuration values were set deliberately, what incident led to "
                          "each, and what failed before?",
          "trigger": {"refresh_after_consolidation": True}}
    r = hs.call("POST", BANK + "/mental-models", mm)
    if r.status in (400, 422):
        mm.pop("id")
        r = hs.call("POST", BANK + "/mental-models", mm)
        out["mental_model_custom_id"] = False
    out["mental_model_create"] = r.status
    out["mental_models_list"] = hs.call("GET", BANK + "/mental-models").status
    log(f"        mental model: create HTTP {out['mental_model_create']}; list HTTP {out['mental_models_list']}")


def check_delay(hs, out, max_wait):
    log(f"[2/7] Waiting until recall returns INC-18 (up to {max_wait}s)")
    start = time.monotonic()
    while True:
        r = hs.recall(f"{SERVICE} maximum-pool-size")
        hits = results_of(r)
        ids = [record_id(h)[0] for h in hits]
        if "INC-18" in ids:
            out["seconds_until_recallable"] = round(time.monotonic() - start, 1)
            first = hits[0]
            out["recall_result_fields"] = sorted(first.keys())
            out["recall_returns_document_id"] = any(h.get("document_id") for h in hits)
            out["recall_returns_metadata"] = any(h.get("metadata") for h in hits)
            out["recall_returns_scores"] = any(h.get("scores") for h in hits)
            out["ids_in_fact_text"] = sum(1 for h in hits if ID_RE.search(h.get("text") or "")) / max(len(hits), 1)
            log(f"        INC-18 found after {out['seconds_until_recallable']}s. document_id returned: "
                f"{out['recall_returns_document_id']}, scores returned: {out['recall_returns_scores']}, "
                f"share of facts that still contain a record ID: {out['ids_in_fact_text']:.0%}")
            return
        if not r.ok:
            log(f"        recall failed: {r.brief()}")
        if time.monotonic() - start > max_wait:
            out["seconds_until_recallable"] = None
            log("        INC-18 never appeared")
            return
        time.sleep(5)


def check_scores(hs, out):
    log("[3/7] Recall scores for each change (no floor)")
    table = []
    for c in CHANGES:
        r = hs.recall(change_query(c))
        hits = results_of(r)
        rows = []
        for h in hits:
            rid, via = record_id(h)
            rows.append({"id": rid, "via": via, "type": h.get("type"), "reranker": reranker(h),
                         "final": (h.get("scores") or {}).get("final"), "text": (h.get("text") or "")[:100]})
        evidence = [row for row in rows if row["id"] and row["id"].startswith(EVIDENCE_PREFIXES)
                    and row["reranker"] is not None]
        top = max(evidence, key=lambda row: row["reranker"]) if evidence else None
        entry = {"label": c["label"], "key": c["key"], "status": r.status, "hits": len(hits),
                 "top_id": top["id"] if top else None, "top_reranker": top["reranker"] if top else None,
                 "expect": c["expect"], "rows": rows[:6]}
        table.append(entry)
        log(f"        {c['label']:<8} top={entry['top_id']!s:<8} reranker={entry['top_reranker']!s:<8} "
            f"hits={entry['hits']:<3} expected {c['expect']}")
    out["scores"] = table

    by_label = {e["label"]: e for e in table}
    logging_top = by_label["logging"]["top_reranker"]
    quiet_tops = {e["label"]: (e["top_reranker"] or 0.0) for e in table if e["label"].startswith("quiet")}
    quiet_label = min(quiet_tops, key=quiet_tops.get)
    out["quiet_key"] = by_label[quiet_label]["key"]
    best_quiet = max(quiet_tops.values())
    if logging_top is not None and logging_top > best_quiet:
        out["min_reranker"] = round((logging_top + best_quiet) / 2, 3)
        out["floor_separates"] = True
    else:
        out["min_reranker"] = None
        out["floor_separates"] = False
    out["logging_top_is_inc13"] = by_label["logging"]["top_id"] == "INC-13"
    log(f"        quiet key: {out['quiet_key']}; floor separates logging from quiet keys: {out['floor_separates']}"
        + (f"; suggested floor {out['min_reranker']}" if out["min_reranker"] is not None else ""))

    floor = out["min_reranker"] if out["min_reranker"] is not None else 0.99
    quiet_change = next(c for c in QUIET if c["key"] == out["quiet_key"])
    r = hs.recall(change_query(quiet_change), min_scores={"reranker": floor})
    out["min_scores_status"] = r.status
    out["min_scores_hits_for_quiet_key"] = len(results_of(r))
    logging_change = next(c for c in CHANGES if c["label"] == "logging")
    r2 = hs.recall(change_query(logging_change), min_scores={"reranker": floor})
    out["min_scores_hits_for_logging"] = len(results_of(r2))
    log(f"        with min_scores.reranker={floor}: HTTP {r.status}, quiet key hits {out['min_scores_hits_for_quiet_key']}, "
        f"logging hits {out['min_scores_hits_for_logging']}")


def summarise_reflect(r):
    data = r.data if isinstance(r.data, dict) else {}
    based_on = data.get("based_on") if isinstance(data.get("based_on"), dict) else {}
    structured = data.get("structured_output") if isinstance(data.get("structured_output"), dict) else None
    text = data.get("text") or ""
    return {
        "status": r.status, "seconds": r.seconds,
        "based_on_keys": sorted(based_on.keys()),
        "based_on_counts": {k: len(v) for k, v in based_on.items() if isinstance(v, list)},
        "structured_output": structured,
        "structured_output_error": data.get("structured_output_error"),
        "ids_in_text": sorted(set(ID_RE.findall(text))),
        "text": text[:600],
        "total_tokens": (data.get("usage") or {}).get("total_tokens"),
    }


def check_reflect_shape(hs, out):
    log("[4/7] Reflect: include.facts and context (the Ask demo question)")
    r = hs.reflect(ASK_PROMPT, budget="mid", include={"facts": {}})
    if r.status in (400, 422):
        r = hs.reflect(ASK_PROMPT, budget="mid", include={"facts": True})
        out["include_facts_shape"] = '{"facts": true}'
    else:
        out["include_facts_shape"] = '{"facts": {}}'
    ask = summarise_reflect(r)
    out["ask"] = ask
    out["based_on_returned"] = bool(ask["based_on_keys"])
    log(f"        {r.brief()} in {r.seconds}s; based_on keys {ask['based_on_keys']} {ask['based_on_counts']}; "
        f"IDs in answer {ask['ids_in_text']}")
    mentions_restart = "restart" in ask["text"].lower()
    out["ask_cites_inc18_and_restart"] = "INC-18" in ask["ids_in_text"] and mentions_restart
    c = hs.reflect("Which incidents involved the connection pool?", budget="low", context="Phase 0 context field test")
    out["reflect_accepts_context"] = c.ok
    log(f"        context field: HTTP {c.status}")


def check_reflect_latency(hs, out):
    log("[5/7] Reflect with the warning schema (latency, matches_history, WARN-42)")
    cases = [
        ("pool-mid", "pool", "mid", "ADR-7: Keep maximum-pool-size at 20 per replica."),
        ("pool-high", "pool", "high", "ADR-7: Keep maximum-pool-size at 20 per replica."),
        ("logging-mid", "logging", "mid", None),
        ("retry-mid", "retry", "mid", f"ADR-9: Retries above 3 require {IDEM} to be true."),
        ("quiet-mid", None, "mid", None),
    ]
    by_label = {c["label"]: c for c in CHANGES}
    runs = {}
    for name, label, budget, decision_text in cases:
        change = by_label[label] if label else next(c for c in QUIET if c["key"] == out["quiet_key"])
        r = hs.reflect(warning_prompt(change, decision_text), budget=budget, response_schema=WARNING_SCHEMA,
                       include={"facts": {}} if out.get("include_facts_shape") != '{"facts": true}' else {"facts": True})
        s = summarise_reflect(r)
        runs[name] = s
        so = s["structured_output"] or {}
        log(f"        {name:<12} {r.brief()} {s['seconds']}s matches_history={so.get('matches_history')!s:<5} "
            f"cited={so.get('cited_ids')} tokens={s['total_tokens']}"
            + (f" error={s['structured_output_error']!r}" if s["structured_output_error"] else ""))
    out["reflect_runs"] = runs
    high = runs["pool-high"]["seconds"]
    out["check_budget"] = "high" if runs["pool-high"]["status"] == 200 and high <= 12 else "mid"
    retry = runs["retry-mid"]["structured_output"] or {}
    retry_text = json.dumps(retry) + runs["retry-mid"]["text"]
    out["retry_mentions_warn42"] = "WARN-42" in retry_text
    quiet = runs["quiet-mid"]["structured_output"] or {}
    logging_run = runs["logging-mid"]["structured_output"] or {}
    out["quiet_matches_history_false"] = quiet.get("matches_history") is False
    out["logging_matches_history_true"] = logging_run.get("matches_history") is True
    out["logging_cites_inc13"] = "INC-13" in (logging_run.get("cited_ids") or []) or "INC-13" in runs["logging-mid"]["text"]


def check_observations(hs, out, max_wait):
    log(f"[6/7] Observations (waiting up to {max_wait}s for consolidation)")
    start = time.monotonic()
    while True:
        r = hs.recall(f"recurring failure patterns in {SERVICE}", types=("observation",), budget="low",
                      max_tokens=1024, include={"source_facts": {}})
        hits = results_of(r)
        if hits or not r.ok or time.monotonic() - start > max_wait:
            break
        time.sleep(20)
    out["observations_status"] = r.status
    out["observations_count"] = len(hits)
    out["observations_seconds"] = round(time.monotonic() - start, 1) if hits else None
    out["observations_have_source_fact_ids"] = any(h.get("source_fact_ids") for h in hits)
    source_facts = (r.data or {}).get("source_facts") if isinstance(r.data, dict) else None
    docs = set()
    if isinstance(source_facts, dict):
        for fact in source_facts.values():
            if isinstance(fact, dict) and fact.get("document_id"):
                docs.add(fact["document_id"])
    out["observation_source_documents"] = sorted(docs)
    out["observations_sample"] = [(h.get("text") or "")[:160] for h in hits[:3]]
    log(f"        {r.brief()}: {len(hits)} observations; source_fact_ids: {out['observations_have_source_fact_ids']}; "
        f"source documents: {out['observation_source_documents']}")


def check_documents(hs, out):
    log("[7/7] Documents and operations")
    out["documents_list"] = hs.call("GET", BANK + "/documents").status
    out["document_delete"] = hs.call("DELETE", BANK + "/documents/SPIKE-TEMP").status
    out["document_get_after_delete"] = hs.call("GET", BANK + "/documents/SPIKE-TEMP").status
    out["operations_list"] = hs.call("GET", BANK + "/operations").status
    log(f"        list HTTP {out['documents_list']}; delete SPIKE-TEMP HTTP {out['document_delete']}; "
        f"get after delete HTTP {out['document_get_after_delete']}; operations HTTP {out['operations_list']}")


# ---------------------------------------------------------------- report

def yes_no(value):
    return {True: "yes", False: "no", None: "unknown"}.get(value, str(value))


def write_report(out, path):
    s = out
    floor = s.get("min_reranker")
    lines = [
        "# Phase 0 results: Hindsight test",
        "",
        f"Run: {s['run_at']} · bank `{s['bank']}` · {s['base_url']}",
        "",
        "## Values for `.env`",
        "",
        "```properties",
        f"HINDSIGHT_RETAIN_PATH={(s.get('retain_path') or '/memories').replace('/v1/default/banks/{bank_id}', '')}",
        f"HINDSIGHT_MIN_RERANKER={floor if floor is not None else 'KEEP-LOW (floor does not separate; see check 3)'}",
        f"RECALLX_CHECK_BUDGET={s.get('check_budget', 'mid')}",
        "```",
        "",
        f"Quiet demo key: `{s.get('quiet_key')}` (the demo change and tools/smoke_test.py use "
        "`management.endpoint.health.show-details`; if this differs, update DEMO_DIFF in frontend/src/pages/DeployCheck.jsx and QUIET "
        "in tools/smoke_test.py)",
        "",
        "## Checks",
        "",
        "| # | Check | Result |",
        "|---|---|---|",
        f"| 1 | Retain path | `{s.get('retain_path')}` (HTTP {s.get('retain_status')}, {s.get('retain_seconds')} s) |",
        f"| 1 | Retain accepts `document_id` and `metadata` | {yes_no(s.get('retain_accepts_document_id_and_metadata'))}"
        + (f" (rejected: `{s['retain_rejected_field']}`)" if s.get("retain_rejected_field") else "") + " |",
        f"| 1 | Recall returns `document_id` / `metadata` / `scores` | {yes_no(s.get('recall_returns_document_id'))} / "
        f"{yes_no(s.get('recall_returns_metadata'))} / {yes_no(s.get('recall_returns_scores'))} |",
        f"| 1 | Share of recalled facts that still contain a record ID | "
        f"{'{:.0%}'.format(s['ids_in_fact_text']) if s.get('ids_in_fact_text') is not None else 'unknown'} |",
        f"| 2 | Seconds from retain until INC-18 is recallable | {s.get('seconds_until_recallable')} |",
        f"| 3 | Score floor separates the logging change from the quiet keys | {yes_no(s.get('floor_separates'))} |",
        f"| 3 | Logging change's top match is INC-13 | {yes_no(s.get('logging_top_is_inc13'))} |",
        f"| 3 | `min_scores` accepted; hits above the floor for quiet key / logging | HTTP {s.get('min_scores_status')}; "
        f"{s.get('min_scores_hits_for_quiet_key')} / {s.get('min_scores_hits_for_logging')} |",
        f"| 4 | Reflect returns `based_on` with `include.facts` | {yes_no(s.get('based_on_returned'))} "
        f"(shape `{s.get('include_facts_shape')}`, keys {s.get('ask', {}).get('based_on_keys')}) |",
        f"| 4 | Reflect accepts `context` | {yes_no(s.get('reflect_accepts_context'))} |",
        f"| 4 | Ask demo question cites INC-18 and mentions the restart | {yes_no(s.get('ask_cites_inc18_and_restart'))} |",
    ]
    runs = s.get("reflect_runs", {})
    for name in ("pool-mid", "pool-high", "logging-mid", "retry-mid", "quiet-mid"):
        run = runs.get(name)
        if run:
            lines.append(f"| 5 | Reflect `{name}` latency | {run['seconds']} s (HTTP {run['status']}, {run['total_tokens']} tokens) |")
    lines += [
        f"| 5 | Logging change: `matches_history` true and cites INC-13 | {yes_no(s.get('logging_matches_history_true'))} / "
        f"{yes_no(s.get('logging_cites_inc13'))} |",
        f"| 5 | Quiet key: `matches_history` false | {yes_no(s.get('quiet_matches_history_false'))} |",
        f"| 5 | Retry change mentions WARN-42 (directive 4) | {yes_no(s.get('retry_mentions_warn42'))} |",
        f"| 6 | Observations after seeding | {s.get('observations_count')} "
        f"(after {s.get('observations_seconds')} s; source_fact_ids {yes_no(s.get('observations_have_source_fact_ids'))}; "
        f"source documents {s.get('observation_source_documents')}) |",
        f"| 7 | Bank create: PUT nested (code pack) / POST | HTTP {s.get('bank_put_nested')} / {s.get('bank_post_create', 'not tried')} |",
        f"| 7 | Bank settings: PATCH bank flat / PATCH config flat / PATCH config `updates` | HTTP {s.get('bank_patch_flat')} / "
        f"{s.get('config_patch_flat')} / {s.get('config_patch_updates', 'not tried')} |",
        f"| 7 | Config values after setup | `{s.get('config_values')}` |",
        f"| 7 | Directives: list / create | HTTP {s.get('directives_list')} / {s.get('directives_create')} |",
        f"| 7 | Mental model: create / list | HTTP {s.get('mental_model_create')} / {s.get('mental_models_list')}"
        + (" (custom id rejected)" if s.get("mental_model_custom_id") is False else "") + " |",
        f"| 7 | Documents: list / delete / get after delete | HTTP {s.get('documents_list')} / {s.get('document_delete')} / "
        f"{s.get('document_get_after_delete')} |",
        "",
        "## Recall scores (check 3)",
        "",
        "| Change | Top evidence | Reranker | Hits | Expected |",
        "|---|---|---|---|---|",
    ]
    for e in s.get("scores", []):
        lines.append(f"| `{e['key']}` | {e['top_id']} | {e['top_reranker']} | {e['hits']} | {e['expect']} |")
    lines += ["", "## Ask answer (check 4)", "", "```", s.get("ask", {}).get("text", ""), "```", ""]
    if s.get("observations_sample"):
        lines += ["## Sample observations (check 6)", ""] + [f"- {t}" for t in s["observations_sample"]] + [""]
    lines += ["Raw requests and responses are in `spike_raw.json`.", ""]
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


# ---------------------------------------------------------------- main

def main():
    parser = argparse.ArgumentParser(description="RECALL-X Phase 0 Hindsight test")
    parser.add_argument("--quick", action="store_true", help="skip the wait for observations")
    parser.add_argument("--observation-wait", type=int, default=300, help="seconds to wait for observations")
    parser.add_argument("--recall-wait", type=int, default=180, help="seconds to wait for INC-18 to be recallable")
    parser.add_argument("--delete-bank", action="store_true", help="delete the throwaway bank at the end")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="replace")   # Windows consoles can't print every character

    for env_path in (os.path.join(HERE, ".env"), os.path.join(HERE, "..", ".env")):
        if os.path.exists(env_path):
            with open(env_path, encoding="utf-8") as f:
                for raw in f:
                    line = raw.strip()
                    if line and not line.startswith("#") and "=" in line:
                        key, value = line.split("=", 1)
                        os.environ.setdefault(key.strip(), value.strip())

    base = os.environ.get("HINDSIGHT_URL", "https://api.hindsight.vectorize.io")
    key = os.environ.get("HINDSIGHT_API_KEY", "")
    bank = os.environ.get("SPIKE_BANK_ID", "recallx-spike")
    if not key:
        sys.exit("HINDSIGHT_API_KEY is not set. Copy .env.example to .env and add your key.")
    if bank == "recallx-acme":
        sys.exit("Use a throwaway bank for this test, not recallx-acme.")

    hs = Hindsight(base, key, bank)
    out = {"run_at": time.strftime("%Y-%m-%d %H:%M:%S"), "bank": bank, "base_url": base}
    try:
        setup_bank(hs, out)
        if not check_retain(hs, out):
            log("Retain failed; stopping. See spike_raw.json.")
            return
        configure_bank(hs, out)
        check_delay(hs, out, args.recall_wait)
        check_scores(hs, out)
        check_reflect_shape(hs, out)
        check_reflect_latency(hs, out)
        check_observations(hs, out, 0 if args.quick else args.observation_wait)
        check_documents(hs, out)
        if args.delete_bank:
            out["bank_delete"] = hs.call("DELETE", BANK).status
            log(f"[cleanup] DELETE bank: HTTP {out['bank_delete']}")
    finally:
        with open(os.path.join(HERE, "spike_raw.json"), "w", encoding="utf-8") as f:
            json.dump({"summary": out, "exchanges": hs.exchanges}, f, indent=2)
        if "retain_status" in out:
            docs = os.path.join(HERE, "..", "docs")
            report_path = os.path.normpath(os.path.join(docs if os.path.isdir(docs) else HERE, "SPIKE_RESULTS.md"))
            write_report(out, report_path)
            log(f"\nWrote {report_path} and {os.path.join(HERE, 'spike_raw.json')}")


if __name__ == "__main__":
    main()
