import React, { useState, useEffect } from 'react'
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

  if (loading) return <div className="p-8 flex items-center justify-center h-full text-app-text-secondary">Loading Organizational Intelligence...</div>

  return (
    <div className="p-8">
      <header className="mb-10">
        <h2 className="text-3xl font-bold text-app-text-primary mb-2">Organizational Memory</h2>
        <p className="text-app-text-secondary">Engineering Intelligence Dashboard</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-10">
        <MetricCard title="Experiences Remembered" value={metrics.experiences} icon={Activity} color="text-app-accent" />
        <MetricCard title="Total Incidents" value={metrics.incidents} icon={AlertTriangle} color="text-app-critical" />
        <MetricCard title="Deployments" value={metrics.deployments} icon={GitCommit} color="text-app-success" />
        <MetricCard title="Recurring Patterns" value={metrics.patterns} icon={Target} color="text-purple-500" />
        
        {/* V2 Metric: Warning Precision */}
        <div className="bg-app-secondary border border-app-border rounded-xl p-6 flex items-start gap-4 col-span-1 md:col-span-2 lg:col-span-1">
          <div className={`p-3 rounded-lg bg-app-bg text-app-memory`}>
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-app-text-secondary mb-1">Warning Precision</p>
            <p className="text-3xl font-bold text-app-text-primary">{metrics.warningPrecision}%</p>
            <p className="text-xs text-app-text-secondary mt-1">Useful ÷ (Useful + False Positive)</p>
          </div>
        </div>
      </div>
    </div>
  )
}

function MetricCard({ title, value, icon: Icon, color }) {
  return (
    <div className="bg-app-secondary border border-app-border rounded-xl p-6 flex items-start gap-4">
      <div className={`p-3 rounded-lg bg-app-bg ${color}`}>
        <Icon className="w-6 h-6" />
      </div>
      <div>
        <p className="text-sm font-medium text-app-text-secondary mb-1">{title}</p>
        <p className="text-3xl font-bold text-app-text-primary">{value}</p>
      </div>
    </div>
  )
}
