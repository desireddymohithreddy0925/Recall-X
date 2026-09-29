import { RecordChips } from './RecordChip.jsx';
import SourcesLine from './SourcesLine.jsx';
import VerdictButtons from './VerdictButtons.jsx';

const KIND_LABEL = { DECISION_GUARD: 'Decision Guard', HISTORY: 'History match' };

function foundBy(warning) {
  if (warning.foundBy === 'MEMORY') return 'Found by Hindsight memory. No decision record covers this key.';
  const decision = warning.citedRefs?.find((id) => id.startsWith('ADR-')) ?? 'a decision record';
  const explained = warning.sources?.memories > 0
    ? 'explained from memory'
    : 'explained from the database (memory unavailable)';
  return `Found by ${decision}, ${explained}.`;
}

export default function WarningCard({ warning, change }) {
  return (
    <article className="rounded-lg border border-[var(--rule)] border-l-4 border-l-[var(--caution)] bg-white p-5">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-[17px] font-semibold">
          {KIND_LABEL[warning.kind] ?? 'Warning'}
          <span className="font-record ml-2 text-[14px] font-normal text-[var(--muted)]">
            {change.key}: {change.oldValue} to {change.newValue}
          </span>
        </h3>
        {warning.severity && (
          <span
            className={`rounded px-2 py-0.5 text-sm font-medium ${
              warning.severity === 'SEV1'
                ? 'bg-[#fdecea] text-[var(--danger)]'
                : 'bg-[var(--caution-tint)] text-[var(--caution)]'
            }`}
          >
            Severity {warning.severity}
          </span>
        )}
      </header>
      <p className="mt-1 text-sm text-[var(--muted)]">{foundBy(warning)}</p>

      <p className="mt-3 max-w-[72ch] text-[16px]">{warning.summary}</p>

      {warning.failedAttempts?.length > 0 && (
        <div className="mt-4">
          <p className="text-sm font-medium">Tried before and failed</p>
          <ul className="mt-1 list-disc pl-5 text-[15px]">
            {warning.failedAttempts.map((a) => (
              <li key={a}>{a}</li>
            ))}
          </ul>
        </div>
      )}

      {warning.priorFalsePositive && (
        <p className="mt-4 rounded-md bg-[var(--paper)] p-3 text-[15px]">
          <span className="font-medium">An earlier warning was a false positive: </span>
          {warning.priorFalsePositive}
        </p>
      )}

      {warning.recommendation && (
        <p className="mt-4 text-[15px]">
          <span className="font-medium">Before shipping: </span>
          {warning.recommendation}
        </p>
      )}

      <div className="mt-4 space-y-2">
        <RecordChips records={warning.citedRecords} />
        <SourcesLine sources={warning.sources} />
      </div>

      {warning.id && warning.status === 'PENDING' && (
        <div className="mt-5 border-t border-[var(--rule)] pt-4">
          <p className="mb-2 text-sm text-[var(--muted)]">
            <span className="font-record">{warning.id}</span>. Was this warning right?
          </p>
          <VerdictButtons warningId={warning.id} />
        </div>
      )}
    </article>
  );
}
