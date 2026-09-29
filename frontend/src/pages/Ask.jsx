import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { api } from '../api.js';
import Markdown from '../components/Markdown.jsx';
import { RecordChips } from '../components/RecordChip.jsx';
import SourcesLine from '../components/SourcesLine.jsx';

const DEMO_QUESTIONS = [
  'The payment API is timing out. What should I do?',
  'What went wrong in payment-service in the last three months?',
];

function Panel({ title, tone, loading, loadingText, panel }) {
  const memory = tone === 'memory';
  return (
    <section
      className={`rounded-lg border bg-white p-5 ${
        memory ? 'border-[var(--rule)] border-l-4 border-l-[var(--memory)]' : 'border-[var(--rule)]'
      }`}
      aria-live="polite"
    >
      <h2 className={`text-[17px] font-semibold ${memory ? 'text-[var(--memory)]' : ''}`}>{title}</h2>
      <p className="text-sm text-[var(--muted)]">
        {panel?.model ? <>Model: <span className="font-record">{panel.model}</span></> : memory ? 'Hindsight reflect over this team\'s memory' : 'The model on its own'}
      </p>
      <div className="mt-4 min-h-[8rem] text-[18px] leading-relaxed">
        {loading ? (
          <p className="flex items-center gap-2 text-[var(--muted)]">
            <Loader2 size={18} className="animate-spin" aria-hidden="true" />
            {loadingText}
          </p>
        ) : panel?.error ? (
          <p className={memory ? 'font-medium text-[var(--caution)]' : 'text-[var(--danger)]'}>{panel.error}</p>
        ) : panel?.text ? (
          <Markdown text={panel.text} />
        ) : (
          <p className="text-[var(--muted)]">The answer appears here.</p>
        )}
      </div>
      {memory && !loading && (panel?.citedRecords?.length > 0 || panel?.sources?.memories > 0) && (
        <div className="mt-4 space-y-2 border-t border-[var(--rule)] pt-3">
          <RecordChips records={panel.citedRecords} />
          <SourcesLine sources={panel.sources} />
        </div>
      )}
    </section>
  );
}

export default function Ask() {
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  async function submit(q) {
    const text = (q ?? question).trim();
    if (!text) return;
    setQuestion(text);
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      setResult(await api.ask(text));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="text-3xl font-semibold tracking-tight">Ask about a problem</h1>
      <p className="mt-2 max-w-[70ch] text-[17px] text-[var(--muted)]">
        The same question, with the same role and format, answered twice: by a model on its own, and by Hindsight
        reasoning over what this team has already learned. Each side names the model that answered.
      </p>

      <form
        className="mt-6 flex flex-col gap-3 sm:flex-row"
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <label htmlFor="question" className="sr-only">
          Your question
        </label>
        <input
          id="question"
          value={question}
          maxLength={500}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Describe what's going wrong"
          className="flex-1 rounded-md border border-[var(--rule)] bg-white px-4 py-3 text-[17px]"
        />
        <button
          type="submit"
          disabled={loading || !question.trim()}
          className="rounded-md bg-[var(--ink)] px-5 py-3 text-[16px] font-medium text-white disabled:opacity-50"
        >
          Ask
        </button>
      </form>
      <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2">
        {DEMO_QUESTIONS.map((q) => (
          <button
            key={q}
            type="button"
            onClick={() => submit(q)}
            disabled={loading}
            className="text-left text-[15px] text-[var(--memory)] underline decoration-[var(--rule)] underline-offset-4 hover:decoration-[var(--memory)]"
          >
            Try: “{q}”
          </button>
        ))}
      </div>

      {error && <p className="mt-4 text-[var(--danger)]">{error}</p>}

      <div className="mt-8 grid gap-5 lg:grid-cols-2">
        <Panel title="Without memory" loading={loading} loadingText="Thinking…" panel={result?.withoutMemory} />
        <Panel
          title="With RECALL-X memory"
          tone="memory"
          loading={loading}
          loadingText="Searching memory…"
          panel={result?.withMemory}
        />
      </div>
    </div>
  );
}
