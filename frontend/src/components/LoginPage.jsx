import React, { useState } from 'react';
import { Lock, LogIn, PieChart, ShieldAlert, UserPlus, User, FileJson } from 'lucide-react';
import api from '../api/client';

const LoginPage = ({ onLoginSuccess }) => {
    const [isSignup, setIsSignup] = useState(false);
    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');
    const [spreadsheetId, setSpreadsheetId] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            if (isSignup) {
                await api.post('/auth/signup', { loginId, password, spreadsheetId });
                // 자동로그인 처리
                await api.post('/auth/login', { loginId, password });
            } else {
                await api.post('/auth/login', { loginId, password });
            }
            onLoginSuccess();
        } catch (err) {
            if (err.response?.status === 401) {
                setError('아이디 또는 비밀번호가 올바르지 않습니다.');
            } else if (err.response?.data?.error) {
                setError(err.response.data.error);
            } else {
                setError('서버와 통신 중 오류가 발생했습니다.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'radial-gradient(circle at top right, #1e1b4b, #0f172a)',
            padding: '24px',
            color: 'white'
        }}>
            <div style={{
                width: '100%',
                maxWidth: '440px',
                backgroundColor: 'rgba(255, 255, 255, 0.03)',
                backdropFilter: 'blur(16px)',
                borderRadius: '24px',
                padding: '40px',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
                textAlign: 'center'
            }}>
                <div style={{
                    width: '64px',
                    height: '64px',
                    borderRadius: '16px',
                    background: 'linear-gradient(135deg, #6366f1, #4f46e5)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 24px',
                    boxShadow: '0 8px 16px rgba(99, 102, 241, 0.3)'
                }}>
                    <PieChart size={32} color="white" />
                </div>

                <h1 style={{ fontSize: '1.75rem', fontWeight: '800', marginBottom: '8px', letterSpacing: '-0.025em' }}>
                    PAM
                </h1>
                <p style={{ color: 'rgba(255, 255, 255, 0.5)', marginBottom: '32px', fontSize: '0.95rem' }}>
                    {isSignup ? '새로운 계정을 생성하여 자산 관리를 시작하세요.' : '자산 관리를 시작하려면 로그인하세요.'}
                </p>

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '16px', position: 'relative' }}>
                        <div style={{
                            position: 'absolute',
                            left: '16px',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            color: 'rgba(255, 255, 255, 0.4)'
                        }}>
                            <User size={18} />
                        </div>
                        <input
                            type="text"
                            placeholder="아이디"
                            value={loginId}
                            onChange={(e) => setLoginId(e.target.value)}
                            style={{
                                width: '100%',
                                padding: '14px 16px 14px 48px',
                                borderRadius: '12px',
                                border: '1px solid rgba(255, 255, 255, 0.1)',
                                background: 'rgba(255, 255, 255, 0.05)',
                                color: 'white',
                                fontSize: '1rem',
                                outline: 'none',
                                transition: 'all 0.2s',
                            }}
                            required
                        />
                    </div>

                    <div style={{ marginBottom: isSignup ? '16px' : '24px', position: 'relative' }}>
                        <div style={{
                            position: 'absolute',
                            left: '16px',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            color: 'rgba(255, 255, 255, 0.4)'
                        }}>
                            <Lock size={18} />
                        </div>
                        <input
                            type="password"
                            placeholder="비밀번호"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            style={{
                                width: '100%',
                                padding: '14px 16px 14px 48px',
                                borderRadius: '12px',
                                border: '1px solid rgba(255, 255, 255, 0.1)',
                                background: 'rgba(255, 255, 255, 0.05)',
                                color: 'white',
                                fontSize: '1rem',
                                outline: 'none',
                                transition: 'all 0.2s',
                            }}
                            required
                        />
                    </div>

                    {isSignup && (
                        <div style={{ marginBottom: '24px', position: 'relative' }}>
                            <div style={{
                                position: 'absolute',
                                left: '16px',
                                top: '50%',
                                transform: 'translateY(-50%)',
                                color: 'rgba(255, 255, 255, 0.4)'
                            }}>
                                <FileJson size={18} />
                            </div>
                            <input
                                type="text"
                                placeholder="구글 스프레드시트 ID"
                                value={spreadsheetId}
                                onChange={(e) => setSpreadsheetId(e.target.value)}
                                style={{
                                    width: '100%',
                                    padding: '14px 16px 14px 48px',
                                    borderRadius: '12px',
                                    border: '1px solid rgba(255, 255, 255, 0.1)',
                                    background: 'rgba(255, 255, 255, 0.05)',
                                    color: 'white',
                                    fontSize: '1rem',
                                    outline: 'none',
                                    transition: 'all 0.2s',
                                }}
                                required
                            />
                        </div>
                    )}

                    {error && (
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            color: '#ef4444',
                            fontSize: '0.85rem',
                            marginBottom: '20px',
                            justifyContent: 'center',
                            background: 'rgba(239, 68, 68, 0.1)',
                            padding: '10px',
                            borderRadius: '8px',
                            border: '1px solid rgba(239, 68, 68, 0.2)'
                        }}>
                            <ShieldAlert size={14} />
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        style={{
                            width: '100%',
                            padding: '14px',
                            borderRadius: '12px',
                            border: 'none',
                            background: loading ? 'rgba(99, 102, 241, 0.5)' : 'linear-gradient(135deg, #6366f1, #4f46e5)',
                            color: 'white',
                            fontWeight: '700',
                            fontSize: '1rem',
                            cursor: loading ? 'not-allowed' : 'pointer',
                            transition: 'all 0.2s',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: '8px',
                            boxShadow: '0 4px 12px rgba(79, 70, 229, 0.3)',
                            marginBottom: '20px'
                        }}
                    >
                        {loading ? '처리 중...' : (
                            <>
                                {isSignup ? <UserPlus size={18} /> : <LogIn size={18} />}
                                {isSignup ? '가입하기' : '로그인'}
                            </>
                        )}
                    </button>

                    <button
                        type="button"
                        onClick={() => setIsSignup(!isSignup)}
                        style={{
                            background: 'transparent',
                            border: 'none',
                            color: 'rgba(255, 255, 255, 0.5)',
                            fontSize: '0.9rem',
                            cursor: 'pointer',
                            textDecoration: 'underline'
                        }}
                    >
                        {isSignup ? '이미 계정이 있으신가요? 로그인' : '계정이 없으신가요? 회원가입'}
                    </button>
                </form>
            </div>
        </div>
    );
};
