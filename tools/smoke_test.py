#!/usr/bin/env python3
"""
Runs the acceptance checklist (implementation plan, section 14) against a running RECALL-X backend.

    python tools/smoke_test.py                       # against http://localhost:8080
    python tools/smoke_test.py --url http://localhost:8089

It resets the demo first and again at the end, so run it between rehearsals. It works out whether Hindsight is
reachable and checks the matching expectations: with memory up, the full demo; with memory down, the fallbacks
(Decision Guard from the database, "memory unavailable" instead of "no history"). Standard library only.

Reads RECALLX_ADMIN_TOKEN (and RECALLX_URL) from the repository's .env or the environment.
Exit code 1 if any check fails. WARN lines are soft checks that depend on the model's wording.
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))

POOL = "spring.datasource.hikari.maximum-pool-size"
LOG = "logging.level.com.acmepay"
RETRY = "payment.retry.max-attempts"
QUIET = "management.endpoint.health.show-details"   # keep in sync with DEMO_DIFF in frontend/src/pages/DeployCheck.jsx
DEMO_DIFF = "\n".join([f"{POOL}: 20 -> 60", f"{LOG}: INFO -> DEBUG", f"{RETRY}: 3 -> 4", f"{QUIET}: never -> always"])
DEMO_QUESTION = "The payment API is timing out. What should I do?"

results = {"PASS": 0, "FAIL": 0, "WARN": 0}


def load_env():
    for path in (os.path.join(HERE, ".env"), os.path.join(HERE, "..", ".env")):
        if os.path.exists(path):
            with open(path, encoding="utf-8") as f:
                for raw in f:
                    line = raw.strip()
                    if line and not line.startswith("#") and "=" in line:
                        key, value = line.split("=", 1)
                        os.environ.setdefault(key.strip(), value.strip())


class Api:
    def __init__(self, base, admin_token):
        self.base, self.admin_token = base.rstrip("/"), admin_token

    def call(self, method, path, body=None, admin=False, timeout=180):
        data = None if body is None else json.dumps(body).encode("utf-8")
        req = urllib.request.Request(self.base + path, data=data, method=method)
        req.add_header("Content-Type", "application/json")
        if admin:
            req.add_header("X-Admin-Token", self.admin_token)
        try:
            with urllib.request.urlopen(req, timeout=timeout) as res:
                status, text = res.status, res.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            status, text = e.code, e.read().decode("utf-8", "replace")
        except (urllib.error.URLError, TimeoutError, OSError) as e:
            return 0, {"error": "unreachable", "detail": str(e)}
        try:
            return status, json.loads(text) if text else None
        except json.JSONDecodeError:
            return status, {"detail": text}


def report(level, name, detail=""):
    results[level] += 1
    print(f"{level:<4}  {name}" + (f"  ({detail})" if detail else ""), flush=True)


def check(condition, name, detail="", soft=False):
    report("PASS" if condition else ("WARN" if soft else "FAIL"), name, "" if condition else detail)
    return condition


def by_key(response):
    return {r["key"]: r for r in (response or {}).get("results", [])}


def main():
    load_env()
    parser = argparse.ArgumentParser(description="RECALL-X acceptance checks")
    parser.add_argument("--url", default=os.environ.get("RECALLX_URL", "http://localhost:8080"))
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="replace")
    api = Api(args.url, os.environ.get("RECALLX_ADMIN_TOKEN", ""))

    status, body = api.call("GET", "/api/health")
    if status != 200:
        sys.exit(f"The backend at {args.url} is not reachable ({status}): {body}")

    # ---- setup
    status, body = api.call("POST", "/api/admin/demo/reset", admin=True)
    if not check(status == 200, "reset the demo (admin token accepted)", f"HTTP {status} {body}"):
        sys.exit("Set RECALLX_ADMIN_TOKEN in .env to the value the backend uses.")
    status, _ = api.call("GET", "/api/admin/memory/check?q=payment-service%20maximum-pool-size", admin=True)
    memory_up = status == 200
    print(f"\nMemory is {'UP: checking the full demo' if memory_up else 'DOWN: checking the fallbacks'}\n")

    # ---- memory
    if memory_up:
        _, body = api.call("GET", "/api/admin/memory/check?q=payment-service%20maximum-pool-size", admin=True)
        ids = set(body.get("recordIds", []))
        check({"INC-18", "ADR-7"} <= ids, "memory: recall finds INC-18 and ADR-7", f"got {sorted(ids)}")
        _, body = api.call("GET", "/api/admin/memory/check?q=debug%20logging%20filled%20the%20log%20volume", admin=True)
        check("INC-13" in body.get("recordIds", []), "memory: recall finds INC-13", f"got {body.get('recordIds')}")

    # ---- dashboard
    status, stats = api.call("GET", "/api/stats")
    counts = stats.get("counts", {}) if status == 200 else {}
    check(counts == {"incidents": 15, "decisions": 5, "deployments": 60, "warnings": 22},
          "dashboard: counts are 15 / 5 / 60 / 22", f"got {counts}")
    precision = [m["precisionPct"] for m in stats.get("precisionByMonth", [])] if status == 200 else []
    check(precision[:6] == [25, 33, 67, 67, 75, 80], "dashboard: precision April-September is 25 33 67 67 75 80",
          f"got {precision}")
    status, patterns = api.call("GET", "/api/patterns")
    if memory_up:
        check(status == 200 and patterns.get("patterns"), "dashboard: Hindsight observations shown as patterns",
              "no observations yet (consolidation may still be running)", soft=True)
    else:
        check(status == 200 and patterns.get("memoryAvailable") is False, "dashboard: patterns say memory is unavailable",
              f"got {status} {patterns}")

    # ---- the demo change
    status, check_response = api.call("POST", "/api/deployments/check", {"version": "v3.4", "diff": DEMO_DIFF})
    check(status == 200 and check_response.get("deploymentId") == "DEP-2026-061",
          "check: the demo change is saved as DEP-2026-061", f"HTTP {status} {check_response}")
    results_by_key = by_key(check_response)

    pool = results_by_key.get(POOL, {})
    warning = pool.get("warning") or {}
    check(pool.get("status") == "WARNING" and warning.get("kind") == "DECISION_GUARD" and warning.get("id") == "WARN-55",
          "check: pool size is a Decision Guard warning, WARN-55", f"got {pool.get('status')} {warning.get('kind')} {warning.get('id')}")
    check({"ADR-7", "INC-18"} <= set(warning.get("citedRefs", [])) and warning.get("severity") == "SEV2",
          "check: pool warning cites ADR-7 and INC-18 at SEV2", f"got {warning.get('citedRefs')} {warning.get('severity')}")
    check(any("Restart" in a for a in warning.get("failedAttempts", [])),
          "check: pool warning names the failed restart", f"got {warning.get('failedAttempts')}", soft=memory_up)

    retry = results_by_key.get(RETRY, {})
    cleared = retry.get("cleared") or {}
    check(retry.get("status") == "PREVIOUSLY_CLEARED" and cleared.get("warningId") == "WARN-42"
          and cleared.get("decisionId") == "ADR-9",
          "check: retry change is previously judged safe (WARN-42, ADR-9 still applies)",
          f"got {retry.get('status')} {cleared.get('warningId')} {cleared.get('decisionId')}")

    log_result = results_by_key.get(LOG, {})
    quiet = results_by_key.get(QUIET, {})
    if memory_up:
        log_warning = log_result.get("warning") or {}
        check(log_result.get("status") == "WARNING" and log_warning.get("foundBy") == "MEMORY"
              and "INC-13" in log_warning.get("citedRefs", []),
              "check: debug logging is a memory-only history match citing INC-13",
              f"got {log_result.get('status')} {log_warning.get('foundBy')} {log_warning.get('citedRefs')}")
        check(quiet.get("status") == "NO_HISTORY", f"check: {QUIET} stays quiet", f"got {quiet.get('status')}")
    else:
        check(log_result.get("status") == "MEMORY_UNAVAILABLE" and quiet.get("status") == "MEMORY_UNAVAILABLE",
              "check: keys without a decision say memory unavailable, never 'no history'",
              f"got {log_result.get('status')} / {quiet.get('status')}")

    status, body = api.call("POST", "/api/deployments/check",
                            {"diff": "payment.settlement.cron: 0 30 1 * * * -> 0 30 23 * * *", "dryRun": True})
    cron = (body or {}).get("results", [{}])[0]
    cron_warning = cron.get("warning") or {}
    check(cron.get("status") == "WARNING" and {"ADR-11", "INC-33"} <= set(cron_warning.get("citedRefs", []))
          and cron_warning.get("severity") == "SEV2", "check: settlement cron cites ADR-11 and INC-33 at SEV2",
          f"got {cron.get('status')} {cron_warning.get('citedRefs')} {cron_warning.get('severity')}")
    check(body.get("deploymentId") is None, "check: a dry run saves nothing", f"got {body.get('deploymentId')}")

    status, body = api.call("POST", "/api/deployments/check", {"diff": "feature.flag.x: off -> on", "dryRun": True})
    expected = "NO_HISTORY" if memory_up else "MEMORY_UNAVAILABLE"
    got = (body or {}).get("results", [{}])[0].get("status")
    check(got == expected, f"check: an unrelated key is {expected}", f"got {got}")

    status, body = api.call("POST", "/api/deployments/check", {"diff": "a.b: 1 -> 2\nnot a change"})
    check(status == 400 and "Line 2" in (body or {}).get("detail", ""), "check: a bad line is a 400 naming the line",
          f"HTTP {status} {body}")
    many = "\n".join(f"key{i}: 1 -> 2" for i in range(21))
    status, _ = api.call("POST", "/api/deployments/check", {"diff": many})
    check(status == 400, "check: 21 changes is a 400", f"HTTP {status}")

    # ---- verdicts
    status, _ = api.call("POST", "/api/warnings/WARN-55/verdict", {"verdict": "FALSE_POSITIVE"})
    check(status == 400, "verdict: a false positive without a reason is a 400", f"HTTP {status}")
    status, body = api.call("POST", "/api/warnings/WARN-55/verdict", {"verdict": "USEFUL"})
    check(status == 200, "verdict: marking WARN-55 useful is saved", f"HTTP {status} {body}")
    check(bool((body or {}).get("retained")) == memory_up, "verdict: retained in memory exactly when memory is up",
          f"retained={body and body.get('retained')}")
    status, _ = api.call("POST", "/api/warnings/WARN-55/verdict", {"verdict": "IGNORED"})
    check(status == 409, "verdict: a second verdict is a 409", f"HTTP {status}")

    # ---- ask
    status, ask = api.call("POST", "/api/ask", {"question": DEMO_QUESTION}, timeout=240)
    check(status == 200, "ask: the demo question returns both panels", f"HTTP {status} {ask}")
    if status == 200:
        without, with_memory = ask["withoutMemory"], ask["withMemory"]
        check(not without.get("citedRecords"), "ask: the memory-off panel cites nothing")
        check(bool(without.get("model")) or bool(without.get("error")), "ask: the memory-off panel names its model",
              f"got {without}")
        check(without.get("error") is None, "ask: the memory-off model answered", str(without.get("error")), soft=True)
        if memory_up:
            cited = [r["id"] for r in with_memory.get("citedRecords", [])]
            check("INC-18" in cited, "ask: the memory panel cites INC-18", f"got {cited}")
            check("restart" in (with_memory.get("text") or "").lower(), "ask: the memory panel mentions the failed restart",
                  "the wording didn't mention it", soft=True)
            check((with_memory.get("sources") or {}).get("memories", 0) > 0, "ask: the Sources line counts memories used",
                  f"got {with_memory.get('sources')}")
        else:
            check(with_memory.get("error") == "Memory unavailable" and with_memory.get("text") is None,
                  "ask: the memory panel says 'Memory unavailable' and shows no other answer", f"got {with_memory}")

    # ---- records and admin
    status, _ = api.call("GET", "/api/records/INC-18")
    check(status == 200, "records: INC-18 exists", f"HTTP {status}")
    status, _ = api.call("GET", "/api/records/INC-99")
    check(status == 404, "records: an invented ID is a 404", f"HTTP {status}")
    status, _ = api.call("POST", "/api/admin/demo/reset")
    check(status == 403, "security: an admin call without the token is refused", f"HTTP {status}")

    # ---- leave it clean for the next run
    status, body = api.call("POST", "/api/admin/demo/reset", admin=True)
    check(status == 200, "reset the demo again", f"HTTP {status} {body}")

    print(f"\n{results['PASS']} passed, {results['FAIL']} failed, {results['WARN']} warnings")
    sys.exit(1 if results["FAIL"] else 0)


if __name__ == "__main__":
    main()
