import { CheckCircle2 } from 'lucide-react';
import { shortDate } from '../format.js';
import { RecordChips } from './RecordChip.jsx';
import SourcesLine from './SourcesLine.jsx';

/**
 * The learning loop, visible: this exact change was warned about before, and an engineer marked that warning a
 * false positive. RECALL-X says so instead of raising the same alarm again. There's nothing to rate, so no buttons.
 */
export default function ClearedCard({ cleared, change }) {
  return (
    <article className="rounded-lg border border-[var(--rule)] border-l-4 border-l-[var(--memory)] bg-white p-5">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="flex items-center gap-2 text-[17px] font-semibold text-[var(--memory)]">
          <CheckCircle2 size={18} aria-hidden="true" />
          Previously judged safe
          <span className="font-record ml-1 text-[14px] font-normal text-[var(--muted)]">
            {change.key}: {change.oldValue} to {change.newValue}
          </span>
        </h3>
      </header>
      <p className="mt-1 text-sm text-[var(--muted)]">
        Remembered verdict: <span className="font-record">{cleared.warningId}</span> ({shortDate(cleared.date)}) was marked a
        false positive
      </p>

      <p className="mt-3 max-w-[72ch] text-[16px]">{cleared.summary}</p>

      {cleared.reason && (
        <p className="mt-4 rounded-md bg-[var(--memory-tint)] p-3 text-[15px]">
          <span className="font-medium">The engineer's reason: </span>
          {cleared.reason}
        </p>
      )}

      {cleared.decisionId && (
        <p className="mt-3 text-[15px]">
          <span className="font-medium">Still applies: </span>
          <span className="font-record">{cleared.decisionId}</span>, {cleared.decisionText}
        </p>
      )}

      {cleared.recommendation && (
        <p className="mt-3 text-[15px]">
          <span className="font-medium">Before shipping: </span>
          {cleared.recommendation}
        </p>
      )}

      <div className="mt-4 space-y-2">
        <RecordChips records={cleared.citedRecords} />
        <SourcesLine sources={cleared.sources} />
      </div>
    </article>
  );
}
