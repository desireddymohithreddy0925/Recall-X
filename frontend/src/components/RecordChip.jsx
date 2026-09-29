import { age, shortDate } from '../format.js';

const TYPE_LABEL = { INCIDENT: 'Incident', DECISION: 'Decision', DEPLOYMENT: 'Deployment', WARNING: 'Earlier warning' };

/** A cited record: its ID, date and age. Every chip comes from the database, so it always points at a real record. */
export default function RecordChip({ record }) {
  const label = TYPE_LABEL[record.type] ?? record.type;
  const tooltip = [label, record.severity, record.title].filter(Boolean).join(' · ');
  return (
    <span
      className="inline-flex items-baseline gap-1.5 rounded border border-[var(--rule)] bg-white px-2 py-0.5 text-[13px]"
      title={tooltip}
    >
      <span className="font-record font-medium">{record.id}</span>
      {record.date && (
        <span className="text-[var(--muted)]">
          {shortDate(record.date)} · {age(record.date)}
        </span>
      )}
    </span>
  );
}

export function RecordChips({ records, label = 'Based on' }) {
  if (!records?.length) return null;
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-sm text-[var(--muted)]">{label}</span>
      {records.map((r) => (
        <RecordChip key={r.id} record={r} />
      ))}
    </div>
  );
}
