import React, { useState } from 'react';
import { X, Lock, ShieldCheck, PlusCircle } from 'lucide-react';
import api from '../api/client';

const AccountModal = ({ isOpen, onClose, onAccountCreated, initialAccount = null }) => {
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        sheetName: '',
        owner: '',
        accountType: 'REGULAR',
        financialInstitution: '',
        accountNumber: ''
    });

    const accountTypes = [
        { value: 'REGULAR', label: '일반' },
        { value: 'PENSION', label: '연금' },
        { value: 'ISA', label: 'ISA' },
        { value: 'IRP', label: 'IRP' },
        { value: 'SPECIAL', label: '기타특수' }
    ];

    const [sheetNames, setSheetNames] = useState([]);
    const [isLoadingSheets, setIsLoadingSheets] = useState(false);

    React.useEffect(() => {
        if (isOpen) {
            fetchSheetNames();
            if (initialAccount) {
                setFormData({
                    name: initialAccount.name || '',
                    description: initialAccount.description || '',
                    sheetName: initialAccount.sheetName || '',
                    owner: initialAccount.owner || '',
                    accountType: initialAccount.accountType || 'REGULAR',
                    financialInstitution: initialAccount.financialInstitution || '',
                    accountNumber: initialAccount.accountNumber || ''
                });
            } else {
                setFormData({
                    name: '',
                    description: '',
                    sheetName: '',
                    owner: '',
                    accountType: 'REGULAR',
                    financialInstitution: '',
                    accountNumber: ''
                });
            }
        }
    }, [isOpen, initialAccount]);

    const fetchSheetNames = async () => {
        try {
            setIsLoadingSheets(true);
            const response = await api.get('/accounts/sheet-names');
            let data = response.data;

            // 생성 모드에서만 [RAWDATA] 필터링
            if (!initialAccount) {
                data = data.filter(name => !name.startsWith('[RAWDATA]'));
            }

            setSheetNames(data);

            if (data.length > 0 && !formData.sheetName && !initialAccount) {
                setFormData(prev => ({ ...prev, sheetName: data[0] }));
            }
        } catch (error) {
            console.error('Error fetching sheet names:', error);
        } finally {
            setIsLoadingSheets(false);
        }
    };

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            let response;
            if (initialAccount) {
                response = await api.put(`/accounts/${initialAccount.id}`, formData);
            } else {
                response = await api.post('/accounts', formData);
            }
            onAccountCreated(response.data, !!initialAccount);
            onClose();
        } catch (error) {
            console.error('Error saving account:', error);
            const errorMessage = error.response?.data || '계좌 저장 중 오류가 발생했습니다.';
            alert(errorMessage);
        }
    };

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
            backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', justifyContent: 'center', alignItems: 'center',
            zIndex: 1000, backdropFilter: 'blur(8px)'
        }}>
            <div className="glass-card animate-fade-in" style={{
                width: '450px', position: 'relative', padding: '28px',
                border: initialAccount ? '1px solid rgba(139, 92, 246, 0.3)' : '1px solid rgba(99, 102, 241, 0.3)'
            }}>
                <button onClick={onClose} style={{
                    position: 'absolute', right: '16px', top: '16px', background: 'none', border: 'none',
                    color: 'var(--text-muted)', cursor: 'pointer'
                }}>
                    <X size={20} />
                </button>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
                    {initialAccount ? (
                        <ShieldCheck size={24} color="var(--accent)" />
                    ) : (
                        <PlusCircle size={24} color="var(--primary)" />
                    )}
                    <h2 style={{ fontSize: '1.25rem', fontWeight: '800' }}>
                        {initialAccount ? '계좌 정보 수정' : '새 계좌 등록'}
                    </h2>
                </div>

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '20px' }}>
                        <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>계좌 이름</label>
                        <input type="text" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                            placeholder="예: 배당주 계좌" required style={{
                                width: '100%', padding: '12px', borderRadius: '10px', background: 'var(--bg-dark)',
                                border: '1px solid var(--border)', color: 'var(--text-main)', fontSize: '1rem'
                            }} />
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>소유자</label>
                            <input type="text" value={formData.owner} onChange={(e) => setFormData({ ...formData, owner: e.target.value })}
                                placeholder="이름" required style={{
                                    width: '100%', padding: '12px', borderRadius: '10px', background: 'var(--bg-dark)',
                                    border: '1px solid var(--border)', color: 'var(--text-main)'
                                }} />
                        </div>
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>계좌 유형</label>
                            <select value={formData.accountType} onChange={(e) => setFormData({ ...formData, accountType: e.target.value })}
                                required style={{
                                    width: '100%', padding: '12px', borderRadius: '10px', background: 'var(--bg-dark)',
                                    border: '1px solid var(--border)', color: 'var(--text-main)', cursor: 'pointer'
                                }}>
                                {accountTypes.map(type => (
                                    <option key={type.value} value={type.value}>{type.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>금융기관</label>
                            <input type="text" value={formData.financialInstitution} onChange={(e) => setFormData({ ...formData, financialInstitution: e.target.value })}
                                placeholder="예: 미래에셋" required style={{
                                    width: '100%', padding: '12px', borderRadius: '10px', background: 'var(--bg-dark)',
                                    border: '1px solid var(--border)', color: 'var(--text-main)'
                                }} />
                        </div>
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>계좌번호</label>
                            <input type="text" value={formData.accountNumber} onChange={(e) => setFormData({ ...formData, accountNumber: e.target.value })}
                                placeholder="123-45-..." required style={{
                                    width: '100%', padding: '12px', borderRadius: '10px', background: 'var(--bg-dark)',
                                    border: '1px solid var(--border)', color: 'var(--text-main)'
                                }} />
                        </div>
                    </div>

                    <div style={{ marginBottom: '20px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                            <label style={{ color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>연동된 시트 탭</label>
                            {initialAccount && <Lock size={14} color="var(--text-muted)" />}
                        </div>
                        <select
                            value={formData.sheetName}
                            onChange={(e) => setFormData({ ...formData, sheetName: e.target.value })}
                            required
                            disabled={!!initialAccount}
                            style={{
                                width: '100%', padding: '12px', borderRadius: '10px',
                                background: initialAccount ? 'rgba(255, 255, 255, 0.05)' : 'var(--bg-dark)',
                                border: initialAccount ? '1px dashed var(--border)' : '1px solid var(--border)',
                                color: initialAccount ? 'var(--text-muted)' : 'var(--text-main)',
                                cursor: initialAccount ? 'not-allowed' : 'pointer'
                            }}
                        >
                            {isLoadingSheets ? (
                                <option>시트 목록 로딩 중...</option>
                            ) : (
                                sheetNames.map(name => (
                                    <option key={name} value={name}>{name}</option>
                                ))
                            )}
                        </select>
                        <p style={{ fontSize: '0.75rem', color: initialAccount ? 'var(--accent)' : 'var(--text-muted)', marginTop: '6px' }}>
                            {initialAccount ? '⚠️ 데이터 정합성을 위해 연동 시트는 수정이 불가능합니다.' : '✅ 자산 데이터를 가져올 시트 탭을 선택하세요.'}
                        </p>
                    </div>

                    <div style={{ marginBottom: '28px' }}>
                        <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>설명 (선택)</label>
                        <textarea value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            placeholder="계좌에 대한 설명을 입력하세요" style={{
                                width: '100%', padding: '12px', borderRadius: '10px', background: 'var(--bg-dark)',
                                border: '1px solid var(--border)', color: 'var(--text-main)', minHeight: '60px', resize: 'none'
                            }} />
                    </div>

                    <button type="submit"
                        className={initialAccount ? 'btn-secondary' : 'btn-primary'}
                        style={{
                            width: '100%', padding: '14px', borderRadius: '12px', fontSize: '1rem', fontWeight: '700',
                            backgroundColor: initialAccount ? 'var(--accent)' : 'var(--primary)',
                            boxShadow: initialAccount ? '0 4px 12px rgba(139, 92, 246, 0.3)' : '0 4px 12px rgba(99, 102, 241, 0.3)'
                        }}>
                        {initialAccount ? '변경 사항 저장' : '계좌 등록 완료'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default AccountModal;
