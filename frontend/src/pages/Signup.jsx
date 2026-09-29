import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ShieldAlert, Activity, CheckCircle } from 'lucide-react';

export default function Signup() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [organizationId, setOrganizationId] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSignup = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          username, 
          password,
          organizationId: parseInt(organizationId) || 1
        })
      });

      const data = await response.json();
      
      if (response.ok) {
        setSuccess('Registration successful! Redirecting to login...');
        setTimeout(() => navigate('/login'), 2000);
      } else {
        setError(data.message || 'Registration failed');
      }
    } catch (err) {
      setError('Cannot connect to server');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-screen bg-app-bg items-center justify-center p-4">
      <div className="bg-app-secondary border border-app-border rounded-2xl w-full max-w-md p-8 shadow-2xl">
        <div className="flex justify-center mb-6">
          <Activity className="w-12 h-12 text-app-accent" />
        </div>
        <h2 className="text-2xl font-bold text-app-text-primary text-center mb-8 tracking-tight">Create an Account</h2>
        
        {error && (
          <div className="bg-app-critical/10 border border-app-critical/30 text-app-critical p-3 rounded-lg text-sm mb-6 flex items-center gap-2">
            <ShieldAlert className="w-4 h-4" /> {error}
          </div>
        )}

        {success && (
          <div className="bg-app-success/10 border border-app-success/30 text-app-success p-3 rounded-lg text-sm mb-6 flex items-center gap-2">
            <CheckCircle className="w-4 h-4" /> {success}
          </div>
        )}

        <form onSubmit={handleSignup} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-app-text-secondary mb-2">Username</label>
            <input 
              type="text" 
              required
              className="w-full bg-app-bg border border-app-border rounded-lg p-3 text-app-text-primary focus:border-app-accent outline-none transition-colors"
              value={username}
              onChange={e => setUsername(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-app-text-secondary mb-2">Password</label>
            <input 
              type="password" 
              required
              className="w-full bg-app-bg border border-app-border rounded-lg p-3 text-app-text-primary focus:border-app-accent outline-none transition-colors"
              value={password}
              onChange={e => setPassword(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-app-text-secondary mb-2">Organization ID (e.g. 1)</label>
            <input 
              type="number" 
              required
              className="w-full bg-app-bg border border-app-border rounded-lg p-3 text-app-text-primary focus:border-app-accent outline-none transition-colors"
              value={organizationId}
              onChange={e => setOrganizationId(e.target.value)}
            />
          </div>
          <button 
            type="submit" 
            disabled={loading || success}
            className="w-full bg-app-accent text-app-bg font-bold py-3 rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50 mt-4"
          >
            {loading ? 'Registering...' : 'Sign Up'}
          </button>
        </form>

        <p className="text-center text-app-text-secondary text-sm mt-6">
          Already have an account? <Link to="/login" className="text-app-accent hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
