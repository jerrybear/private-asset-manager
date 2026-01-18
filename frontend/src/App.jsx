import React, { useState, useEffect } from 'react';
import Dashboard from './components/Dashboard';
import Overview from './components/Overview';
import LoginPage from './components/LoginPage';
import api from './api/client';
import './styles/index.css';
import { LayoutDashboard, PieChart, LogOut, User, UserX } from 'lucide-react';

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [isAuthenticated, setIsAuthenticated] = useState(null);
  const [user, setUser] = useState(null);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const response = await api.get('/auth/status');
      setIsAuthenticated(response.data.authenticated);
      if (response.data.authenticated) {
        setUser({
          loginId: response.data.loginId,
          spreadsheetId: response.data.spreadsheetId
        });
      }
    } catch (err) {
      console.error('Auth check failed:', err);
      setIsAuthenticated(false);
    }
  };

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
      setIsAuthenticated(false);
      setUser(null);
    } catch (err) {
      console.error('Logout failed:', err);
    }
  };

  const handleWithdraw = async () => {
    if (window.confirm('정말로 탈퇴하시겠습니까? 모든 정보가 삭제됩니다.')) {
      try {
        await api.post('/auth/withdraw');
        setIsAuthenticated(false);
        setUser(null);
        alert('탈퇴 처리가 완료되었습니다.');
      } catch (err) {
        console.error('Withdraw failed:', err);
        alert('탈퇴 처리 중 오류가 발생했습니다.');
      }
    }
  };

  if (isAuthenticated === null) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a', color: 'white' }}>
        <p>인증 확인 중...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage onLoginSuccess={checkAuthStatus} />;
  }

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
          <span style={{ fontWeight: '800', fontSize: '1.2rem', letterSpacing: '-0.025em' }}>PAM</span>
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

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: '500' }}>
            <User size={16} />
            {user?.loginId} 님
          </div>

          <button
            onClick={handleLogout}
            style={{
              display: 'flex', alignItems: 'center', gap: '8px',
              background: 'rgba(255, 255, 255, 0.05)', border: '1px solid rgba(255, 255, 255, 0.1)',
              color: 'var(--text)',
              fontWeight: '600', cursor: 'pointer', padding: '8px 16px', borderRadius: '8px',
              transition: 'all 0.2s',
            }}
          >
            <LogOut size={16} />
            로그아웃
          </button>

          <button
            onClick={handleWithdraw}
            title="회원 탈퇴"
            style={{
              display: 'flex', alignItems: 'center', justifyCenter: 'center',
              background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)',
              color: '#ef4444',
              cursor: 'pointer', padding: '8px', borderRadius: '8px',
              transition: 'all 0.2s',
            }}
          >
            <UserX size={16} />
          </button>
        </div>
      </nav>

      <main style={{ flex: 1 }}>
        {activeTab === 'dashboard' ? <Dashboard /> : <Overview />}
      </main>
    </div>
  );
}

export default App;
