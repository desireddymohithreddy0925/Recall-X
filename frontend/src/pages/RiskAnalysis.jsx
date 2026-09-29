import React, { useState } from 'react'
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
          <h2 className="text-3xl font-bold text-app-text-primary mb-2">Memory-Based Risk Detection</h2>
          <p className="text-app-text-secondary">Analyze new deployments against historical engineering experiences.</p>
        </div>
        
        {/* V2: Memory ON/OFF Toggle */}
        <button 
          onClick={() => setMemoryEnabled(!memoryEnabled)}
          className={`flex items-center gap-2 px-4 py-2 rounded-full border transition-colors ${memoryEnabled ? 'bg-app-accent/20 border-app-accent text-app-accent' : 'bg-app-card border-app-border text-app-text-secondary'}`}
        >
          {memoryEnabled ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
          <span className="font-medium text-sm">Hindsight Memory: {memoryEnabled ? 'ON' : 'OFF'}</span>
        </button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="bg-app-secondary border border-app-border rounded-xl p-6 flex flex-col">
          <h3 className="text-lg font-semibold text-app-text-primary mb-4 flex items-center gap-2">
            <Zap className="w-5 h-5 text-app-accent" /> New Deployment Configuration
          </h3>
          <textarea 
            className="w-full bg-app-bg border border-app-border rounded-lg p-4 text-app-text-primary mb-4 flex-1 focus:border-app-accent focus:ring-1 focus:ring-app-accent outline-none resize-none font-mono text-sm"
            placeholder="e.g. Payment Service v2.9
- connection pool changed to 500"
            value={deployment}
            onChange={(e) => setDeployment(e.target.value)}
          />
          <button 
            onClick={handleAnalyze}
            disabled={loading || !deployment}
            className="w-full bg-app-accent text-app-bg hover:opacity-90 text-app-text-primary font-medium py-3 rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            {loading ? 'Analyzing...' : 'Analyze Risk'}
          </button>
        </div>

        <div className="flex flex-col">
          {!analysis && !loading && (
            <div className="flex-1 bg-app-secondary border border-app-border border-dashed rounded-xl flex items-center justify-center text-app-text-secondary">
              Awaiting deployment details...
            </div>
          )}

          {loading && (
            <div className="flex-1 bg-app-secondary border border-app-border rounded-xl flex flex-col items-center justify-center text-app-accent">
              <Activity className="w-12 h-12 animate-pulse mb-4" />
              <p className="font-medium animate-pulse">Querying Hindsight...</p>
            </div>
          )}

          {analysis && !loading && (
            <div className={`flex-1 rounded-xl p-6 border ${analysis.status === 'MEMORY-BASED RISK DETECTED' ? 'bg-app-critical/10 border-app-critical/30' : 'bg-app-success/10 border-app-success/30'}`}>
              <div className="flex items-center gap-3 mb-6">
                {analysis.status === 'MEMORY-BASED RISK DETECTED' ? <ShieldAlert className="w-8 h-8 text-app-critical" /> : <CheckCircle className="w-8 h-8 text-app-success" />}
                <div>
                  <h3 className={`text-xl font-bold ${analysis.status === 'MEMORY-BASED RISK DETECTED' ? 'text-red-400' : 'text-app-success'}`}>
                    {analysis.status}
                  </h3>
                  <p className="text-app-text-secondary text-sm mt-1">{analysis.warningMessage}</p>
                </div>
              </div>

              {analysis.evidence && (
                <div className="space-y-6">
                  <div className="bg-app-secondary/80 rounded-lg p-5 border border-app-border">
                    <h4 className="text-xs font-bold text-app-text-secondary uppercase tracking-wider mb-3">Decision Lineage: {analysis.evidence.referenceId}</h4>
                    <div className="flex items-center gap-4 text-sm">
                      <div className="flex-1 p-3 bg-app-card rounded text-app-text-primary">
                        {analysis.evidence.historicalExperience}
                      </div>
                      <ArrowRight className="w-5 h-5 text-app-text-secondary shrink-0" />
                      <div className="flex-1 p-3 bg-red-900/20 text-red-400 border border-red-900/30 rounded font-medium">
                        {analysis.evidence.previousIncident}
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-app-warning/10 border border-app-warning/30 rounded-lg p-4">
                      <h4 className="text-xs font-bold text-app-warning uppercase mb-2">What Failed</h4>
                      <p className="text-sm text-app-text-primary">{analysis.evidence.previouslyFailed}</p>
                    </div>
                    <div className="bg-app-success/10 border border-app-success/30 rounded-lg p-4">
                      <h4 className="text-xs font-bold text-app-success uppercase mb-2">What Worked</h4>
                      <p className="text-sm text-app-text-primary">{analysis.evidence.previouslyWorked}</p>
                    </div>
                  </div>

                  {/* Feedback UI */}
                  <div className="mt-6 pt-6 border-t border-red-900/30 flex items-center justify-between">
                    <span className="text-sm text-app-text-secondary">Was this warning helpful?</span>
                    <div className="flex gap-2">
                      <button onClick={() => handleFeedback('USEFUL')} className="px-3 py-1 text-xs font-medium bg-app-card hover:bg-app-success/20 text-app-text-primary hover:text-app-success rounded transition-colors">Useful</button>
                      <button onClick={() => handleFeedback('FALSE_POSITIVE')} className="px-3 py-1 text-xs font-medium bg-app-card hover:bg-app-warning/20 text-app-text-primary hover:text-app-warning rounded transition-colors">False Positive</button>
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
