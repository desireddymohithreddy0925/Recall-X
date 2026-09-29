import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, Activity } from 'lucide-react';
import { auth, googleProvider } from '../firebase';
import { signInWithPopup } from 'firebase/auth';

export default function Login() {
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleGoogleSignIn = async () => {
    setLoading(true);
    setError('');

    try {
      const result = await signInWithPopup(auth, googleProvider);
      
      // We must only allow specific domains or just send the token to the backend
      const token = await result.user.getIdToken();
      const email = result.user.email;
      
      // In this example, we restrict to Gmail users, or any email pattern you desire.
      if (!email.endsWith('@gmail.com')) {
        auth.signOut();
        setError("Only valid @gmail.com accounts are allowed.");
        setLoading(false);
        return;
      }
      
      // Optionally, you can send this token to your backend to sync users
      // For now, we trust the Firebase Token on the frontend
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify({ email: email, name: result.user.displayName }));
      navigate('/');
    } catch (err) {
      console.error(err);
      setError(err.message || 'Login failed');
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
        <h2 className="text-2xl font-bold text-app-text-primary text-center mb-8 tracking-tight">Sign in to Recall-X</h2>
        
        {error && (
          <div className="bg-app-critical/10 border border-app-critical/30 text-app-critical p-3 rounded-lg text-sm mb-6 flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 shrink-0" /> {error}
          </div>
        )}

        <button 
          onClick={handleGoogleSignIn}
          disabled={loading}
          className="w-full bg-white text-black font-bold py-3 rounded-lg hover:bg-gray-100 transition-colors disabled:opacity-50 flex items-center justify-center gap-3"
        >
          <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" className="w-5 h-5" />
          {loading ? 'Authenticating...' : 'Sign in with Google'}
        </button>

        <p className="text-center text-app-text-secondary text-sm mt-6">
          Only authorized Gmail accounts are permitted.
        </p>
      </div>
    </div>
  );
}
