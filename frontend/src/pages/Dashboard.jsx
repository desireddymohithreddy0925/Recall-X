import { useEffect, useState } from 'react';
import { api } from '../api.js';
import PrecisionChart from '../components/PrecisionChart.jsx';
import { RecordChips } from '../components/RecordChip.jsx';
import { MemoryStatusBanner } from '../components/StatusNotes.jsx';

const STATUS_LABEL = { PENDING: 'Waiting for a verdict', USEFUL: 'Useful', FALSE_POSITIVE: 'False positive', IGNORED: 'Ignored' };
const KIND_LABEL = { DECISION_GUARD: 'Decision Guard', HISTORY: 'History match' };

function Patterns({ patterns }) {
  if (!patterns) return <p className="mt-3 text-[var(--muted)]">Loading…</p>;
  if (!patterns.memoryAvailable) {
    return (
      <div className="mt-3">
        <MemoryStatusBanner>Memory is unavailable, so patterns can't be shown right now.</MemoryStatusBanner>
      </div>
    );
  }
  if (patterns.patterns.length === 0) {
    return (
      <p className="mt-3 text-[var(--muted)]">
        No patterns yet. They appear once Hindsight has consolidated related memories on its own.
      </p>
    );
  }
  return (
    <ul className="mt-3 space-y-3">
      {patterns.patterns.map((p) => (
        <li key={p.text} className="space-y-2 rounded-lg border border-[var(--rule)] bg-white p-4 text-[15px]">
          <p>{p.text}</p>
          <RecordChips records={p.records} label="From" />
        </li>
      ))}
    </ul>
  );
}

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [patterns, setPatterns] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.stats().then(setStats).catch((e) => setError(e.message));
    api.patterns().then(setPatterns).catch(() => setPatterns({ patterns: [], memoryAvailable: false }));
  }, []);

  if (error) return <p className="text-[var(--danger)]">Couldn't load the dashboard: {error}</p>;
  if (!stats) return <p className="text-[var(--muted)]">Loading…</p>;

  const counts = [
    ['Incidents', stats.counts.incidents],
    ['Decisions', stats.counts.decisions],
    ['Deployments', stats.counts.deployments],
    ['Warnings', stats.counts.warnings],
  ];

  return (
    <div className="mx-auto max-w-5xl">
      <h1 className="text-3xl font-semibold tracking-tight">What RECALL-X knows</h1>
      <p className="mt-2 max-w-[65ch] text-[17px] text-[var(--muted)]">
        Every number here comes from the database, never from a model.
      </p>

      <dl className="mt-6 grid grid-cols-2 gap-y-4 border-y border-[var(--rule)] py-5 sm:grid-cols-4">
        {counts.map(([label, value]) => (
          <div key={label}>
            <dt className="text-sm text-[var(--muted)]">{label}</dt>
            <dd className="text-3xl font-semibold tabular-nums">{value}</dd>
          </div>
        ))}
      </dl>

      <div className="mt-8 grid gap-8 lg:grid-cols-[3fr_2fr]">
        <section className="rounded-lg border border-[var(--rule)] bg-white p-5">
          <h2 className="text-lg font-semibold">Warning precision by month</h2>
          <p className="text-sm text-[var(--muted)]">
            Useful ÷ (useful + false positive). Ignored warnings are left out. Simulated history to 25 Sep 2026; live
            verdicts after that.
          </p>
          <div className="mt-4">
            <PrecisionChart data={stats.precisionByMonth} />
          </div>
        </section>

        <section>
          <h2 className="text-lg font-semibold">Patterns Hindsight noticed</h2>
          <p className="text-sm text-[var(--muted)]">Observations Hindsight consolidated from several memories.</p>
          <Patterns patterns={patterns} />
        </section>
      </div>

      <section className="mt-8">
        <h2 className="text-lg font-semibold">Recent warnings</h2>
        <div className="mt-3 overflow-x-auto rounded-lg border border-[var(--rule)] bg-white">
          <table className="w-full text-left text-[15px]">
            <thead className="border-b border-[var(--rule)] text-sm text-[var(--muted)]">
              <tr>
                <th className="px-4 py-2 font-medium">Warning</th>
                <th className="px-4 py-2 font-medium">Deployment</th>
                <th className="px-4 py-2 font-medium">Kind</th>
                <th className="px-4 py-2 font-medium">Severity</th>
                <th className="px-4 py-2 font-medium">Verdict</th>
              </tr>
            </thead>
            <tbody>
              {stats.recentWarnings.map((w) => (
                <tr key={w.id} className="border-b border-[var(--rule)] last:border-0">
                  <td className="px-4 py-2">
                    <span className="font-record">{w.id}</span>
                    <span className="block text-sm text-[var(--muted)]">{w.key}</span>
                  </td>
                  <td className="font-record px-4 py-2 text-sm">{w.deploymentId}</td>
                  <td className="px-4 py-2">{KIND_LABEL[w.kind] ?? w.kind}</td>
                  <td className="px-4 py-2">{w.severity ?? 'None'}</td>
                  <td className="px-4 py-2">{STATUS_LABEL[w.status] ?? w.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
