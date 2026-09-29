import os

app_jsx = """import React from 'react'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import RiskAnalysis from './pages/RiskAnalysis'
import { Activity, ShieldAlert, LayoutDashboard } from 'lucide-react'

function App() {
  return (
    <BrowserRouter>
      <div className="flex h-screen bg-slate-950 text-slate-200 font-sans">
        {/* Simplified Sidebar */}
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
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 overflow-auto bg-slate-950">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/risk" element={<RiskAnalysis />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
"""

dashboard_jsx = """import React, { useState, useEffect } from 'react'
import { Activity, ShieldAlert, GitCommit, Target, AlertTriangle } from 'lucide-react'

export default function Dashboard() {
  const [metrics, setMetrics] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // MySQL Driven Metrics (simulated fetch)
    setTimeout(() => {
      setMetrics({
        experiences: 124,
        incidents: 45,
        deployments: 78,
        patterns: 4, // Represents observations with proof count > 1
        failedFixes: 23,
        warningPrecision: 90 // 45 useful / 50 total
      })
      setLoading(false)
    }, 1000)
  }, [])

  if (loading) return <div className="p-8 flex items-center justify-center h-full text-slate-500">Loading Organizational Intelligence...</div>

  return (
    <div className="p-8">
      <header className="mb-10">
        <h2 className="text-3xl font-bold text-white mb-2">Organizational Memory</h2>
        <p className="text-slate-400">Engineering Intelligence Dashboard</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-10">
        <MetricCard title="Experiences Remembered" value={metrics.experiences} icon={Activity} color="text-blue-500" />
        <MetricCard title="Total Incidents" value={metrics.incidents} icon={AlertTriangle} color="text-red-500" />
        <MetricCard title="Deployments" value={metrics.deployments} icon={GitCommit} color="text-green-500" />
        <MetricCard title="Recurring Patterns" value={metrics.patterns} icon={Target} color="text-purple-500" />
        
        {/* V2 Metric: Warning Precision */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex items-start gap-4 col-span-1 md:col-span-2 lg:col-span-1">
          <div className={`p-3 rounded-lg bg-slate-950 text-indigo-500`}>
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-slate-400 mb-1">Warning Precision</p>
            <p className="text-3xl font-bold text-white">{metrics.warningPrecision}%</p>
            <p className="text-xs text-slate-500 mt-1">Useful ÷ (Useful + False Positive)</p>
          </div>
        </div>
      </div>
    </div>
  )
}

function MetricCard({ title, value, icon: Icon, color }) {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex items-start gap-4">
      <div className={`p-3 rounded-lg bg-slate-950 ${color}`}>
        <Icon className="w-6 h-6" />
      </div>
      <div>
        <p className="text-sm font-medium text-slate-400 mb-1">{title}</p>
        <p className="text-3xl font-bold text-white">{value}</p>
      </div>
    </div>
  )
}
"""

risk_analysis_jsx = """import React, { useState } from 'react'
import { ShieldAlert, CheckCircle, Activity, ArrowRight, Zap, ToggleLeft, ToggleRight } from 'lucide-react'

export default function RiskAnalysis() {
  const [deployment, setDeployment] = useState('')
  const [analysis, setAnalysis] = useState(null)
  const [loading, setLoading] = useState(false)
  const [memoryEnabled, setMemoryEnabled] = useState(true)

  const handleAnalyze = async () => {
    setLoading(true)
    setTimeout(() => {
      if (!memoryEnabled) {
        // Without Memory: Generic Response
        setAnalysis({
          status: 'NO HISTORICAL SIMILARITY',
          warningMessage: 'Proceed with standard deployment checklist.',
          evidence: null
        })
      } else if (deployment.toLowerCase().includes('connection pool')) {
        // With Memory: Gated lookup finds ADR-7
        setAnalysis({
          status: 'MEMORY-BASED RISK DETECTED',
          warningMessage: 'This change touches a setting that was chosen deliberately after an incident.',
          evidence: {
            historicalExperience: 'Connection pool change leading to database saturation',
            similarity: 'Both changes alter the maximum connection limit on the payment DB',
            previousIncident: 'INC-1024: Payment Service Outage',
            previouslyFailed: 'Increasing the timeout without capping the pool size',
            previouslyWorked: 'Reverting the pool size and implementing circuit breakers',
            lessonLearned: 'Uncapped connection pools overwhelm the primary writer DB instance',
            preventiveAction: 'Ensure circuit breaker is deployed alongside pool changes',
            referenceId: 'ADR-7'
          }
        })
      } else {
        setAnalysis({
          status: 'NO HISTORICAL SIMILARITY',
          warningMessage: 'No active architectural decisions or historical incidents found for this change.',
          evidence: null
        })
      }
      setLoading(false)
    }, 1200)
  }

  const handleFeedback = (outcome) => {
    alert(`Feedback submitted: ${outcome}. Agent will learn from this.`);
  }

  return (
    <div className="p-8 max-w-6xl mx-auto">
      <header className="mb-10 flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-bold text-white mb-2">Memory-Based Risk Detection</h2>
          <p className="text-slate-400">Analyze new deployments against historical engineering experiences.</p>
        </div>
        
        {/* V2: Memory ON/OFF Toggle */}
        <button 
          onClick={() => setMemoryEnabled(!memoryEnabled)}
          className={`flex items-center gap-2 px-4 py-2 rounded-full border transition-colors ${memoryEnabled ? 'bg-blue-900/30 border-blue-800 text-blue-400' : 'bg-slate-800 border-slate-700 text-slate-400'}`}
        >
          {memoryEnabled ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
          <span className="font-medium text-sm">Hindsight Memory: {memoryEnabled ? 'ON' : 'OFF'}</span>
        </button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex flex-col">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Zap className="w-5 h-5 text-blue-500" /> New Deployment Configuration
          </h3>
          <textarea 
            className="w-full bg-slate-950 border border-slate-700 rounded-lg p-4 text-slate-200 mb-4 flex-1 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none resize-none font-mono text-sm"
            placeholder="e.g. Payment Service v2.9\n- connection pool changed to 500"
            value={deployment}
            onChange={(e) => setDeployment(e.target.value)}
          />
          <button 
            onClick={handleAnalyze}
            disabled={loading || !deployment}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            {loading ? 'Analyzing...' : 'Analyze Risk'}
          </button>
        </div>

        <div className="flex flex-col">
          {!analysis && !loading && (
            <div className="flex-1 bg-slate-900 border border-slate-800 border-dashed rounded-xl flex items-center justify-center text-slate-500">
              Awaiting deployment details...
            </div>
          )}

          {loading && (
            <div className="flex-1 bg-slate-900 border border-slate-800 rounded-xl flex flex-col items-center justify-center text-blue-500">
              <Activity className="w-12 h-12 animate-pulse mb-4" />
              <p className="font-medium animate-pulse">Querying Hindsight...</p>
            </div>
          )}

          {analysis && !loading && (
            <div className={`flex-1 rounded-xl p-6 border ${analysis.status === 'MEMORY-BASED RISK DETECTED' ? 'bg-red-950/20 border-red-900/50' : 'bg-green-950/20 border-green-900/50'}`}>
              <div className="flex items-center gap-3 mb-6">
                {analysis.status === 'MEMORY-BASED RISK DETECTED' ? <ShieldAlert className="w-8 h-8 text-red-500" /> : <CheckCircle className="w-8 h-8 text-green-500" />}
                <div>
                  <h3 className={`text-xl font-bold ${analysis.status === 'MEMORY-BASED RISK DETECTED' ? 'text-red-400' : 'text-green-400'}`}>
                    {analysis.status}
                  </h3>
                  <p className="text-slate-400 text-sm mt-1">{analysis.warningMessage}</p>
                </div>
              </div>

              {analysis.evidence && (
                <div className="space-y-6">
                  <div className="bg-slate-900/80 rounded-lg p-5 border border-slate-800">
                    <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">Decision Lineage: {analysis.evidence.referenceId}</h4>
                    <div className="flex items-center gap-4 text-sm">
                      <div className="flex-1 p-3 bg-slate-800 rounded text-slate-300">
                        {analysis.evidence.historicalExperience}
                      </div>
                      <ArrowRight className="w-5 h-5 text-slate-600 shrink-0" />
                      <div className="flex-1 p-3 bg-red-900/20 text-red-400 border border-red-900/30 rounded font-medium">
                        {analysis.evidence.previousIncident}
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-orange-950/20 border border-orange-900/30 rounded-lg p-4">
                      <h4 className="text-xs font-bold text-orange-500 uppercase mb-2">What Failed</h4>
                      <p className="text-sm text-slate-300">{analysis.evidence.previouslyFailed}</p>
                    </div>
                    <div className="bg-green-950/20 border border-green-900/30 rounded-lg p-4">
                      <h4 className="text-xs font-bold text-green-500 uppercase mb-2">What Worked</h4>
                      <p className="text-sm text-slate-300">{analysis.evidence.previouslyWorked}</p>
                    </div>
                  </div>

                  {/* Feedback UI */}
                  <div className="mt-6 pt-6 border-t border-red-900/30 flex items-center justify-between">
                    <span className="text-sm text-slate-400">Was this warning helpful?</span>
                    <div className="flex gap-2">
                      <button onClick={() => handleFeedback('USEFUL')} className="px-3 py-1 text-xs font-medium bg-slate-800 hover:bg-green-900/50 text-slate-300 hover:text-green-400 rounded transition-colors">Useful</button>
                      <button onClick={() => handleFeedback('FALSE_POSITIVE')} className="px-3 py-1 text-xs font-medium bg-slate-800 hover:bg-orange-900/50 text-slate-300 hover:text-orange-400 rounded transition-colors">False Positive</button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
"""

with open("frontend/src/App.jsx", "w") as f: f.write(app_jsx)
with open("frontend/src/pages/Dashboard.jsx", "w") as f: f.write(dashboard_jsx)
with open("frontend/src/pages/RiskAnalysis.jsx", "w") as f: f.write(risk_analysis_jsx)

print("Scaffolded V2 Frontend React Dashboard.")
