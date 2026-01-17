import React, { useState, useEffect } from 'react';
import { X, PlusCircle, Coins, BadgeInfo } from 'lucide-react';
import api from '../api/client';

const AssetInputModal = ({ isOpen, onClose, onAssetAdded, accountId }) => {
    const [formData, setFormData] = useState({
        type: 'STOCK_KR',
        code: '',
        name: '',
        quantity: '',
        averagePurchasePrice: '',
        purchaseDate: new Date().toISOString().split('T')[0]
    });

    const assetTypes = [
        { value: 'STOCK_KR', label: '주식 (국내)' },
        { value: 'STOCK_US', label: '주식 (해외)' },
        { value: 'ETF_KR', label: 'ETF (국내)' },
        { value: 'CRYPTO', label: '가상화폐' },
        { value: 'REITS', label: '리츠' },
        { value: 'BOND_KR', label: '채권 (국내)' },
        { value: 'BOND_US', label: '채권 (해외)' },
        { value: 'GOLD_SPOT', label: '금현물' },
        { value: 'COMMODITY', label: '원자재' },
        { value: 'CASH', label: '현금/예수금' },
        { value: 'RP', label: 'RP' },
        { value: 'ISSUED_NOTE', label: '발행어음' },
        { value: 'DEPOSIT_SAVINGS', label: '예적금' }
    ];

    useEffect(() => {
        if (isOpen) {
            setFormData({
                type: 'STOCK_KR',
                code: '',
                name: '',
                quantity: '',
                averagePurchasePrice: '',
                purchaseDate: new Date().toISOString().split('T')[0]
            });
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const isCodeRequired = ['STOCK_KR', 'STOCK_US', 'ETF_KR', 'REITS', 'COMMODITY', 'BOND_KR', 'BOND_US', 'GOLD_SPOT', 'CRYPTO'].includes(formData.type);

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const assetData = {
                ...formData,
                quantity: parseFloat(formData.quantity),
                averagePurchasePrice: parseFloat(formData.averagePurchasePrice)
            };
            const response = await api.post(`/accounts/${accountId}/assets`, assetData);
            onAssetAdded(response.data);
            onClose();
        } catch (error) {
            console.error('Error adding asset:', error);
            alert(error.response?.data || '자산 추가 중 오류가 발생했습니다.');
        }
    };

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
            backgroundColor: 'rgba(0,0,0,0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center',
            zIndex: 1000, backdropFilter: 'blur(10px)'
        }}>
            <div className="glass-card animate-fade-in" style={{
                width: '480px', position: 'relative', padding: '32px',
                border: '1px solid rgba(99, 102, 241, 0.3)',
                boxShadow: '0 20px 50px rgba(0,0,0,0.5)'
            }}>
                <button onClick={onClose} style={{
                    position: 'absolute', right: '20px', top: '20px', background: 'none', border: 'none',
                    color: 'var(--text-muted)', cursor: 'pointer', transition: 'color 0.2s'
                }} className="hover-lift">
                    <X size={24} />
                </button>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '28px' }}>
                    <PlusCircle size={28} color="var(--primary)" />
                    <h2 style={{ fontSize: '1.5rem', fontWeight: '800', letterSpacing: '-0.5px' }}>새 자산 등록</h2>
                </div>

                <form onSubmit={handleSubmit}>
                    <div style={{ marginBottom: '20px' }}>
                        <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>자산 유형</label>
                        <select
                            value={formData.type}
                            onChange={(e) => setFormData({ ...formData, type: e.target.value, code: '', name: '' })}
                            required
                            className="inline-input"
                            style={{ height: '48px', borderRadius: '12px' }}
                        >
                            {assetTypes.map(type => (
                                <option key={type.value} value={type.value}>{type.label}</option>
                            ))}
                        </select>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
                        {isCodeRequired ? (
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>종목코드</label>
                                <input
                                    type="text"
                                    value={formData.code}
                                    onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                                    placeholder="예: 005930 / TSLA"
                                    required
                                    className="inline-input"
                                    style={{ height: '48px', borderRadius: '12px' }}
                                />
                            </div>
                        ) : (
                            <div>
                                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>자산명</label>
                                <input
                                    type="text"
                                    value={formData.name}
                                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    placeholder="예: 예수금 / 발행어음"
                                    required
                                    className="inline-input"
                                    style={{ height: '48px', borderRadius: '12px' }}
                                />
                            </div>
                        )}
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>매수일자</label>
                            <input
                                type="date"
                                value={formData.purchaseDate}
                                onChange={(e) => setFormData({ ...formData, purchaseDate: e.target.value })}
                                required
                                className="inline-input"
                                style={{ height: '48px', borderRadius: '12px' }}
                            />
                        </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '28px' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>보유량 (수량)</label>
                            <input
                                type="number"
                                step="any"
                                value={formData.quantity}
                                onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                                placeholder="0"
                                required
                                className="inline-input"
                                style={{ height: '48px', borderRadius: '12px', textAlign: 'right' }}
                            />
                        </div>
                        <div>
                            <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-muted)', fontSize: '0.85rem', fontWeight: '600' }}>매수평균가</label>
                            <input
                                type="number"
                                step="any"
                                value={formData.averagePurchasePrice}
                                onChange={(e) => setFormData({ ...formData, averagePurchasePrice: e.target.value })}
                                placeholder="0"
                                required
                                className="inline-input"
                                style={{ height: '48px', borderRadius: '12px', textAlign: 'right' }}
                            />
                        </div>
                    </div>

                    <button type="submit"
                        className="btn-primary"
                        style={{
                            width: '100%', padding: '16px', borderRadius: '14px', fontSize: '1.1rem', fontWeight: '700',
                            backgroundColor: 'var(--primary)',
                            boxShadow: '0 8px 20px rgba(99, 102, 241, 0.35)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px'
                        }}>
                        <Coins size={20} />
                        자산 등록 완료
                    </button>
                </form>

                <div style={{ marginTop: '16px', display: 'flex', alignItems: 'flex-start', gap: '8px', color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                    <BadgeInfo size={14} style={{ flexShrink: 0, marginTop: '2px' }} />
                    <p>주식 종목을 추가하면 실시간 주가 데이터와 배당금 정보를 기반으로 포트폴리오가 자동 계산됩니다.</p>
                </div>
            </div>
        </div>
    );
};

export default AssetInputModal;
