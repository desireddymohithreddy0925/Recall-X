import React from 'react'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import RiskAnalysis from './pages/RiskAnalysis'
import { Activity, ShieldAlert, BookOpen, GitMerge, LayoutDashboard } from 'lucide-react'

function App() {
  return (
    <BrowserRouter>
      <div className="flex h-screen bg-slate-950 text-slate-200 font-sans">
        {/* Sidebar */}
        <aside className="w-64 bg-slate-900 border-r border-slate-800 p-4 flex flex-col">
          <div className="flex items-center gap-3 mb-10 px-2">
            <Activity className="w-8 h-8 text-blue-500" />
            <h1 className="text-xl font-bold tracking-tight text-white">RECALL-X</h1>
          </div>
          <nav className="flex-1 space-y-2">
            <Link to="/" className="flex items-center gap-3 px-3 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <LayoutDashboard className="w-5 h-5" /> Dashboard
            </Link>
            <Link to="/risk" className="flex items-center gap-3 px-3 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <ShieldAlert className="w-5 h-5" /> Risk Analysis
            </Link>
            <Link to="/incidents" className="flex items-center gap-3 px-3 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <Activity className="w-5 h-5" /> Incidents
            </Link>
            <Link to="/deployments" className="flex items-center gap-3 px-3 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <GitMerge className="w-5 h-5" /> Deployments
            </Link>
            <Link to="/lessons" className="flex items-center gap-3 px-3 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <BookOpen className="w-5 h-5" /> Lessons
            </Link>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 overflow-auto bg-slate-950">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/risk" element={<RiskAnalysis />} />
            <Route path="*" element={<div className="p-8 text-center text-slate-500">Feature coming soon in full release.</div>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
