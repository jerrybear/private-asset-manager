import React, { useState, useEffect } from 'react';
import { LayoutDashboard, TrendingUp, Calendar, PieChart as PieChartIcon, Filter, Users, Wallet, CreditCard, ChevronRight } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import api from '../api/client';

const Overview = () => {
    const [summaries, setSummaries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [ownerFilter, setOwnerFilter] = useState('ALL');
    const [owners, setOwners] = useState([]);

    useEffect(() => {
        fetchSummaries();
    }, []);

    const fetchSummaries = async () => {
        try {
            setLoading(true);
            const response = await api.get('/accounts/summary');
            setSummaries(response.data);

            // Extract unique owners
            const uniqueOwners = ['ALL', ...new Set(response.data.map(s => s.owner).filter(Boolean))];
            setOwners(uniqueOwners);
        } catch (error) {
            console.error('Error fetching summaries:', error);
        } finally {
            setLoading(false);
        }
    };

    const filteredSummaries = ownerFilter === 'ALL'
        ? summaries
        : summaries.filter(s => s.owner === ownerFilter);

    // Calculate totals for filtered data
    const totalAssets = filteredSummaries.reduce((sum, s) => sum + (s.totalCurrentValue || 0), 0);
    const totalDividend = filteredSummaries.reduce((sum, s) => sum + (s.totalExpectedDividend || 0), 0);
    const totalPurchase = filteredSummaries.reduce((sum, s) => sum + (s.totalPurchaseAmount || 0), 0);
    const totalProfitLoss = totalAssets - totalPurchase;
    const totalReturnRate = totalPurchase > 0 ? (totalProfitLoss / totalPurchase) * 100 : 0;

    // Asset Type Mapping (Synced with backend AssetType enum)
    const ASSET_TYPE_LABELS = {
        STOCK: '주식',
        STOCK_KR: '국내주식',
        STOCK_US: '해외주식',
        ETF_KR: '국내ETF',
        BOND_KR: '국내채권',
        BOND_US: '해외채권',
        REITS: '리츠',
        COMMODITY: '원자재',
        CASH: '현금',
        CRYPTO: '가상화폐',
        RP: 'RP',
        ISSUED_NOTE: '발행어음',
        BOND: '채권',
        DEPOSIT_SAVINGS: '예적금',
        GOLD_SPOT: '금현물'
    };

    // Aggregate asset allocation by type
    const assetAllocation = filteredSummaries.reduce((acc, summary) => {
        if (summary.assets) {
            summary.assets.forEach(asset => {
                const type = asset.type || 'STOCK';
                const value = asset.currentValue || 0;
                acc[type] = (acc[type] || 0) + value;
            });
        }
        return acc;
    }, {});

    const chartData = Object.entries(assetAllocation)
        .filter(([_, value]) => value > 0)
        .map(([type, value]) => ({
            name: ASSET_TYPE_LABELS[type] || type,
            value: value,
            percent: totalAssets > 0 ? (value / totalAssets) * 100 : 0
        }))
        .sort((a, b) => b.value - a.value);

    // Modern, Premium Color Palette
    const COLORS = [
        '#6366f1', // Indigo
        '#8b5cf6', // Violet
        '#ec4899', // Pink
        '#f59e0b', // Amber
        '#10b981', // Emerald
        '#3b82f6', // Blue
        '#ef4444', // Red
        '#06b6d4', // Cyan
        '#f97316', // Orange
        '#84cc16'  // Lime
    ];

    if (loading) return <div style={{ padding: '32px', color: 'var(--text-muted)' }}>데이터를 불러오는 중...</div>;

    const ACCOUNT_TYPE_LABELS = {
        REGULAR: '일반',
        PENSION: '연금',
        ISA: 'ISA',
        IRP: 'IRP',
        SPECIAL: '특수'
    };

    const CustomTooltip = ({ active, payload }) => {
        if (active && payload && payload.length) {
            return (
                <div style={{
                    background: 'rgba(15, 23, 42, 0.9)',
                    backdropFilter: 'blur(8px)',
                    border: '1px solid var(--border)',
                    padding: '12px',
                    borderRadius: '12px',
                    boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.3)'
                }}>
                    <p style={{ fontWeight: 'bold', marginBottom: '4px', color: '#fff' }}>{payload[0].name}</p>
                    <p style={{ color: 'var(--primary)', fontWeight: '700', fontSize: '1.1rem' }}>
                        ₩{payload[0].value.toLocaleString()}
                    </p>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                        비중: {payload[0].payload.percent.toFixed(1)}%
                    </p>
                </div>
            );
        }
        return null;
    };

    return (
        <div className="animate-fade-in" style={{ padding: '32px', maxWidth: '1200px', margin: '0 auto' }}>
            <header style={{ marginBottom: '32px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <div>
                    <h1 style={{ fontSize: '2.5rem', fontWeight: '900', letterSpacing: '-0.025em' }}>Assets Overview</h1>
                    <p style={{ color: 'var(--text-muted)', marginTop: '4px', fontSize: '1.1rem' }}>모든 계좌의 자산 현황을 한눈에 확인하세요</p>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px', background: 'rgba(255,255,255,0.03)', borderRadius: '12px', border: '1px solid var(--border)', backdropFilter: 'blur(10px)' }}>
                        <Users size={18} color="var(--text-muted)" />
                        <select
                            value={ownerFilter}
                            onChange={(e) => setOwnerFilter(e.target.value)}
                            style={{ background: 'transparent', border: 'none', color: 'var(--text-main)', fontSize: '0.95rem', fontWeight: '600', outline: 'none', cursor: 'pointer' }}
                        >
                            {owners.map(owner => (
                                <option key={owner} value={owner} style={{ background: 'var(--bg-main)' }}>
                                    {owner === 'ALL' ? '전체 소유자' : owner}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>
            </header>

            <div className="grid-dashboard" style={{ marginBottom: '32px', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))' }}>
                <div className="glass-card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                        <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(99, 102, 241, 0.1)' }}>
                            <Wallet size={20} color="var(--primary)" />
                        </div>
                        <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: '600' }}>총 자산 합계</span>
                    </div>
                    <div style={{ fontSize: '2rem', fontWeight: '900', letterSpacing: '-0.02em' }}>
                        ₩{totalAssets.toLocaleString()}
                    </div>
                    <div style={{
                        color: totalProfitLoss >= 0 ? 'var(--success)' : 'var(--danger)',
                        marginTop: '12px', fontSize: '1rem', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '4px'
                    }}>
                        {totalProfitLoss >= 0 ? '+' : ''}₩{totalProfitLoss.toLocaleString()} ({totalReturnRate.toFixed(2)}%)
                    </div>
                </div>

                <div className="glass-card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                        <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.1)' }}>
                            <Calendar size={20} color="var(--accent)" />
                        </div>
                        <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: '600' }}>예상 연간 배당금</span>
                    </div>
                    <div style={{ fontSize: '2rem', fontWeight: '900', letterSpacing: '-0.02em' }}>
                        ₩{totalDividend.toLocaleString()}
                    </div>
                    <div style={{ color: 'var(--text-muted)', marginTop: '12px', fontSize: '1rem', fontWeight: '500' }}>
                        연 수익률 약 {(totalAssets > 0 ? (totalDividend / totalAssets) * 100 : 0).toFixed(2)}%
                    </div>
                </div>

                <div className="glass-card" style={{ gridColumn: 'span 2', display: 'flex', gap: '24px', alignItems: 'center', minHeight: '200px' }}>
                    <div style={{ flex: 1, height: '180px' }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={chartData}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={50}
                                    outerRadius={80}
                                    paddingAngle={5}
                                    dataKey="value"
                                    animationDuration={1500}
                                >
                                    {chartData.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="rgba(255,255,255,0.1)" strokeWidth={2} />
                                    ))}
                                </Pie>
                                <Tooltip content={<CustomTooltip />} />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                    <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                            <PieChartIcon size={18} color="var(--primary)" />
                            <span style={{ fontWeight: '700', fontSize: '1rem' }}>자산 구성 비중</span>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', maxHeight: '140px', overflowY: 'auto', paddingRight: '4px' }}>
                            {chartData.map((item, index) => (
                                <div key={item.name} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.85rem' }}>
                                    <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: COLORS[index % COLORS.length] }} />
                                    <span style={{ color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={item.name}>{item.name}</span>
                                    <span style={{ fontWeight: '700', marginLeft: 'auto' }}>{item.percent.toFixed(1)}%</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            <div className="glass-card" style={{ padding: '0', overflow: 'hidden' }}>
                <div style={{ padding: '24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <div style={{ background: 'rgba(255,255,255,0.05)', padding: '8px', borderRadius: '8px' }}>
                            <TrendingUp size={20} />
                        </div>
                        <span style={{ fontWeight: '700', fontSize: '1.2rem' }}>계좌별 상세 현황</span>
                    </div>
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: '500' }}>
                        총 {filteredSummaries.length}개 계좌
                    </span>
                </div>
                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                        <thead>
                            <tr style={{ color: 'var(--text-muted)', fontSize: '0.85rem', background: 'rgba(255,255,255,0.02)' }}>
                                <th style={{ padding: '16px 24px' }}>계좌 정보</th>
                                <th style={{ padding: '16px 24px' }}>금융기관 / 번호</th>
                                <th style={{ padding: '16px 24px', textAlign: 'right' }}>총 자산 평가액</th>
                                <th style={{ padding: '16px 24px', textAlign: 'right' }}>예상 배당금</th>
                                <th style={{ padding: '16px 24px', textAlign: 'right' }}>수익률</th>
                                <th style={{ padding: '16px 24px', textAlign: 'right', width: '120px' }}>비중</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredSummaries.map((s, index) => {
                                const weight = totalAssets > 0 ? ((s.totalCurrentValue || 0) / totalAssets) * 100 : 0;
                                return (
                                    <tr key={s.accountId || index} style={{ borderBottom: index === filteredSummaries.length - 1 ? 'none' : '1px solid var(--border-light)', transition: 'background 0.2s' }} className="hover-highlight">
                                        <td style={{ padding: '20px 24px' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                                <div style={{
                                                    padding: '10px', borderRadius: '12px', background: 'rgba(99, 102, 241, 0.1)', color: 'var(--primary)'
                                                }}>
                                                    <CreditCard size={20} />
                                                </div>
                                                <div>
                                                    <div style={{ fontWeight: '700', fontSize: '1.05rem' }}>{s.accountName}</div>
                                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
                                                        <span style={{ fontSize: '0.75rem', padding: '2px 8px', borderRadius: '100px', background: 'rgba(255,255,255,0.05)', color: 'var(--text-muted)', border: '1px solid var(--border)' }}>
                                                            {ACCOUNT_TYPE_LABELS[s.accountType] || s.accountType}
                                                        </span>
                                                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: '500' }}>{s.owner}</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                        <td style={{ padding: '20px 24px' }}>
                                            <div style={{ color: 'var(--text-main)', fontSize: '0.9rem', fontWeight: '600' }}>{s.financialInstitution}</div>
                                            <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '4px' }}>{s.accountNumber}</div>
                                        </td>
                                        <td style={{ padding: '20px 24px', textAlign: 'right' }}>
                                            <div style={{ fontWeight: '700', fontSize: '1.05rem' }}>₩{(s.totalCurrentValue || 0).toLocaleString()}</div>
                                            <div style={{ fontSize: '0.8rem', color: s.totalProfitLoss >= 0 ? 'var(--success)' : 'var(--danger)', marginTop: '4px', fontWeight: '600' }}>
                                                {s.totalProfitLoss >= 0 ? '+' : ''}₩{(s.totalProfitLoss || 0).toLocaleString()}
                                            </div>
                                        </td>
                                        <td style={{ padding: '20px 24px', textAlign: 'right' }}>
                                            <div style={{ fontWeight: '600' }}>₩{(s.totalExpectedDividend || 0).toLocaleString()}</div>
                                            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                                                연 {((s.totalExpectedDividend || 0) / (s.totalCurrentValue || 1) * 100).toFixed(2)}%
                                            </div>
                                        </td>
                                        <td style={{ padding: '20px 24px', textAlign: 'right' }}>
                                            <span style={{
                                                fontWeight: '800', fontSize: '1.1rem',
                                                color: s.totalReturnRate >= 0 ? 'var(--success)' : 'var(--danger)'
                                            }}>
                                                {s.totalReturnRate >= 0 ? '+' : ''}{(s.totalReturnRate || 0).toFixed(2)}%
                                            </span>
                                        </td>
                                        <td style={{ padding: '20px 24px', textAlign: 'right' }}>
                                            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' }}>
                                                <span style={{ fontWeight: '700', fontSize: '0.95rem' }}>{weight.toFixed(1)}%</span>
                                                <div style={{ width: '80px', height: '6px', background: 'rgba(255,255,255,0.05)', borderRadius: '100px', overflow: 'hidden' }}>
                                                    <div style={{ width: `${weight}%`, height: '100%', background: 'var(--primary)', boxShadow: '0 0 10px var(--primary)' }} />
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                        <tfoot style={{ background: 'rgba(255,255,255,0.02)', borderTop: '2px solid var(--border)' }}>
                            <tr style={{ fontWeight: '900' }}>
                                <td colSpan="2" style={{ padding: '24px', fontSize: '1.1rem' }}>통합 요약 (Filtered Total)</td>
                                <td style={{ padding: '24px', textAlign: 'right', fontSize: '1.2rem' }}>₩{totalAssets.toLocaleString()}</td>
                                <td style={{ padding: '24px', textAlign: 'right', fontSize: '1.2rem' }}>₩{totalDividend.toLocaleString()}</td>
                                <td style={{ padding: '24px', textAlign: 'right', color: totalProfitLoss >= 0 ? 'var(--success)' : 'var(--danger)', fontSize: '1.2rem' }}>
                                    {totalProfitLoss >= 0 ? '+' : ''}{totalReturnRate.toFixed(2)}%
                                </td>
                                <td style={{ padding: '24px', textAlign: 'right', fontSize: '1.2rem' }}>100.0%</td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default Overview;
