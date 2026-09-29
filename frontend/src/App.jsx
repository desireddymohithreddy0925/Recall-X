import { NavLink, Route, Routes } from 'react-router-dom';
import { FilePlus2, GitCompareArrows, LayoutDashboard, MessageSquareText } from 'lucide-react';
import Ask from './pages/Ask.jsx';
import DeployCheck from './pages/DeployCheck.jsx';
import Dashboard from './pages/Dashboard.jsx';
import NewIncident from './pages/NewIncident.jsx';
import { SimulatedBadge } from './components/StatusNotes.jsx';

const NAV = [
  { to: '/', label: 'Ask', icon: MessageSquareText, end: true },
  { to: '/deploy', label: 'Deploy check', icon: GitCompareArrows },
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/incidents/new', label: 'New incident', icon: FilePlus2 },
];

export default function App() {
  return (
    <div className="flex min-h-full flex-col md:flex-row">
      <aside className="flex shrink-0 flex-col border-b border-[var(--rule)] bg-white md:w-60 md:border-b-0 md:border-r">
        <div className="px-6 pb-4 pt-6">
          <p className="text-xl font-semibold tracking-tight">RECALL-X</p>
          <p className="mt-1 text-sm text-[var(--muted)]">Engineering memory for payment-service</p>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col" aria-label="Main">
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `flex items-center gap-3 whitespace-nowrap rounded-md px-3 py-2 text-[15px] ${
                  isActive
                    ? 'bg-[var(--memory-tint)] font-medium text-[var(--memory)]'
                    : 'text-[var(--ink)] hover:bg-[var(--paper)]'
                }`
              }
            >
              <Icon size={18} aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>
        <p className="mt-auto hidden px-6 pb-6 text-sm text-[var(--muted)] md:block">
          Memory by Hindsight. The history shown is simulated, for Acme Pay, a fictional company.
        </p>
      </aside>

      <main className="flex-1 px-5 py-6 md:px-10">
        <div className="mb-4 flex justify-end">
          <SimulatedBadge />
        </div>
        <Routes>
          <Route path="/" element={<Ask />} />
          <Route path="/deploy" element={<DeployCheck />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/incidents/new" element={<NewIncident />} />
          <Route path="*" element={<Ask />} />
        </Routes>
      </main>
    </div>
  );
}
