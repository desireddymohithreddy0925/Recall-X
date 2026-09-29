import { useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { api } from '../api.js';

const OUTCOMES = ['FAILED', 'PARTIAL', 'RESOLVED'];
const emptyAttempt = () => ({ action: '', outcome: 'FAILED', note: '' });
const emptyForm = () => ({
  severity: 'SEV3', startedAt: '', resolvedAt: '', symptom: '', rootCause: '', lesson: '',
  nothingNewLearned: false, configKeys: [], causedBy: '',
});

const input = 'mt-1 w-full rounded-md border border-[var(--rule)] bg-white px-3 py-2 text-[15px]';
const label = 'text-sm font-medium';

/** "What we already learned": record a closed incident in under two minutes, including the fixes that failed. */
export default function NewIncident() {
  const [keys, setKeys] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [attempts, setAttempts] = useState([emptyAttempt(), { ...emptyAttempt(), outcome: 'RESOLVED' }]);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.configKeys().then(setKeys).catch(() => setKeys([]));
  }, []);

  const set = (field) => (e) =>
    setForm({ ...form, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const setAttempt = (i, field, value) => setAttempts(attempts.map((a, j) => (j === i ? { ...a, [field]: value } : a)));
  const toggleKey = (key) =>
    setForm({
      ...form,
      configKeys: form.configKeys.includes(key) ? form.configKeys.filter((k) => k !== key) : [...form.configKeys, key],
    });

  function reset() {
    setForm(emptyForm());
    setAttempts([emptyAttempt(), { ...emptyAttempt(), outcome: 'RESOLVED' }]);
    setSaved(null);
  }

  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      setSaved(await api.createIncident({
        ...form,
        startedAt: form.startedAt && new Date(form.startedAt).toISOString(),
        resolvedAt: form.resolvedAt && new Date(form.resolvedAt).toISOString(),
        causedBy: form.causedBy.trim() || null,
        attempts: attempts.map((a) => ({ ...a, note: a.note || null })),
      }));
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  if (saved) {
    return (
      <div className="mx-auto max-w-3xl">
        <h1 className="text-3xl font-semibold tracking-tight">Incident recorded</h1>
        <p className="mt-4 text-[17px]" role="status">
          Saved as <span className="font-record font-medium">{saved.id}</span>
          {saved.retained ? ' · sent to memory.' : ' · memory is unavailable, so it will be sent on the next sync.'}
        </p>
        <button type="button" onClick={reset} className="mt-6 text-[var(--memory)] underline">
          Record another incident
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="text-3xl font-semibold tracking-tight">Record a closed incident</h1>
      <p className="mt-2 max-w-[65ch] text-[17px] text-[var(--muted)]">
        Include the fixes that didn't work. They're what the next person needs most, and what write-ups usually drop.
      </p>

      <form onSubmit={submit} className="mt-6 space-y-5 rounded-lg border border-[var(--rule)] bg-white p-5">
        <div className="grid gap-4 sm:grid-cols-3">
          <div>
            <label htmlFor="severity" className={label}>Severity</label>
            <select id="severity" value={form.severity} onChange={set('severity')} className={input}>
              <option>SEV1</option>
              <option>SEV2</option>
              <option>SEV3</option>
            </select>
          </div>
          <div>
            <label htmlFor="startedAt" className={label}>Started</label>
            <input id="startedAt" type="datetime-local" required value={form.startedAt} onChange={set('startedAt')}
              className={input} />
          </div>
          <div>
            <label htmlFor="resolvedAt" className={label}>Resolved</label>
            <input id="resolvedAt" type="datetime-local" required value={form.resolvedAt} onChange={set('resolvedAt')}
              className={input} />
          </div>
        </div>

        <div>
          <label htmlFor="symptom" className={label}>Symptom</label>
          <textarea id="symptom" required rows={2} maxLength={2000} value={form.symptom} onChange={set('symptom')}
            className={input} placeholder="What people saw, including error messages" />
        </div>
        <div>
          <label htmlFor="rootCause" className={label}>Root cause</label>
          <textarea id="rootCause" rows={2} maxLength={2000} value={form.rootCause} onChange={set('rootCause')}
            className={input} />
        </div>

        <fieldset>
          <legend className={label}>What was tried, in order. The last attempt is the one that resolved it.</legend>
          <div className="mt-2 space-y-2">
            {attempts.map((a, i) => (
              <div key={i} className="grid gap-2 sm:grid-cols-[1fr_9rem_auto]">
                <input aria-label={`Attempt ${i + 1}`} required maxLength={500} value={a.action}
                  onChange={(e) => setAttempt(i, 'action', e.target.value)} className={input}
                  placeholder="Restarted the pods" />
                <select aria-label={`Outcome of attempt ${i + 1}`} value={a.outcome}
                  onChange={(e) => setAttempt(i, 'outcome', e.target.value)} className={input}>
                  {OUTCOMES.map((o) => <option key={o}>{o}</option>)}
                </select>
                <button type="button" disabled={attempts.length === 1} aria-label={`Remove attempt ${i + 1}`}
                  onClick={() => setAttempts(attempts.filter((_, j) => j !== i))}
                  className="mt-1 rounded-md px-2 text-[var(--muted)] hover:text-[var(--danger)] disabled:opacity-30">
                  <Trash2 size={18} aria-hidden="true" />
                </button>
              </div>
            ))}
          </div>
          <button type="button" onClick={() => setAttempts([...attempts, emptyAttempt()])}
            disabled={attempts.length >= 10} className="mt-2 inline-flex items-center gap-1 text-sm text-[var(--memory)]">
            <Plus size={16} aria-hidden="true" /> Add an attempt
          </button>
        </fieldset>

        <fieldset>
          <legend className={label}>Config keys involved</legend>
          <div className="mt-2 flex flex-wrap gap-2">
            {keys.map((k) => (
              <label key={k}
                className="font-record inline-flex items-center gap-2 rounded border border-[var(--rule)] px-2 py-1 text-[13px]">
                <input type="checkbox" checked={form.configKeys.includes(k)} onChange={() => toggleKey(k)} />
                {k}
              </label>
            ))}
          </div>
        </fieldset>

        <div className="grid gap-4 sm:grid-cols-[1fr_14rem]">
          <div>
            <label htmlFor="lesson" className={label}>Lesson</label>
            <input id="lesson" maxLength={2000} value={form.lesson} onChange={set('lesson')} className={input}
              disabled={form.nothingNewLearned} placeholder="One line the next person should know" />
            <label className="mt-2 inline-flex items-center gap-2 text-sm">
              <input type="checkbox" checked={form.nothingNewLearned} onChange={set('nothingNewLearned')} />
              Nothing new learned
            </label>
          </div>
          <div>
            <label htmlFor="causedBy" className={label}>Caused by deployment (optional)</label>
            <input id="causedBy" maxLength={32} value={form.causedBy} onChange={set('causedBy')}
              className={`font-record ${input}`} placeholder="DEP-2026-017" />
          </div>
        </div>

        <button type="submit" disabled={saving}
          className="rounded-md bg-[var(--ink)] px-5 py-2.5 font-medium text-white disabled:opacity-50">
          {saving ? 'Saving…' : 'Save and send to memory'}
        </button>
        {error && <p className="text-[var(--danger)]">{error}</p>}
      </form>
    </div>
  );
}
