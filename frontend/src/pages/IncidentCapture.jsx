import React, { useState } from 'react'
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
        <CheckCircle className="w-16 h-16 text-app-success mb-6" />
        <h2 className="text-3xl font-bold text-app-text-primary mb-2">Memory Retained</h2>
        <p className="text-app-text-secondary mb-8">INC-1025 has been consolidated into organizational memory.</p>
        <button onClick={() => setSuccess(false)} className="bg-app-card hover:bg-app-elevated text-app-text-primary px-6 py-2 rounded-full transition-colors">
          Capture Another Incident
        </button>
      </div>
    )
  }

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <header className="mb-10">
        <h2 className="text-3xl font-bold text-app-text-primary mb-2">What We Learned</h2>
        <p className="text-app-text-secondary">Capture the failed fixes and final resolutions before closing the incident ticket.</p>
      </header>

      <div className="bg-app-accent/10 border border-app-accent/30 rounded-lg p-4 mb-8 flex items-start gap-3">
        <AlertCircle className="w-5 h-5 text-app-accent shrink-0 mt-0.5" />
        <p className="text-sm text-app-text-primary">
          <strong>Pre-filled from INC-1025 timeline.</strong> Please confirm the failed attempts and provide the final lesson learned to close this incident.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="bg-app-secondary border border-app-border rounded-xl p-6 space-y-6">
        <div>
          <label className="block text-sm font-medium text-app-text-secondary mb-2 flex items-center gap-2">
            <FileText className="w-4 h-4" /> Incident Title
          </label>
          <input 
            type="text"
            className="w-full bg-app-bg border border-app-border rounded-lg p-3 text-app-text-primary focus:border-app-accent outline-none"
            value={formData.title}
            readOnly
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-app-warning mb-2">What Failed (Attempted Fixes)</label>
            <textarea 
              className="w-full bg-app-bg border border-app-border rounded-lg p-3 text-app-text-primary focus:border-app-warning outline-none h-24"
              value={formData.whatFailed}
              onChange={(e) => setFormData({...formData, whatFailed: e.target.value})}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-app-success mb-2">What Worked (Resolution)</label>
            <textarea 
              className="w-full bg-app-bg border border-app-border rounded-lg p-3 text-app-text-primary focus:border-app-success outline-none h-24"
              value={formData.whatWorked}
              onChange={(e) => setFormData({...formData, whatWorked: e.target.value})}
            />
          </div>
        </div>

        <div className="border-t border-app-border pt-6">
          <label className="block text-sm font-bold text-app-text-primary mb-2">The Lesson Learned (Required)</label>
          <p className="text-xs text-app-text-secondary mb-3">If nothing new was learned, explicitly write "Nothing new learned".</p>
          <textarea 
            required
            className="w-full bg-app-bg border border-app-border rounded-lg p-3 text-app-text-primary focus:border-app-accent outline-none h-24 placeholder:text-app-text-secondary"
            placeholder="e.g. Auth service latency causes cascading timeouts in payment gateway. Must scale both together."
            value={formData.lesson}
            onChange={(e) => setFormData({...formData, lesson: e.target.value})}
          />
        </div>

        <button 
          type="submit"
          disabled={loading || !formData.lesson.trim()}
          className="w-full bg-app-accent text-app-bg hover:opacity-90 text-app-text-primary font-medium py-3 rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2 mt-4"
        >
          {loading ? 'Retaining in Memory...' : <><Send className="w-5 h-5" /> Retain & Close Incident</>}
        </button>
      </form>
    </div>
  )
}
