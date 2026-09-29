import os

incident_capture_jsx = """import React, { useState } from 'react'
import { CheckCircle, AlertCircle, FileText, Send } from 'lucide-react'

export default function IncidentCapture() {
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  
  // Simulated pre-filled state from a ticketing system
  const [formData, setFormData] = useState({
    incidentId: 'INC-1025',
    title: 'Payment Gateway 504 Timeout',
    whatFailed: 'Restarted auth-service pods. Did not resolve timeout.',
    whatWorked: 'Scaled up payment-service replicas from 3 to 6.',
    lesson: ''
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    setLoading(true)
    
    // Simulate saving to MySQL and pushing to Hindsight Memory
    setTimeout(() => {
      setLoading(false)
      setSuccess(true)
    }, 1500)
  }

  if (success) {
    return (
      <div className="p-8 max-w-4xl mx-auto flex flex-col items-center justify-center h-full text-center">
        <CheckCircle className="w-16 h-16 text-green-500 mb-6" />
        <h2 className="text-3xl font-bold text-white mb-2">Memory Retained</h2>
        <p className="text-slate-400 mb-8">INC-1025 has been consolidated into organizational memory.</p>
        <button onClick={() => setSuccess(false)} className="bg-slate-800 hover:bg-slate-700 text-white px-6 py-2 rounded-full transition-colors">
          Capture Another Incident
        </button>
      </div>
    )
  }

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <header className="mb-10">
        <h2 className="text-3xl font-bold text-white mb-2">What We Learned</h2>
        <p className="text-slate-400">Capture the failed fixes and final resolutions before closing the incident ticket.</p>
      </header>

      <div className="bg-blue-950/30 border border-blue-900/50 rounded-lg p-4 mb-8 flex items-start gap-3">
        <AlertCircle className="w-5 h-5 text-blue-400 shrink-0 mt-0.5" />
        <p className="text-sm text-blue-200">
          <strong>Pre-filled from INC-1025 timeline.</strong> Please confirm the failed attempts and provide the final lesson learned to close this incident.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-6">
        <div>
          <label className="block text-sm font-medium text-slate-400 mb-2 flex items-center gap-2">
            <FileText className="w-4 h-4" /> Incident Title
          </label>
          <input 
            type="text"
            className="w-full bg-slate-950 border border-slate-700 rounded-lg p-3 text-slate-200 focus:border-blue-500 outline-none"
            value={formData.title}
            readOnly
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-orange-400 mb-2">What Failed (Attempted Fixes)</label>
            <textarea 
              className="w-full bg-slate-950 border border-slate-700 rounded-lg p-3 text-slate-200 focus:border-orange-500 outline-none h-24"
              value={formData.whatFailed}
              onChange={(e) => setFormData({...formData, whatFailed: e.target.value})}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-green-400 mb-2">What Worked (Resolution)</label>
            <textarea 
              className="w-full bg-slate-950 border border-slate-700 rounded-lg p-3 text-slate-200 focus:border-green-500 outline-none h-24"
              value={formData.whatWorked}
              onChange={(e) => setFormData({...formData, whatWorked: e.target.value})}
            />
          </div>
        </div>

        <div className="border-t border-slate-800 pt-6">
          <label className="block text-sm font-bold text-white mb-2">The Lesson Learned (Required)</label>
          <p className="text-xs text-slate-500 mb-3">If nothing new was learned, explicitly write "Nothing new learned".</p>
          <textarea 
            required
            className="w-full bg-slate-950 border border-slate-700 rounded-lg p-3 text-white focus:border-blue-500 outline-none h-24 placeholder:text-slate-600"
            placeholder="e.g. Auth service latency causes cascading timeouts in payment gateway. Must scale both together."
            value={formData.lesson}
            onChange={(e) => setFormData({...formData, lesson: e.target.value})}
          />
        </div>

        <button 
          type="submit"
          disabled={loading || !formData.lesson.trim()}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2 mt-4"
        >
          {loading ? 'Retaining in Memory...' : <><Send className="w-5 h-5" /> Retain & Close Incident</>}
        </button>
      </form>
    </div>
  )
}
"""

app_jsx = """import React from 'react'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import RiskAnalysis from './pages/RiskAnalysis'
import IncidentCapture from './pages/IncidentCapture'
import { Activity, ShieldAlert, LayoutDashboard, FileEdit } from 'lucide-react'

function App() {
  return (
    <BrowserRouter>
      <div className="flex h-screen bg-slate-950 text-slate-200 font-sans">
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
            <Link to="/capture" className="flex items-center gap-3 px-3 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
              <FileEdit className="w-5 h-5" /> Incident Capture
            </Link>
          </nav>
        </aside>

        <main className="flex-1 overflow-auto bg-slate-950">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/risk" element={<RiskAnalysis />} />
            <Route path="/capture" element={<IncidentCapture />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
"""

with open("frontend/src/pages/IncidentCapture.jsx", "w") as f: f.write(incident_capture_jsx)
with open("frontend/src/App.jsx", "w") as f: f.write(app_jsx)

print("Scaffolded V2 Phase 7 Incident Capture UI.")
