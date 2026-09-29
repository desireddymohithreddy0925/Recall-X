import { useState } from 'react';
import { api } from '../api.js';

const LABEL = { USEFUL: 'useful', FALSE_POSITIVE: 'a false positive', IGNORED: 'ignored' };

export default function VerdictButtons({ warningId, onRecorded }) {
  const [mode, setMode] = useState('idle'); // idle | reason | saving | done
  const [reason, setReason] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  async function send(verdict, why) {
    setMode('saving');
    setError(null);
    try {
      const res = await api.recordVerdict(warningId, verdict, why);
      setResult(res);
      setMode('done');
      onRecorded?.(res);
    } catch (e) {
      setError(e.message);
      setMode(verdict === 'FALSE_POSITIVE' ? 'reason' : 'idle');
    }
  }

  if (mode === 'done') {
    return (
      <p className="text-[15px] text-[var(--memory)]" role="status">
        Marked as {LABEL[result.status]}.{' '}
        {result.retained ? 'RECALL-X will remember this.' : 'Saved. It will reach memory on the next sync.'}
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {mode !== 'reason' ? (
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={mode === 'saving'}
            onClick={() => send('USEFUL')}
            className="rounded-md bg-[var(--ink)] px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50"
          >
            Useful
          </button>
          <button
            type="button"
            disabled={mode === 'saving'}
            onClick={() => setMode('reason')}
            className="rounded-md border border-[var(--rule)] bg-white px-3 py-1.5 text-sm font-medium disabled:opacity-50"
          >
            False positive
          </button>
          <button
            type="button"
            disabled={mode === 'saving'}
            onClick={() => send('IGNORED')}
            className="rounded-md px-3 py-1.5 text-sm text-[var(--muted)] hover:text-[var(--ink)] disabled:opacity-50"
          >
            Ignore
          </button>
        </div>
      ) : (
        <form
          className="space-y-2"
          onSubmit={(e) => {
            e.preventDefault();
            if (reason.trim()) send('FALSE_POSITIVE', reason.trim());
          }}
        >
          <label htmlFor={`reason-${warningId}`} className="block text-sm font-medium">
            Why doesn't this warning apply?
          </label>
          <textarea
            id={`reason-${warningId}`}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={2}
            required
            className="w-full rounded-md border border-[var(--rule)] p-2 text-[15px]"
            placeholder="For example: idempotency is enabled, so duplicate charges can't happen."
          />
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={!reason.trim() || mode === 'saving'}
              className="rounded-md bg-[var(--ink)] px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50"
            >
              Save as false positive
            </button>
            <button type="button" onClick={() => setMode('idle')} className="px-3 py-1.5 text-sm text-[var(--muted)]">
              Cancel
            </button>
          </div>
        </form>
      )}
      {error && <p className="text-sm text-[var(--danger)]">{error}</p>}
    </div>
  );
}
