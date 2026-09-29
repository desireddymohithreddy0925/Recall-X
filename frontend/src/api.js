// All calls go to /api, which Vite proxies to the Spring Boot backend (VITE_BACKEND_URL, default :8080).
// Errors come back as { error, detail }; they're thrown with error.code set, e.g. 'memory_unavailable'.
async function request(path, options = {}) {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const text = await res.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = { detail: text };
    }
  }
  if (!res.ok) {
    const error = new Error(body?.detail || body?.error || `Request failed with status ${res.status}`);
    error.status = res.status;
    error.code = body?.error;
    throw error;
  }
  return body;
}

const post = (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) });

export const api = {
  ask: (question) => post('/ask', { question }),
  checkChange: ({ version, diff }) => post('/deployments/check', { version, diff }),
  recordVerdict: (id, verdict, reason) => post(`/warnings/${encodeURIComponent(id)}/verdict`, { verdict, reason }),
  stats: () => request('/stats'),
  patterns: () => request('/patterns'),
  record: (id) => request(`/records/${encodeURIComponent(id)}`),
  configKeys: () => request('/config-keys'),
  createIncident: (incident) => post('/incidents', incident),
};
