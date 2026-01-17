import React, { useState } from 'react';
import Dashboard from './components/Dashboard';
import Overview from './components/Overview';
import './styles/index.css';
import { LayoutDashboard, PieChart } from 'lucide-react';

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');

  return (
    <div className="App" style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <nav style={{
        padding: '16px 32px',
        background: 'rgba(255, 255, 255, 0.02)',
        borderBottom: '1px solid var(--border)',
        display: 'flex',
        gap: '24px',
        alignItems: 'center',
        backdropFilter: 'blur(10px)',
        position: 'sticky',
        top: 0,
        zIndex: 100
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginRight: '24px' }}>
          <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white' }}>
            <PieChart size={20} />
          </div>
          <span style={{ fontWeight: '800', fontSize: '1.2rem', letterSpacing: '-0.025em' }}>ANTIGRAVITY</span>
        </div>

        <button
          onClick={() => setActiveTab('dashboard')}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            background: 'transparent', border: 'none',
            color: activeTab === 'dashboard' ? 'var(--primary)' : 'var(--text-muted)',
            fontWeight: '600', cursor: 'pointer', padding: '8px 12px', borderRadius: '8px',
            transition: 'all 0.2s',
            backgroundColor: activeTab === 'dashboard' ? 'rgba(99, 102, 241, 0.1)' : 'transparent'
          }}
        >
          <LayoutDashboard size={18} />
          대시보드
        </button>

        <button
          onClick={() => setActiveTab('overview')}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            background: 'transparent', border: 'none',
            color: activeTab === 'overview' ? 'var(--primary)' : 'var(--text-muted)',
            fontWeight: '600', cursor: 'pointer', padding: '8px 12px', borderRadius: '8px',
            transition: 'all 0.2s',
            backgroundColor: activeTab === 'overview' ? 'rgba(99, 102, 241, 0.1)' : 'transparent'
          }}
        >
          <PieChart size={18} />
          전체 자산 현황
        </button>
      </nav>

      <main style={{ flex: 1 }}>
        {activeTab === 'dashboard' ? <Dashboard /> : <Overview />}
      </main>
    </div>
  );
}

export default App;
