"""Generates backend/src/main/resources/db/changelog/changes/002-seed-data.sql: six months of simulated history
for Acme Pay's payment-service. Every number is consistent: 4 replicas, MySQL max_connections 150, pool 20, and so on.

Run from anywhere: python tools/gen_seed.py. Never edit the SQL by hand; change this script and regenerate.
After changing the seed, recreate the recallx database so Liquibase loads it again."""
import os
from datetime import datetime, timedelta

SERVICE = "payment-service"
POOL = "spring.datasource.hikari.maximum-pool-size"
CONN = "spring.datasource.hikari.connection-timeout"
RETRY = "payment.retry.max-attempts"
IDEM = "payment.retry.idempotency-enabled"
GW = "payment.gateway.timeout-ms"
CRON = "payment.settlement.cron"
LOG = "logging.level.com.acmepay"

CONFIG_KEYS = [
    (POOL, "20", "Connections per replica. 4 replicas x pool size must stay under MySQL max_connections (150)."),
    (CONN, "30000", "Milliseconds to wait for a pooled connection. Lower values caused errors at peak (INC-31)."),
    (RETRY, "3", "Gateway retry attempts. Above 3 requires idempotency (ADR-9)."),
    (IDEM, "true", "Idempotency keys on gateway calls. Makes retries safe."),
    (GW, "8000", "Gateway call timeout in ms. Longer values piled up threads in a provider outage (INC-24)."),
    (CRON, "0 30 1 * * *", "Month-end settlement schedule. Must run outside peak hours (ADR-11)."),
    (LOG, "INFO", "Application log level."),
]

def d(s):
    return datetime.strptime(s, "%Y-%m-%d %H:%M")

# ---- deployment dates: interpolate between anchors ----
ANCHORS = {1: d("2026-03-02 10:30"), 17: d("2026-03-11 10:30"), 24: d("2026-04-01 10:30"), 35: d("2026-05-18 10:30"),
           41: d("2026-06-16 10:30"), 55: d("2026-08-28 10:30"), 60: d("2026-09-24 10:30")}
keys = sorted(ANCHORS)
dep_date = {}
for a, b in zip(keys, keys[1:]):
    span = (ANCHORS[b] - ANCHORS[a]) / (b - a)
    for n in range(a, b + 1):
        t = ANCHORS[a] + span * (n - a)
        dep_date[n] = t.replace(second=0, microsecond=0)

SPECIAL = {  # deployment number: (version, key, old, new)
    17: ("v2.4.0", POOL, "20", "50"),
    24: ("v2.7.0", RETRY, "3", "5"),
    26: (None, IDEM, "false", "true"),
    35: ("v2.9.0", CONN, "30000", "5000"),
    41: ("v3.0.0", RETRY, "3", "4"),
    55: ("v3.2.0", CRON, "0 30 1 * * *", "0 30 23 * * *"),
    60: ("v3.3.0", None, None, None),
}

# ---- incidents ----
INCIDENTS = [
    dict(id="INC-11", sev="SEV3", start="2026-03-04 09:10", end="2026-03-04 10:05", caused=None, keys=[],
         symptom="Gateway calls failed with SSLHandshakeException: PKIX path building failed.",
         cause="The payment gateway rotated its TLS certificate chain and the client trust store was stale.",
         attempts=[("Restarted payment-service pods", "FAILED", "Handshake errors continued"),
                   ("Updated the trust store with the new certificate chain", "RESOLVED", "")],
         lesson="Subscribe to the gateway's certificate rotation notices and automate trust store updates."),
    dict(id="INC-18", sev="SEV2", start="2026-03-12 09:40", end="2026-03-12 11:05", caused=17, keys=[POOL],
         symptom="Payment API requests timed out. Logs: HikariPool-1 - Connection is not available, request timed out after 30000ms.",
         cause=f"DEP-2026-017 (v2.4.0) raised {POOL} from 20 to 50. 4 replicas x 50 = 200 connections, above MySQL max_connections of 150, so MySQL rejected connections with error 1040 Too many connections.",
         attempts=[("Restarted payment-service pods", "FAILED", "Timeouts returned within 10 minutes"),
                   ("Raised the client timeout to 60s", "PARTIAL", "Fewer errors, root cause unchanged"),
                   ("Reverted maximum-pool-size to 20", "RESOLVED", "")],
         lesson="Replicas x maximum-pool-size must stay below MySQL max_connections."),
    dict(id="INC-24", sev="SEV3", start="2026-03-24 14:20", end="2026-03-24 15:45", caused=None, keys=[GW],
         symptom="Gateway calls returned HTTP 504 and Tomcat request threads piled up during a payment provider outage.",
         cause="Payment provider outage.",
         attempts=[(f"Raised {GW} from 8000 to 20000", "PARTIAL", "Threads piled up faster and more requests queued"),
                   ("Reverted the timeout to 8000 and waited for the provider to recover", "RESOLVED", "Provider status page confirmed the outage")],
         lesson="Check the provider status page before changing timeouts. Longer timeouts make an outage worse."),
    dict(id="INC-29", sev="SEV1", start="2026-04-02 13:05", end="2026-04-02 15:30", caused=24, keys=[RETRY, IDEM],
         symptom="Customers were charged twice. Gateway logs showed repeated charge requests for the same order.",
         cause=f"DEP-2026-024 (v2.7.0) raised {RETRY} from 3 to 5 while {IDEM} was false, so retries after gateway timeouts created duplicate charges.",
         attempts=[("Lowered the gateway timeout to 5000", "FAILED", "Duplicate charges continued"),
                   ("Reverted max-attempts to 3", "RESOLVED", "Duplicates stopped. Affected orders refunded")],
         lesson="Retries must be idempotent. Never raise retries without idempotency keys."),
    dict(id="INC-31", sev="SEV2", start="2026-05-19 12:15", end="2026-05-19 13:00", caused=35, keys=[CONN],
         symptom="HikariPool-1 - Connection is not available, request timed out after 5000ms at peak traffic.",
         cause=f"DEP-2026-035 (v2.9.0) lowered {CONN} from 30000 to 5000, too short for peak-time connection waits.",
         attempts=[("Restarted payment-service pods", "FAILED", "Errors returned at the next traffic peak"),
                   ("Reverted connection-timeout to 30000", "RESOLVED", "")],
         lesson="A restart does not fix connection pool configuration. Check recent pool setting changes first."),
    dict(id="INC-33", sev="SEV2", start="2026-08-31 23:40", end="2026-09-01 01:10", caused=55, keys=[CRON],
         symptom="MySQL error 1213 Deadlock found when trying to get lock during month-end settlement. Checkout latency spiked.",
         cause=f"DEP-2026-055 (v3.2.0) moved {CRON} to 23:30, overlapping peak checkout traffic.",
         attempts=[("Restarted the settlement batch", "FAILED", "Deadlocks continued"),
                   ("Moved the settlement cron back to 01:30", "RESOLVED", "")],
         lesson="Settlement must run outside peak hours."),
]


BACKGROUND = [
    dict(id="INC-13", sev="SEV3", start="2026-03-07 14:10", end="2026-03-07 15:00", caused=None, keys=[],
         symptom="Pods restarted with No space left on device. The log volume was full.",
         cause="Debug logging left on in the refund module filled the node's log volume.",
         attempts=[("Deleted old log files by hand", "PARTIAL", "Disk filled again within an hour"),
                   ("Turned the refund module back to INFO and set log rotation to 500 MB", "RESOLVED", "")],
         lesson="Always set log rotation, and turn debug logging off after an investigation."),
    dict(id="INC-15", sev="SEV2", start="2026-03-10 11:20", end="2026-03-10 13:45", caused=None, keys=[],
         symptom="Response times spiked to 8 seconds with long GC pauses. Heap usage climbed steadily.",
         cause="A receipt cache had no size limit and grew until the heap was nearly full.",
         attempts=[("Increased the JVM heap from 2 GB to 3 GB", "FAILED", "Pauses returned after two hours"),
                   ("Added a 10,000-entry limit to the receipt cache", "RESOLVED", "")],
         lesson="A bigger heap hides a leak. Every in-memory cache needs a size limit."),
    dict(id="INC-20", sev="SEV3", start="2026-03-17 16:05", end="2026-03-17 16:50", caused=None, keys=[],
         symptom="Tomcat thread pool exhausted. Requests queued while gateway calls were slow.",
         cause="The payment gateway slowed down, and every request thread waited on it.",
         attempts=[("Raised the Tomcat max threads from 200 to 400", "PARTIAL", "More memory use, same queueing"),
                   ("Reverted the thread count and waited for the gateway to recover", "RESOLVED", "")],
         lesson="More threads don't help when a downstream call is slow."),
    dict(id="INC-22", sev="SEV3", start="2026-03-20 10:30", end="2026-03-20 11:10", caused=None, keys=[],
         symptom="NullPointerException in the refund path right after a deploy. Refunds failed.",
         cause="A new optional field on refund requests was read without a null check.",
         attempts=[("Rolled back to the previous version", "RESOLVED", "Fix shipped the next day")],
         lesson="Add a test for optional fields on every new request field."),
    dict(id="INC-26", sev="SEV3", start="2026-03-27 13:15", end="2026-03-27 14:30", caused=None, keys=[],
         symptom="Gateway webhooks were rejected with a signature mismatch.",
         cause="The gateway rotated its webhook signing key, and the old key was still configured.",
         attempts=[("Re-sent the failed webhooks from the gateway dashboard", "FAILED", "Signatures still failed"),
                   ("Installed the new signing key from the gateway dashboard", "RESOLVED", "")],
         lesson="Track the gateway's key rotation schedule, and accept both keys during a rotation."),
    dict(id="INC-27", sev="SEV3", start="2026-03-30 09:40", end="2026-03-30 10:20", caused=None, keys=[],
         symptom="UnknownHostException for the gateway host from some pods.",
         cause="A cluster DNS pod was overloaded and dropped lookups.",
         attempts=[("Scaled the cluster DNS deployment from 2 to 4 pods", "RESOLVED", "")],
         lesson="Watch DNS lookup errors, and size cluster DNS for peak traffic."),
    dict(id="INC-30", sev="SEV3", start="2026-04-21 18:05", end="2026-04-21 18:55", caused=None, keys=[],
         symptom="The gateway returned HTTP 429 Too Many Requests at evening peak.",
         cause="Traffic passed the rate limit in the gateway contract.",
         attempts=[("Retried rejected calls sooner", "FAILED", "More 429s"),
                   ("Asked the gateway for a higher limit and added client-side rate limiting", "RESOLVED", "")],
         lesson="Know the gateway contract limits, and rate-limit on our side before reaching them."),
    dict(id="INC-32", sev="SEV3", start="2026-07-14 01:40", end="2026-07-14 03:10", caused=None, keys=[],
         symptom="Settlement ran for 90 minutes instead of 15. One query did a full table scan.",
         cause="A new settlement query filtered on a column without an index.",
         attempts=[("Re-ran the settlement batch", "FAILED", "Just as slow"),
                   ("Added an index on payments.settled_on", "RESOLVED", "")],
         lesson="Check the query plan for every new settlement query."),
    dict(id="INC-34", sev="SEV3", start="2026-09-15 12:30", end="2026-09-15 15:00", caused=None, keys=[],
         symptom="Receipts showed amounts off by one unit for currencies with three decimal places.",
         cause="Receipt formatting rounded every currency to two decimal places.",
         attempts=[("Hotfixed the receipt template for one currency", "PARTIAL", "Other three-decimal currencies still wrong"),
                   ("Used each currency's own decimal places when formatting", "RESOLVED", "")],
         lesson="Never assume two decimal places. Use the currency's own precision."),
]
INCIDENTS = sorted(INCIDENTS + BACKGROUND, key=lambda i: int(i["id"].split("-")[1]))

DECISIONS = [
    ("ADR-7", POOL, "2026-03-15 11:00", "Keep maximum-pool-size at 20 per replica.",
     "After INC-18, replicas x pool size must stay below MySQL max_connections of 150, with headroom for ledger-service.", "INC-18"),
    ("ADR-12", GW, "2026-03-27 11:00", "Keep payment.gateway.timeout-ms at 8000 and check the provider status page before changing it.",
     "In INC-24 a longer timeout piled up threads during a provider outage.", "INC-24"),
    ("ADR-9", RETRY, "2026-04-06 11:00", "Retries above 3 require payment.retry.idempotency-enabled to be true.",
     "INC-29: retries without idempotency caused duplicate charges.", "INC-29"),
    ("ADR-10", CONN, "2026-05-22 11:00", "Keep connection-timeout at 30000 ms.",
     "INC-31: 5000 ms caused connection errors at peak, and restarting did not help.", "INC-31"),
    ("ADR-11", CRON, "2026-09-03 11:00", "Settlement runs at 01:30 and never during peak hours.",
     "INC-33: a 23:30 settlement overlapped peak traffic and caused deadlocks.", "INC-33"),
]
ADR_BY_KEY = {k: (i, t) for i, k, t, *_ in DECISIONS}
ADR_INCIDENT = {i: inc for i, _k, _t, _d, _r, inc in DECISIONS}
SEVERITY = {i["id"]: i["sev"] for i in INCIDENTS}
KEY_INCIDENT = {POOL: "INC-18", GW: "INC-24", RETRY: "INC-29", IDEM: "INC-29", CONN: "INC-31", CRON: "INC-33"}

# ---- warnings: (warning number, deployment number, key, old, new, verdict, reason) ----
U, FP = "USEFUL", "FALSE_POSITIVE"
WARNINGS = [
    (33, 25, GW, "8000", "10000", FP, "Only the sandbox profile changes. Production stays at 8000."),
    (34, 27, POOL, "20", "25", U, "4 x 25 = 100 leaves too little headroom for ledger-service. Change dropped."),
    (35, 28, GW, "8000", "9000", FP, "Override applies to the refunds endpoint only, not charge calls."),
    (36, 29, GW, "8000", "8500", FP, "Load-test profile only. Production unchanged."),
    (37, 32, IDEM, "true", "false", U, "Would have disabled idempotency. Reverted before shipping."),
    (38, 33, GW, "8000", "12000", FP, "Staging only, for a provider sandbox that responds slowly."),
    (39, 37, CONN, "30000", "28000", FP, "Staging only. Production unchanged."),
    (40, 38, CONN, "30000", "10000", U, "Too close to the INC-31 value. Kept at 30000."),
    (41, 39, POOL, "20", "40", U, "4 x 40 = 160 would exceed 150. Change dropped."),
    (42, 41, RETRY, "3", "4", FP, f"{IDEM} is true, so the INC-29 duplicate-charge risk did not apply."),
    (43, 44, GW, "8000", "15000", U, "Would pile up threads in a provider outage, as in INC-24. Kept at 8000."),
    (44, 46, CONN, "30000", "25000", FP, "Staging only, for a connection-leak test."),
    (45, 48, POOL, "20", "30", U, "4 x 30 = 120 leaves too little headroom for ledger-service. Change dropped."),
    (46, 50, CONN, "30000", "8000", U, "Would repeat INC-31. Kept at 30000."),
    (47, 51, GW, "8000", "20000", U, "The exact change that made INC-24 worse. Dropped."),
    (48, 52, GW, "8000", "8200", FP, "Staging only, for the fraud-check latency budget."),
    (49, 53, IDEM, "true", "false", U, "Would reintroduce the INC-29 duplicate-charge risk. Reverted."),
    (50, 56, CRON, "0 30 1 * * *", "0 0 22 * * *", U, "22:00 is peak checkout time. Kept at 01:30."),
    (51, 57, POOL, "20", "50", U, "The exact INC-18 change. Dropped."),
    (52, 58, GW, "8000", "6000", FP, "Staging only. Production stays at 8000."),
    (53, 59, CONN, "30000", "5000", U, "The exact INC-31 change. Dropped."),
    (54, 60, CRON, "0 30 1 * * *", "0 30 23 * * *", U, "The exact INC-33 change. Dropped."),
]

def summary(key, old, new, adr):
    inc = KEY_INCIDENT[key]
    if key == POOL:
        s = f"Raising {key} from {old} to {new} resembles {inc}: 4 replicas x {new} = {4 * int(new)} connections against MySQL max_connections of 150."
    elif key == GW:
        s = f"Changing {key} from {old} to {new} resembles {inc}, where a longer timeout piled up threads during a provider outage."
    elif key == CONN:
        s = f"Lowering {key} from {old} to {new} resembles {inc}, where 5000 ms caused Connection is not available errors at peak."
    elif key == RETRY:
        s = f"Changing {key} from {old} to {new} resembles {inc}, where extra retries caused duplicate charges."
    elif key == IDEM:
        s = f"Turning {key} off resembles {inc}, where retries without idempotency caused duplicate charges."
    else:
        s = f"Moving {key} to {new} resembles {inc}, where settlement overlapped peak traffic and caused deadlocks."
    if adr:
        s += f" {adr} governs this setting."
    return s

RECOMMEND = {POOL: "Keep the pool at 20 unless MySQL max_connections is raised first.",
             GW: "Check the provider status page before changing the timeout.",
             CONN: "Keep connection-timeout at 30000 ms.",
             RETRY: "Confirm idempotency is enabled before raising retries.",
             IDEM: "Keep idempotency enabled.",
             CRON: "Keep settlement outside peak hours."}

def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, int):
        return str(v)
    s = str(v)
    assert ";" not in s and "--" not in s, s
    return "'" + s.replace("'", "''") + "'"

def ts(t):
    return q(t.strftime("%Y-%m-%d %H:%M:%S"))

def dep_id(n):
    return f"DEP-2026-{n:03d}"

out = ["--liquibase formatted sql", "",
       "--changeset recallx:002-seed-data",
       "--comment: Simulated history for the demo. Generated by tools/gen_seed.py. Label it as simulated on screen.", ""]

out.append(f"INSERT INTO service_app (id, replicas) VALUES ({q(SERVICE)}, 4);")
out.append("INSERT INTO config_key (key_name, service_id, current_value, description) VALUES")
out.append(",\n".join(f"  ({q(k)}, {q(SERVICE)}, {q(v)}, {q(desc)})" for k, v, desc in CONFIG_KEYS) + ";")

# deployments
warned = {w[1]: w for w in WARNINGS}
dep_rows, change_rows = [], []
base = "v2.3"
patch = 0
for n in range(1, 61):
    special = SPECIAL.get(n)
    if special and special[0]:
        version = special[0]
        base, patch = version.rsplit(".", 1)[0], 0
    else:
        patch += 1
        version = f"{base}.{patch}"
    dep_rows.append(f"  ({q(dep_id(n))}, {q(SERVICE)}, {q(version)}, {ts(dep_date[n])})")
    change_rows.append(f"  ({q(dep_id(n))}, 'app.image.tag', {q(f'build-{1000 + n - 1}')}, {q(f'build-{1000 + n}')})")
    if special and special[1]:
        change_rows.append(f"  ({q(dep_id(n))}, {q(special[1])}, {q(special[2])}, {q(special[3])})")
    if n in warned and not (special and special[1] == warned[n][2]):
        _, _, key, old, new, _, _ = warned[n]
        change_rows.append(f"  ({q(dep_id(n))}, {q(key)}, {q(old)}, {q(new)})")
out.append("INSERT INTO deployment (id, service_id, version, deployed_at) VALUES")
out.append(",\n".join(dep_rows) + ";")
out.append("INSERT INTO deployment_change (deployment_id, key_name, old_value, new_value) VALUES")
out.append(",\n".join(change_rows) + ";")

# incidents
inc_rows, att_rows, key_rows = [], [], []
for i in INCIDENTS:
    inc_rows.append(f"  ({q(i['id'])}, {q(SERVICE)}, {q(i['sev'])}, {ts(d(i['start']))}, {ts(d(i['end']))}, "
                    f"{q(i['symptom'])}, {q(i['cause'])}, {q(i['lesson'])}, {q(dep_id(i['caused']) if i['caused'] else None)})")
    for seq, (action, outcome, note) in enumerate(i["attempts"], 1):
        att_rows.append(f"  ({q(i['id'])}, {seq}, {q(action)}, {q(outcome)}, {q(note or None)})")
    for k in i["keys"]:
        key_rows.append(f"  ({q(i['id'])}, {q(k)})")
out.append("INSERT INTO incident (id, service_id, severity, started_at, resolved_at, symptom, root_cause, lesson, caused_by) VALUES")
out.append(",\n".join(inc_rows) + ";")
out.append("INSERT INTO troubleshooting_attempt (incident_id, seq, action, outcome, note) VALUES")
out.append(",\n".join(att_rows) + ";")
out.append("INSERT INTO incident_key (incident_id, key_name) VALUES")
out.append(",\n".join(key_rows) + ";")

# decisions
out.append("INSERT INTO architecture_decision (id, service_id, key_name, decided_at, decision, reason, status, linked_incident_id) VALUES")
out.append(",\n".join(f"  ({q(i)}, {q(SERVICE)}, {q(k)}, {ts(d(t))}, {q(dec)}, {q(r)}, 'ACTIVE', {q(inc)})"
                      for i, k, t, dec, r, inc in DECISIONS) + ";")

# warnings
warn_rows = []
monthly = {}
for num, dn, key, old, new, verdict, reason in WARNINGS:
    created = dep_date[dn] + timedelta(minutes=3)
    decided = created + timedelta(hours=2)
    inc = KEY_INCIDENT[key]
    inc_date = d(next(i["start"] for i in INCIDENTS if i["id"] == inc))
    assert inc_date < created, (num, "cites an incident that has not happened yet")
    adr = None
    if key in ADR_BY_KEY and d(ADR_BY_KEY[key][1]) < created:
        adr = ADR_BY_KEY[key][0]
    cited = [adr, inc] if adr else [inc]
    kind = "DECISION_GUARD" if adr else "HISTORY"
    failed = [f"{a} ({inc})" for a, o, _n in next(i["attempts"] for i in INCIDENTS if i["id"] == inc) if o == "FAILED"]
    warn_rows.append(f"  ({q(f'WARN-{num}')}, {q(dep_id(dn))}, {q(key)}, {ts(created)}, {q(kind)}, {q(SEVERITY[inc])}, "
                     f"{q(summary(key, old, new, adr))}, {q(','.join(cited))}, {q(chr(10).join(failed) or None)}, NULL, "
                     f"{q(RECOMMEND[key])}, {q(verdict)}, {q(reason)}, {ts(decided)})")
    m = created.strftime("%Y-%m")
    u, f = monthly.get(m, (0, 0))
    monthly[m] = (u + (verdict == U), f + (verdict == FP))
out.append("INSERT INTO warning (id, deployment_id, key_name, created_at, kind, severity, summary, cited_refs, "
           "failed_attempts, prior_false_positive, recommendation, status, verdict_reason, decided_at) VALUES")
out.append(",\n".join(warn_rows) + ";")

# Everything above is the simulated history. The demo reset deletes every row that is not marked seeded.
for table in ("deployment", "incident", "architecture_decision", "warning"):
    out.append(f"UPDATE {table} SET seeded = 1;")

sql = "\n".join(out) + "\n"
# failed_attempts may contain a newline (chr(10)) inside a quoted string, which MySQL accepts.
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "backend", "src", "main", "resources",
                   "db", "changelog", "changes", "002-seed-data.sql")
with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write(sql)

print("deployments:", len(dep_rows), "changes:", len(change_rows), "incidents:", len(inc_rows),
      "decisions:", len(DECISIONS), "warnings:", len(warn_rows))
for m in sorted(monthly):
    u, f = monthly[m]
    print(m, "useful", u, "fp", f, "precision", round(100 * u / (u + f)), "%")
for n in (17, 24, 26, 35, 41, 55, 60):
    print(dep_id(n), dep_date[n].date())
print("tracked deployments:", len(set([n for n in SPECIAL if SPECIAL[n][1]] + [w[1] for w in WARNINGS])))
