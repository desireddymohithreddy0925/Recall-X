import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { api } from '../api.js';
import ClearedCard from '../components/ClearedCard.jsx';
import WarningCard from '../components/WarningCard.jsx';
import { MemoryStatusBanner } from '../components/StatusNotes.jsx';

// Four changes, four outcomes: a protected decision, a match only memory could find (INC-13), a remembered false
// alarm (WARN-42) and silence. The last key had the lowest relevance score in tools/hindsight_spike.py, check 3.
export const DEMO_DIFF = [
  'spring.datasource.hikari.maximum-pool-size: 20 -> 60',
  'logging.level.com.acmepay: INFO -> DEBUG',
  'payment.retry.max-attempts: 3 -> 4',
  'management.endpoint.health.show-details: never -> always',
].join('\n');

function NoHistoryRow({ result }) {
  return (
    <div className="rounded-lg border border-dashed border-[var(--rule)] px-5 py-3 text-[15px] text-[var(--muted)]">
      <span className="font-record text-[var(--ink)]">{result.key}</span>: no relevant history, so no warning.
    </div>
  );
}

function MemoryUnavailableRow({ result }) {
  return (
    <MemoryStatusBanner>
      <span className="font-record">{result.key}</span>: history couldn't be checked because memory is unavailable. This
      is not the same as "no history". Check this change again before shipping.
    </MemoryStatusBanner>
  );
}

export default function DeployCheck() {
  const [version, setVersion] = useState('v3.4');
  const [diff, setDiff] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  async function check(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      setResult(await api.checkChange({ version, diff }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-4xl">
      <h1 className="text-3xl font-semibold tracking-tight">Check a change before it ships</h1>
      <p className="mt-2 max-w-[65ch] text-[17px] text-[var(--muted)]">
        Paste the settings you're changing. RECALL-X warns only when the team's history is relevant, cites the records
        it's based on, and remembers when a warning turned out to be wrong.
      </p>

      <form onSubmit={check} className="mt-6 space-y-4 rounded-lg border border-[var(--rule)] bg-white p-5">
        <div className="grid gap-4 sm:grid-cols-[1fr_12rem]">
          <div>
            <p className="text-sm font-medium">Service</p>
            <p className="font-record mt-1 py-2 text-[15px]">payment-service</p>
          </div>
          <div>
            <label htmlFor="version" className="text-sm font-medium">
              Version
            </label>
            <input
              id="version"
              value={version}
              maxLength={32}
              onChange={(e) => setVersion(e.target.value)}
              className="font-record mt-1 w-full rounded-md border border-[var(--rule)] px-3 py-2 text-[15px]"
            />
          </div>
        </div>
        <div>
          <div className="flex items-baseline justify-between">
            <label htmlFor="diff" className="text-sm font-medium">
              Changes, one per line as key: old -&gt; new
            </label>
            <button
              type="button"
              onClick={() => setDiff(DEMO_DIFF)}
              className="text-sm text-[var(--memory)] underline decoration-[var(--rule)] underline-offset-4"
            >
              Load demo change
            </button>
          </div>
          <textarea
            id="diff"
            value={diff}
            onChange={(e) => setDiff(e.target.value)}
            rows={5}
            spellCheck={false}
            placeholder="spring.datasource.hikari.maximum-pool-size: 20 -> 60"
            className="font-record mt-1 w-full rounded-md border border-[var(--rule)] p-3 text-[15px]"
          />
        </div>
        <button
          type="submit"
          disabled={loading || !diff.trim()}
          className="inline-flex items-center gap-2 rounded-md bg-[var(--ink)] px-5 py-2.5 font-medium text-white disabled:opacity-50"
        >
          {loading && <Loader2 size={18} className="animate-spin" aria-hidden="true" />}
          {loading ? 'Checking history…' : 'Check change'}
        </button>
        {error && <p className="text-[var(--danger)]">{error}</p>}
      </form>

      {result && (
        <section className="mt-8 space-y-4" aria-live="polite">
          <h2 className="text-lg font-semibold">
            Results
            {result.deploymentId && (
              <span className="font-record ml-2 text-[15px] font-normal text-[var(--muted)]">{result.deploymentId}</span>
            )}
          </h2>
          {result.results.map((r) => {
            switch (r.status) {
              case 'WARNING':
                return <WarningCard key={r.key} warning={r.warning} change={r} />;
              case 'PREVIOUSLY_CLEARED':
                return <ClearedCard key={r.key} cleared={r.cleared} change={r} />;
              case 'MEMORY_UNAVAILABLE':
                return <MemoryUnavailableRow key={r.key} result={r} />;
              default:
                return <NoHistoryRow key={r.key} result={r} />;
            }
          })}
        </section>
      )}
    </div>
  );
}
