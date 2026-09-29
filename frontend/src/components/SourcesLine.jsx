/** What Hindsight's reflect used for this answer: memories, mental models and the bank's directives. */
export default function SourcesLine({ sources }) {
  if (!sources || (!sources.memories && !sources.mentalModels?.length && !sources.directives?.length)) return null;
  // Reflect can answer from a mental model alone; "0 memories" would read as "memory wasn't used".
  const parts = sources.memories ? [`${sources.memories} ${sources.memories === 1 ? 'memory' : 'memories'}`] : [];
  if (sources.mentalModels?.length) parts.push(`mental model: ${sources.mentalModels.join(', ')}`);
  if (sources.directives?.length) parts.push(`rules applied: ${sources.directives.join(', ')}`);
  return (
    <p className="text-[13px] text-[var(--muted)]">
      <span className="font-medium">Hindsight used</span> {parts.join(' · ')}
    </p>
  );
}
