import React, { useState } from 'react'
import { ShieldAlert, CheckCircle, Activity, ArrowRight, Zap } from 'lucide-react'

export default function RiskAnalysis() {
  const [deployment, setDeployment] = useState('')
  const [analysis, setAnalysis] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleAnalyze = async () => {
    setLoading(true)
    // Simulate API call to /api/deployments/analyze
    setTimeout(() => {
      if (deployment.toLowerCase().includes('connection pool')) {
        setAnalysis({
          status: 'MEMORY-BASED RISK DETECTED',
          warningMessage: 'This change resembles historical changes associated with previous incidents.',
          evidence: {
            historicalExperience: 'Connection pool change leading to database saturation',
            similarity: 'Both changes alter the maximum connection limit on the payment DB',
            previousIncident: 'INC-1024: Payment Service Outage',
            previouslyFailed: 'Increasing the timeout without capping the pool size',
            previouslyWorked: 'Reverting the pool size and implementing circuit breakers',
            lessonLearned: 'Uncapped connection pools overwhelm the primary writer DB instance',
            preventiveAction: 'Ensure circuit breaker is deployed alongside pool changes'
          }
        })
      } else {
        setAnalysis({
          status: 'NO HISTORICAL SIMILARITY',
          warningMessage: 'No significant historical precedents found for this change.',
          evidence: null
        })
      }
      setLoading(false)
    }, 1500)
  }

  return (
    <div className="p-8 max-w-6xl mx-auto">
      <header className="mb-10">
        <h2 className="text-3xl font-bold text-white mb-2">Memory-Based Risk Detection</h2>
        <p className="text-slate-400">Analyze new deployments against historical engineering experiences before they ship.</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Input Section */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 flex flex-col">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Zap className="w-5 h-5 text-blue-500" /> New Deployment Configuration
          </h3>
          <textarea 
            className="w-full bg-slate-950 border border-slate-700 rounded-lg p-4 text-slate-200 mb-4 flex-1 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none resize-none font-mono text-sm"
            placeholder="e.g. Payment Service v2.9
- connection pool changed to 500
- retry logic updated
- timeout changed to 30s"
            value={deployment}
            onChange={(e) => setDeployment(e.target.value)}
          />
          <button 
            onClick={handleAnalyze}
            disabled={loading || !deployment}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {loading ? 'Analyzing Historical Memory...' : 'Analyze Deployment Risk'}
          </button>
        </div>

        {/* Output Section */}
        <div className="flex flex-col">
          {!analysis && !loading && (
            <div className="flex-1 bg-slate-900 border border-slate-800 border-dashed rounded-xl flex items-center justify-center text-slate-500">
              Awaiting deployment details...
            </div>
          )}

          {loading && (
            <div className="flex-1 bg-slate-900 border border-slate-800 rounded-xl flex flex-col items-center justify-center text-blue-500">
              <Activity className="w-12 h-12 animate-pulse mb-4" />
              <p className="font-medium animate-pulse">Querying Hindsight Memory Layer...</p>
            </div>
          )}

          {analysis && !loading && (
            <div className={`flex-1 rounded-xl p-6 border ${analysis.status === 'MEMORY-BASED RISK DETECTED' ? 'bg-red-950/20 border-red-900/50' : 'bg-green-950/20 border-green-900/50'}`}>
              
              <div className="flex items-center gap-3 mb-6">
                {analysis.status === 'MEMORY-BASED RISK DETECTED' ? (
                  <ShieldAlert className="w-8 h-8 text-red-500" />
                ) : (
                  <CheckCircle className="w-8 h-8 text-green-500" />
                )}
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
                    <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">Memory Lineage</h4>
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
                      <h4 className="text-xs font-bold text-orange-500 uppercase tracking-wider mb-2">What Failed</h4>
                      <p className="text-sm text-slate-300">{analysis.evidence.previouslyFailed}</p>
                    </div>
                    <div className="bg-green-950/20 border border-green-900/30 rounded-lg p-4">
                      <h4 className="text-xs font-bold text-green-500 uppercase tracking-wider mb-2">What Worked</h4>
                      <p className="text-sm text-slate-300">{analysis.evidence.previouslyWorked}</p>
                    </div>
                  </div>

                  <div className="bg-blue-950/20 border border-blue-900/30 rounded-lg p-5">
                    <h4 className="text-xs font-bold text-blue-500 uppercase tracking-wider mb-2">Preventive Action Required</h4>
                    <p className="text-slate-200 font-medium">{analysis.evidence.preventiveAction}</p>
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
