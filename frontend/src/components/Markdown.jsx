// A deliberately small Markdown renderer for model answers: bullet lists, paragraphs, **bold** and `code`.
// It builds React elements (never raw HTML), so nothing a model writes can inject markup.

const INLINE = /(\*\*[^*]+\*\*|`[^`]+`)/g;

function inline(text) {
  return text.split(INLINE).filter(Boolean).map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={i}>{part.slice(2, -2)}</strong>;
    if (part.startsWith('`') && part.endsWith('`')) {
      return <code key={i} className="font-record rounded bg-[var(--paper)] px-1 text-[0.9em]">{part.slice(1, -1)}</code>;
    }
    return part;
  });
}

const BULLET = /^\s*(?:[-*•]|\d+[.)])\s+/;

export default function Markdown({ text }) {
  const blocks = [];
  let list = null;
  for (const raw of text.split('\n')) {
    const line = raw.trimEnd();
    if (BULLET.test(line)) {
      if (!list) {
        list = [];
        blocks.push({ type: 'list', items: list });
      }
      list.push(line.replace(BULLET, ''));
    } else if (line.trim()) {
      list = null;
      blocks.push({ type: 'p', text: line.replace(/^#+\s*/, '') });
    } else {
      list = null;
    }
  }
  return (
    <div className="space-y-3">
      {blocks.map((b, i) =>
        b.type === 'list' ? (
          <ul key={i} className="list-disc space-y-2 pl-5">
            {b.items.map((item, j) => <li key={j}>{inline(item)}</li>)}
          </ul>
        ) : (
          <p key={i}>{inline(b.text)}</p>
        ),
      )}
    </div>
  );
}
