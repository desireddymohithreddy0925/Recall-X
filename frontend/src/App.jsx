import React from 'react'
import { BrowserRouter, Routes, Route, Link, Navigate, useLocation } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import RiskAnalysis from './pages/RiskAnalysis'
import IncidentCapture from './pages/IncidentCapture'
import Login from './pages/Login'
import Signup from './pages/Signup'
import { Activity, ShieldAlert, LayoutDashboard, FileEdit, LogOut } from 'lucide-react'

function PrivateLayout({ children }) {
  const token = localStorage.getItem('token');
  if (!token) return <Navigate to="/login" replace />;

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  };

  return (
    <div className="flex h-screen bg-app-bg text-app-text-primary font-sans">
      <aside className="w-64 bg-app-secondary border-r border-app-border p-4 flex flex-col">
        <div className="flex items-center gap-3 mb-10 px-2">
          <Activity className="w-8 h-8 text-app-accent" />
          <h1 className="text-xl font-bold tracking-tight text-app-text-primary">RECALL-X</h1>
        </div>
        <nav className="flex-1 space-y-2">
          <Link to="/" className="flex items-center gap-3 px-3 py-2 text-app-text-secondary hover:text-app-text-primary hover:bg-app-card rounded-lg transition-colors">
            <LayoutDashboard className="w-5 h-5" /> Dashboard
          </Link>
          <Link to="/risk" className="flex items-center gap-3 px-3 py-2 text-app-text-secondary hover:text-app-text-primary hover:bg-app-card rounded-lg transition-colors">
            <ShieldAlert className="w-5 h-5" /> Risk Analysis
          </Link>
          <Link to="/capture" className="flex items-center gap-3 px-3 py-2 text-app-text-secondary hover:text-app-text-primary hover:bg-app-card rounded-lg transition-colors">
            <FileEdit className="w-5 h-5" /> Incident Capture
          </Link>
        </nav>
        
        <div className="mt-auto pt-4 border-t border-app-border">
          <button onClick={handleLogout} className="flex w-full items-center gap-3 px-3 py-2 text-app-text-secondary hover:text-app-critical hover:bg-app-critical/10 rounded-lg transition-colors">
            <LogOut className="w-5 h-5" /> Sign out
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto bg-app-bg">
        {children}
      </main>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        
        <Route path="/" element={<PrivateLayout><Dashboard /></PrivateLayout>} />
        <Route path="/risk" element={<PrivateLayout><RiskAnalysis /></PrivateLayout>} />
        <Route path="/capture" element={<PrivateLayout><IncidentCapture /></PrivateLayout>} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
