// Dates come from the API as yyyy-MM-dd (UTC).

const MONTH = 30.44;

/** "12 Mar" */
export function shortDate(day) {
  if (!day) return '';
  return new Date(`${day}T00:00:00Z`).toLocaleString('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' });
}

/** "7 months ago", "3 weeks ago", "yesterday", "today" */
export function age(day, now = new Date()) {
  if (!day) return '';
  const days = Math.floor((now - new Date(`${day}T00:00:00Z`)) / 86_400_000);
  if (days <= 0) return 'today';
  if (days === 1) return 'yesterday';
  if (days < 14) return `${days} days ago`;
  if (days < 60) return `${Math.round(days / 7)} weeks ago`;
  const months = Math.round(days / MONTH);
  return months < 24 ? `${months} months ago` : `${Math.round(months / 12)} years ago`;
}

/** "Mar 2026" for chart axes */
export function monthName(ym) {
  return new Date(`${ym}-01T00:00:00Z`).toLocaleString('en', { month: 'short', year: 'numeric', timeZone: 'UTC' });
}
