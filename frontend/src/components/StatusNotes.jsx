import { AlertTriangle } from 'lucide-react';

/** Shown on every page: the history is simulated, and we say so. */
export function SimulatedBadge() {
  return (
    <span className="inline-flex items-center rounded-full border border-[var(--rule)] bg-white px-3 py-1 text-[13px] text-[var(--muted)]">
      Simulated history · Acme Pay (fictional)
    </span>
  );
}

/** Memory failures are always said out loud, never hidden behind a memory-free answer. */
export function MemoryStatusBanner({ children }) {
  return (
    <p
      role="status"
      className="flex items-start gap-2 rounded-lg border border-[var(--caution)] bg-[var(--caution-tint)] px-4 py-3 text-[15px]"
    >
      <AlertTriangle size={18} className="mt-0.5 shrink-0 text-[var(--caution)]" aria-hidden="true" />
      <span>{children}</span>
    </p>
  );
}
