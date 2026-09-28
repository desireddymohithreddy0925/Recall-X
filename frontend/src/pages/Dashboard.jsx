import React, { useState, useEffect } from 'react'
import { Activity, ShieldAlert, GitCommit, Target, AlertTriangle } from 'lucide-react'

export default function Dashboard() {
  const [metrics, setMetrics] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // In production, this fetches from /api/metrics
    setTimeout(() => {
      setMetrics({
        experiences: 124,
        incidents: 45,
        deployments: 78,
        patterns: 4,
        failedFixes: 23,
        lessons: 34
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
        <MetricCard title="Failed Fixes" value={metrics.failedFixes} icon={ShieldAlert} color="text-orange-500" />
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
