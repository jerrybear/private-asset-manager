import React, { useState, useEffect, useCallback } from 'react';
import { LayoutDashboard, TrendingUp, TrendingDown, Wallet, PieChart as PieChartIcon, Calendar, Plus, PlusCircle, List as ListIcon, Trash2, RefreshCcw, CheckCircle, XCircle, Upload, Settings, RefreshCw, RotateCw, History, Edit2 } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import AssetInputModal from './AssetInputModal';
import api from '../api/client';
import AccountSelector from './AccountSelector';
import AccountModal from './AccountModal';

const Dashboard = () => {
    const [summary, setSummary] = useState(null);
    const [accounts, setAccounts] = useState([]);
    const [currentAccountId, setCurrentAccountId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isAccountModalOpen, setIsAccountModalOpen] = useState(false);
    const [isAssetModalOpen, setIsAssetModalOpen] = useState(false);
    const [editingAccount, setEditingAccount] = useState(null);
    const [editingAsset, setEditingAsset] = useState(null); // New state for editing asset
    const [isSyncing, setIsSyncing] = useState(false); // Renamed from syncingAccountId for clarity with existing logic
    const [refreshingAll, setRefreshingAll] = useState(false); // New state for refreshing all prices
    const [refreshingAssetIds, setRefreshingAssetIds] = useState([]); // New state for refreshing individual asset prices (multiple)
    const [isExporting, setIsExporting] = useState(false);
    const [isInitialSyncing, setIsInitialSyncing] = useState(false);
    const [news, setNews] = useState([]);
    const [loadingNews, setLoadingNews] = useState(false);
    const [highlightedAsset, setHighlightedAsset] = useState(null);
    const [lastNewsFetchedTime, setLastNewsFetchedTime] = useState(null);
    const [toast, setToast] = useState({ show: false, message: '', type: 'success' });

    // 인라인 편집 관련 상태
    const [isInlineEditEnabled, setIsInlineEditEnabled] = useState(true);
    const [editingCell, setEditingCell] = useState(null); // { id, field }
    const [editValues, setEditValues] = useState({});

    const showToast = (message, type = 'success') => {
        setToast({ show: true, message, type });
        setTimeout(() => setToast({ ...toast, show: false }), 3000);
    };

    const handleSync = async () => {
        if (!currentAccountId) return;
        try {
            setIsSyncing(true);
            await api.post(`/accounts/${currentAccountId}/sync`);
            await fetchAccounts();
            await fetchSummary();
            showToast('구글 시트와 동기화되었습니다.');
        } catch (error) {
            console.error('Error syncing with Google Sheets:', error);
            const msg = error.response?.data?.message || error.message || '동기화 중 오류가 발생했습니다.';
            showToast(msg, 'error');
        } finally {
            setIsSyncing(false);
        }
    };

    const handleExport = async () => {
        if (!currentAccountId) return;
        try {
            setIsExporting(true);
            await api.post(`/accounts/${currentAccountId}/export`);
            showToast('데이터가 구글 시트로 내보내졌습니다.');
        } catch (error) {
            console.error('Error exporting to Google Sheets:', error);
            const msg = error.response?.data?.message || error.message || '내보내기 중 오류가 발생했습니다.';
            showToast(msg, 'error');
        } finally {
            setIsExporting(false);
        }
    };

    const handleRefreshAllPrices = async (accountId) => {
        if (!accountId || !summary?.assets) return;

        const assetsToRefresh = summary.assets.filter(a => a.code?.startsWith('KRX:'));
        if (assetsToRefresh.length === 0) return;

        try {
            setRefreshingAll(true);

            // 모든 요청을 동시에 시작
            const refreshPromises = assetsToRefresh.map(async (asset) => {
                try {
                    setRefreshingAssetIds(prev => [...prev, asset.id]);
                    const response = await api.post(`/accounts/${accountId}/assets/${asset.id}/refresh-price?force=true`);
                    const { newPrice } = response.data;

                    if (newPrice !== undefined) {
                        // 로컬 상태 즉시 업데이트
                        setSummary(prevSummary => {
                            if (!prevSummary) return null;
                            const updatedAssets = prevSummary.assets.map(a => {
                                if (a.id === asset.id) {
                                    const quantity = a.quantity || 0;
                                    const purchasePrice = a.averagePurchasePrice || 0;
                                    const purchaseAmount = quantity * purchasePrice;
                                    const currentValue = quantity * newPrice;
                                    const profitLoss = currentValue - purchaseAmount;
                                    const returnRate = purchaseAmount === 0 ? 0 : parseFloat(((profitLoss / purchaseAmount) * 100).toFixed(2));

                                    return {
                                        ...a,
                                        currentPrice: newPrice,
                                        currentValue: currentValue,
                                        profitLoss: profitLoss,
                                        returnRate: returnRate,
                                        lastPriceUpdate: new Date().toISOString()
                                    };
                                }
                                return a;
                            });
                            return { ...prevSummary, assets: updatedAssets };
                        });
                    }
                } catch (err) {
                    console.warn(`Failed to refresh price for asset ${asset.id}:`, err);
                } finally {
                    setRefreshingAssetIds(prev => prev.filter(id => id !== asset.id));
                }
            });

            await Promise.all(refreshPromises);

            // 모든 조회가 끝난 후 최종 요약 정보 재동기화 (서버측 계산 결과 합계 등 반영)
            await fetchSummary();
            showToast('모든 자산 시세가 업데이트되었습니다.');
        } catch (error) {
            console.error('Error refreshing all prices:', error);
            showToast('일부 시세 업데이트 중 오류가 발생했습니다.', 'error');
        } finally {
            setRefreshingAll(false);
        }
    };

    const handleRefreshAssetPrice = async (accountId, assetId) => {
        if (!accountId || !assetId) return;
        try {
            setRefreshingAssetIds(prev => [...prev, assetId]);
            const response = await api.post(`/accounts/${accountId}/assets/${assetId}/refresh-price?force=true`);
            const { newPrice } = response.data;

            if (newPrice !== undefined) {
                setSummary(prevSummary => {
                    if (!prevSummary) return null;
                    const updatedAssets = prevSummary.assets.map(a => {
                        if (a.id === assetId) {
                            const quantity = a.quantity || 0;
                            const purchasePrice = a.averagePurchasePrice || 0;
                            const purchaseAmount = quantity * purchasePrice;
                            const currentValue = quantity * newPrice;
                            const profitLoss = currentValue - purchaseAmount;
                            const returnRate = purchaseAmount === 0 ? 0 : parseFloat(((profitLoss / purchaseAmount) * 100).toFixed(2));

                            return {
                                ...a,
                                currentPrice: newPrice,
                                currentValue: currentValue,
                                profitLoss: profitLoss,
                                returnRate: returnRate,
                                lastPriceUpdate: new Date().toISOString()
                            };
                        }
                        return a;
                    });
                    return { ...prevSummary, assets: updatedAssets };
                });
            }
            showToast('자산 시세가 업데이트되었습니다.');
        } catch (error) {
            console.error('Error refreshing asset price:', error);
            showToast('자산 시세 업데이트 중 오류가 발생했습니다.', 'error');
        } finally {
            setRefreshingAssetIds(prev => prev.filter(id => id !== assetId));
        }
    };

    const fetchAccounts = useCallback(async () => {
        try {
            const response = await api.get('/accounts');
            setAccounts(response.data);
            if (response.data.length > 0 && !currentAccountId) {
                setCurrentAccountId(response.data[0].id);
            }
        } catch (error) {
            console.error('Error fetching accounts:', error);
        }
    }, [currentAccountId]);

    const fetchSummary = useCallback(async () => {
        if (!currentAccountId) return;
        try {
            setLoading(true);
            const response = await api.get(`/accounts/${currentAccountId}/summary`);
            setSummary(response.data);
        } catch (error) {
            console.error('Error fetching summary:', error);
        } finally {
            setLoading(false);
        }
    }, [currentAccountId]);

    const fetchNews = useCallback(async (force = false) => {
        if (!summary?.assets || summary.assets.length === 0) {
            setNews([]);
            return;
        }

        const stockAssets = summary.assets.filter(a =>
            a.type === 'STOCK' || a.type === 'STOCK_KR' || a.type === 'STOCK_US' ||
            a.type === 'ETF_KR' || a.type === 'REITS' || a.type === 'COMMODITY' || a.type === 'GOLD_SPOT'
        );

        if (stockAssets.length === 0) {
            setNews([]);
            return;
        }

        // 최대 10개 종목 이름으로 검색 키워드 구성
        const keywords = stockAssets.slice(0, 10).map(a => a.name);

        try {
            setLoadingNews(true);
            const params = new URLSearchParams();
            keywords.forEach(kw => params.append('keywords', kw));
            if (force) params.append('force', 'true');

            const response = await api.get(`/news/search?${params.toString()}`);
            setNews(response.data);
            setLastNewsFetchedTime(new Date().toLocaleTimeString('ko-KR', { hour12: false }));
            if (force) showToast('뉴스가 새로고침되었습니다.');
        } catch (error) {
            console.error('Error fetching news:', error);
            if (force) showToast('뉴스 새로고침 중 오류가 발생했습니다.', 'error');
        } finally {
            setLoadingNews(false);
        }
    }, [summary?.assets?.map(a => a.name).join(',')]);

    const handleClickAssetName = (assetName) => {
        setHighlightedAsset(assetName);
        const newsSection = document.getElementById('news-section');
        if (newsSection) {
            newsSection.scrollIntoView({ behavior: 'smooth' });
        }
        setTimeout(() => {
            setHighlightedAsset(null);
        }, 3000);
    };

    useEffect(() => {
        fetchAccounts();
    }, [fetchAccounts]);

    useEffect(() => {
        fetchSummary();
    }, [fetchSummary]);

    useEffect(() => {
        fetchNews();
    }, [fetchNews]);

    const attemptedAccounts = React.useRef(new Set());

    useEffect(() => {
        // summary가 현재 선택된 계좌와 일치하는지 먼저 확인
        if (currentAccountId && summary && summary.accountId === currentAccountId) {

            // 이미 이 세션에서 자동 조회를 시도한 계좌라면 중복 실행 방지
            if (attemptedAccounts.current.has(currentAccountId)) {
                return;
            }

            // 시세 정보가 한 번도 조회된 적 없는 자산(KRX 대상)이 있는지 확인
            const hasMissingPrice = summary.assets?.some(asset =>
                asset.code?.startsWith('KRX:') && !asset.lastPriceUpdate
            );

            if (hasMissingPrice) {
                handleRefreshAllPrices(currentAccountId);
            }

            // 시도 여부 기록 (성공/실패 여부와 무관하게 한 번 로드 시 한 번만 시도)
            attemptedAccounts.current.add(currentAccountId);
        }
    }, [currentAccountId, summary, handleRefreshAllPrices]);


    useEffect(() => {
        let interval;
        const checkSyncStatus = async () => {
            try {
                const response = await api.get('/accounts/sync-status');
                const syncing = response.data.isInitialSyncing;
                setIsInitialSyncing(syncing);
                if (!syncing && interval) {
                    clearInterval(interval);
                    fetchAccounts();
                    fetchSummary();
                }
            } catch (error) {
                console.error('Error checking sync status:', error);
            }
        };

        checkSyncStatus();
        interval = setInterval(checkSyncStatus, 2000);

        return () => clearInterval(interval);
    }, [fetchAccounts, fetchSummary]);

    const handleAccountCreated = async (account, isUpdate = false) => {
        await fetchAccounts();
        if (account && account.id) {
            setCurrentAccountId(account.id);
            showToast(`${account.name} 계좌가 ${isUpdate ? '수정' : '생성'}되었습니다.`);
        }
        setEditingAccount(null);
    };

    const handleAccountDelete = async (id) => {
        if (window.confirm('정말 이 계좌를 삭제하시겠습니까? 관련 자산 정보가 모두 삭제됩니다.')) {
            try {
                await api.delete(`/accounts/${id}`);
                if (currentAccountId === id) {
                    setCurrentAccountId(null);
                }
                fetchAccounts();
                showToast('계좌가 삭제되었습니다.', 'success');
            } catch (error) {
                console.error('Error deleting account:', error);
                showToast('계좌 삭제 중 오류가 발생했습니다.', 'error');
            }
        }
    };

    const handleAssetUpdate = async (assetId, field, value) => {
        try {
            const asset = summary?.assets?.find(a => a.id === assetId);
            if (!asset) return;

            const numValue = parseFloat(value);
            if (isNaN(numValue)) return;

            const updatedAsset = {
                ...asset,
                [field]: numValue
            };

            await api.put(`/accounts/${currentAccountId}/assets/${assetId}`, updatedAsset);
            setEditingCell(null);
            fetchSummary();
            showToast('자산 정보가 수정되었습니다.');
        } catch (error) {
            console.error('Error updating asset:', error);
            showToast('수정 중 오류가 발생했습니다.', 'error');
        }
    };

    const handleAssetAdded = () => {
        fetchSummary();
        showToast('새 자산이 추가되었습니다.');
    };

    const handleDeleteAsset = async (accountId, assetId) => {
        if (window.confirm('정말 이 자산을 삭제하시겠습니까?')) {
            try {
                await api.delete(`/accounts/${accountId}/assets/${assetId}`);
                fetchSummary();
                showToast('자산이 삭제되었습니다.', 'success');
            } catch (error) {
                console.error('Error deleting asset:', error);
                showToast('자산 삭제 중 오류가 발생했습니다.', 'error');
            }
        }
    };

    if (loading && !summary) return <div style={{ padding: '24px', color: 'var(--text-muted)' }}>로딩 중...</div>;

    const TYPE_LABELS = {
        STOCK: '주식',
        STOCK_KR: '국내주식',
        STOCK_US: '해외주식',
        ETF_KR: '국내ETF',
        CRYPTO: '가상화폐',
        CASH: '현금',
        RP: 'RP',
        ISSUED_NOTE: '발행어음',
        BOND: '채권',
        BOND_KR: '국내채권',
        BOND_US: '해외채권',
        REITS: '리츠',
        COMMODITY: '원자재',
        DEPOSIT_SAVINGS: '예적금',
        GOLD_SPOT: '금현물'
    };

    const chartData = summary?.assets?.reduce((acc, asset) => {
        const typeLabel = TYPE_LABELS[asset.type] || '기타';
        const existing = acc.find(item => item.name === typeLabel);
        const value = asset.currentValue || 0;
        if (existing) {
            existing.value += value;
        } else {
            acc.push({ name: typeLabel, value: value });
        }
        return acc;
    }, []) || [];

    const totalValue = chartData.reduce((sum, item) => sum + item.value, 0);
    const formattedChartData = chartData.map(item => ({
        ...item,
        value: totalValue > 0 ? Math.round((item.value / totalValue) * 100) : 0
    }));

    const COLORS = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#ef4444', '#64748b'];

    const currentAccount = accounts.find(a => a.id === currentAccountId);

    return (
        <div className="animate-fade-in" style={{ padding: '32px', maxWidth: '1200px', margin: '0 auto' }}>
            {isInitialSyncing && (
                <div style={{
                    position: 'fixed', bottom: '32px', right: '32px',
                    padding: '16px 24px', borderRadius: '16px',
                    backgroundColor: 'rgba(59, 130, 246, 0.9)',
                    backdropFilter: 'blur(8px)',
                    color: 'white', display: 'flex', alignItems: 'center', gap: '12px',
                    boxShadow: '0 10px 40px -10px rgba(0, 0, 0, 0.5)',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    zIndex: 2000,
                    animation: 'pulse 2s infinite'
                }}>
                    <RefreshCcw size={18} className="animate-spin" />
                    <span style={{ fontWeight: '600', fontSize: '0.95rem' }}>구글 시트와 초기 동기화 중...</span>
                </div>
            )}
            <header style={{ marginBottom: '32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                    <div>
                        <h1 style={{ fontSize: '2rem', fontWeight: 'bold' }}>Dashboard</h1>
                        <p style={{ color: 'var(--text-muted)', marginTop: '4px' }}>실시간 자산 현황 및 수익률 분석</p>
                    </div>
                    <AccountSelector
                        accounts={accounts}
                        currentAccountId={currentAccountId}
                        onSelect={setCurrentAccountId}
                        onNew={() => setIsAccountModalOpen(true)}
                        onDelete={handleAccountDelete}
                    />
                </div>
                <div
                    onClick={() => setIsInlineEditEnabled(!isInlineEditEnabled)}
                    style={{
                        display: 'flex', alignItems: 'center', gap: '8px',
                        color: isInlineEditEnabled ? 'var(--primary)' : 'var(--text-muted)',
                        fontSize: '0.875rem', cursor: 'pointer',
                        padding: '6px 12px', borderRadius: '8px',
                        background: isInlineEditEnabled ? 'rgba(99, 102, 241, 0.1)' : 'rgba(255, 255, 255, 0.05)',
                        transition: 'all 0.2s ease',
                        border: `1px solid ${isInlineEditEnabled ? 'rgba(99, 102, 241, 0.2)' : 'transparent'}`
                    }}
                    className="hover-lift"
                    title={isInlineEditEnabled ? "편집 모드 비활성화" : "편집 모드 활성화"}
                >
                    <RefreshCcw size={14} className={isInlineEditEnabled ? 'animate-spin-slow' : ''} />
                    <span style={{ fontWeight: '600' }}>
                        실시간 편집 {isInlineEditEnabled ? '모드 활성화됨' : 'Off'}
                    </span>
                </div>
            </header>

            {!currentAccountId ? (
                <div className="glass-card" style={{ textAlign: 'center', padding: '64px' }}>
                    <h2 style={{ marginBottom: '16px' }}>계좌를 선택하거나 새로 생성해주세요.</h2>
                    <button className="btn-primary" onClick={() => setIsAccountModalOpen(true)}>
                        <Plus size={18} /> 새 계좌 생성
                    </button>
                </div>
            ) : (
                <div className="grid-dashboard">
                    {/* 1. 총 자산 평가액 카드 (계산 결과) */}
                    <div className="glass-card">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                            <TrendingUp size={20} color="var(--primary)" />
                            <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: '600' }}>총 자산 평가액</span>
                        </div>
                        <div style={{ fontSize: '1.75rem', fontWeight: '800' }}>
                            ₩{summary?.totalCurrentValue?.toLocaleString() || '0'}
                        </div>
                        <div style={{
                            color: summary?.totalProfitLoss >= 0 ? 'var(--success)' : 'var(--danger)',
                            marginTop: '12px',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            fontSize: '0.9rem',
                            fontWeight: '600'
                        }}>
                            {summary?.totalProfitLoss >= 0 ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
                            {summary?.totalProfitLoss >= 0 ? '+' : ''}₩{summary?.totalProfitLoss?.toLocaleString() || '0'}
                            ({summary?.totalReturnRate || '0'}%)
                        </div>
                    </div>

                    {/* 2. 예상 연간 배당금 카드 */}
                    <div className="glass-card">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                            <Calendar size={20} color="var(--accent)" />
                            <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: '600' }}>예상 연간 배당금</span>
                        </div>
                        <div style={{ fontSize: '1.75rem', fontWeight: '800' }}>
                            ₩{summary?.totalExpectedDividend?.toLocaleString() || '0'}
                        </div>
                        <div style={{ color: 'var(--text-muted)', marginTop: '12px', fontSize: '0.9rem' }}>
                            월 평균 약 ₩{(summary?.totalExpectedDividend / 12 || 0).toLocaleString(undefined, { maximumFractionDigits: 0 })}
                        </div>
                    </div>

                    {/* 3. 자산 비중 카드 (그리드 유지) */}
                    <div className="glass-card" style={{ gridRow: 'span 2' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
                            <PieChartIcon size={20} color="var(--primary)" />
                            <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: '600' }}>자산 비중</span>
                        </div>
                        <div style={{ height: '220px' }}>
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie data={formattedChartData} innerRadius={60} outerRadius={85} paddingAngle={8} dataKey="value" animationBegin={0} animationDuration={1000}>
                                        {formattedChartData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="none" />
                                        ))}
                                    </Pie>
                                    <Tooltip
                                        contentStyle={{ backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '8px' }}
                                        itemStyle={{ color: 'var(--text-main)' }}
                                    />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                        <div style={{ marginTop: '24px' }}>
                            {formattedChartData.map((item, idx) => (
                                <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', alignItems: 'center' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <div style={{ width: '10px', height: '10px', borderRadius: '2px', backgroundColor: COLORS[idx] }} />
                                        <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>{item.name}</span>
                                    </div>
                                    <span style={{ fontWeight: '700', fontSize: '0.875rem' }}>{item.value}%</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* 4. 계좌 및 자산 통합 관리 카드 (Span 2) */}
                    <div className="glass-card" style={{ gridColumn: 'span 2', display: 'flex', flexDirection: 'column', gap: '32px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '24px', flexWrap: 'wrap' }}>
                            {/* 좌측: 계좌 기본 정보 */}
                            <div style={{ flex: '1 1 300px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                                    <h2 style={{ fontSize: '1.75rem', fontWeight: '800', letterSpacing: '-0.025em' }}>{summary?.accountName}</h2>
                                    <span style={{
                                        fontSize: '0.75rem', padding: '4px 10px', borderRadius: '100px',
                                        backgroundColor: 'rgba(99, 102, 241, 0.15)', color: 'var(--primary)',
                                        fontWeight: '700', border: '1px solid rgba(99, 102, 241, 0.3)',
                                        textTransform: 'uppercase'
                                    }}>
                                        {summary?.accountType === 'REGULAR' ? '일반계좌' :
                                            summary?.accountType === 'PENSION' ? '연금계좌' :
                                                summary?.accountType === 'ISA' ? 'ISA' :
                                                    summary?.accountType === 'IRP' ? 'IRP' : '기타특수'}
                                    </span>
                                </div>
                                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(255,255,255,0.03)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border)', fontSize: '0.85rem' }}>
                                        <Wallet size={14} color="var(--text-muted)" />
                                        <span style={{ color: 'var(--text-muted)' }}>기관</span>
                                        <span style={{ fontWeight: '600' }}>{summary?.financialInstitution}</span>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(255,255,255,0.03)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border)', fontSize: '0.85rem' }}>
                                        <History size={14} color="var(--text-muted)" />
                                        <span style={{ color: 'var(--text-muted)' }}>번호</span>
                                        <span style={{ fontWeight: '600', fontVariantNumeric: 'tabular-nums' }}>{summary?.accountNumber}</span>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(255,255,255,0.03)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border)', fontSize: '0.85rem' }}>
                                        <Settings size={14} color="var(--text-muted)" />
                                        <span style={{ color: 'var(--text-muted)' }}>소유자</span>
                                        <span style={{ fontWeight: '600' }}>{summary?.owner}</span>
                                    </div>
                                </div>
                            </div>

                            {/* 우측: 액션 버튼 그룹 */}
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center', justifyContent: 'flex-end' }}>
                                {/* Primary Group: 자산 관리 */}
                                <div style={{ display: 'flex', gap: '8px', padding: '4px', background: 'rgba(255,255,255,0.03)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                                    <button
                                        onClick={() => setIsAssetModalOpen(true)}
                                        className="btn-primary hover-lift"
                                        style={{ padding: '8px 16px', borderRadius: '8px', fontSize: '0.9rem' }}
                                    >
                                        <PlusCircle size={18} />
                                        자산 추가
                                    </button>
                                    <button
                                        onClick={() => {
                                            setEditingAccount(currentAccount);
                                            setIsAccountModalOpen(true);
                                        }}
                                        className="btn-secondary hover-lift"
                                        style={{
                                            background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border)',
                                            color: 'var(--text-main)', padding: '8px 12px', borderRadius: '8px'
                                        }}
                                        title="계좌 설정"
                                    >
                                        <Settings size={18} />
                                    </button>
                                </div>

                                {/* Secondary Group: 데이터 연동 */}
                                <div style={{ display: 'flex', gap: '8px', padding: '4px', background: 'rgba(255,255,255,0.03)', borderRadius: '12px', border: '1px solid var(--border)' }}>
                                    <button
                                        onClick={() => handleRefreshAllPrices(currentAccountId)}
                                        disabled={refreshingAll}
                                        className="hover-lift"
                                        style={{
                                            display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 14px',
                                            borderRadius: '8px', background: 'transparent', border: 'none',
                                            color: 'var(--primary)', fontSize: '0.85rem', fontWeight: '700', cursor: 'pointer',
                                            transition: 'all 0.2s ease', opacity: refreshingAll ? 0.6 : 1
                                        }}
                                    >
                                        <RotateCw size={16} className={refreshingAll ? 'animate-spin' : ''} />
                                        <span>시세 조회</span>
                                    </button>
                                    <div style={{ width: '1px', height: '16px', background: 'var(--border)', alignSelf: 'center' }} />
                                    <button
                                        onClick={handleExport}
                                        disabled={isExporting}
                                        className="hover-lift"
                                        style={{
                                            display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 14px',
                                            borderRadius: '8px', background: 'transparent', border: 'none',
                                            color: '#38bdf8', fontSize: '0.85rem', fontWeight: '700', cursor: 'pointer',
                                            opacity: isExporting ? 0.6 : 1
                                        }}
                                    >
                                        <Upload size={16} className={isExporting ? 'animate-pulse' : ''} />
                                        <span>내보내기</span>
                                    </button>
                                    <div style={{ width: '1px', height: '16px', background: 'var(--border)', alignSelf: 'center' }} />
                                    <button
                                        onClick={handleSync}
                                        disabled={isSyncing}
                                        className="hover-lift"
                                        style={{
                                            display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 14px',
                                            borderRadius: '8px', background: 'transparent', border: 'none',
                                            color: '#2dd4bf', fontSize: '0.85rem', fontWeight: '700', cursor: 'pointer',
                                            opacity: isSyncing ? 0.6 : 1
                                        }}
                                    >
                                        <RefreshCcw size={16} className={isSyncing ? 'animate-spin' : ''} />
                                        <span>가져오기</span>
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div style={{ overflowX: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                                <thead>
                                    <tr style={{ color: 'var(--text-muted)', fontSize: '0.8rem', borderBottom: '1px solid var(--border)' }}>
                                        <th style={{ padding: '12px 8px', width: '25%' }}>종목명</th>
                                        <th style={{ padding: '12px 8px', width: '12%' }}>유형</th>
                                        <th style={{ padding: '12px 8px', textAlign: 'right', width: '15%' }}>보유량</th>
                                        <th style={{ padding: '12px 8px', textAlign: 'right', width: '15%' }}>평단가</th>
                                        <th style={{ padding: '12px 8px', textAlign: 'right', width: '15%' }}>현재가</th>
                                        <th style={{ padding: '12px 8px', textAlign: 'right', width: '10%' }}>수익률</th>
                                        <th style={{ padding: '12px 8px', textAlign: 'center', width: '8%' }}>관리</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {summary?.assets?.map((asset, index) => (
                                        <tr key={asset.id || index} style={{ borderBottom: index === summary.assets.length - 1 ? 'none' : '1px solid var(--border-light)', fontSize: '0.9rem' }}>
                                            <td style={{ padding: '16px 8px' }}>
                                                <div
                                                    style={{ fontWeight: '600', cursor: 'pointer', color: 'var(--text-main)' }}
                                                    onClick={() => handleClickAssetName(asset.name)}
                                                    className="hover:text-primary transition-colors"
                                                >
                                                    {asset.name}
                                                </div>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{asset.code}</div>
                                            </td>
                                            <td style={{ padding: '16px 8px' }}>
                                                <span style={{
                                                    fontSize: '0.75rem', padding: '2px 6px', borderRadius: '4px',
                                                    backgroundColor: (asset.type === 'STOCK' || asset.type === 'STOCK_KR' || asset.type === 'STOCK_US') ? 'rgba(99, 102, 241, 0.1)' :
                                                        asset.type === 'CASH' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(139, 92, 246, 0.1)',
                                                    color: (asset.type === 'STOCK' || asset.type === 'STOCK_KR' || asset.type === 'STOCK_US') ? 'var(--primary)' :
                                                        asset.type === 'CASH' ? '#10b981' : 'var(--accent)'
                                                }}>
                                                    {TYPE_LABELS[asset.type] || asset.type}
                                                </span>
                                            </td>
                                            <td style={{ padding: '16px 8px', textAlign: 'right' }}>
                                                {editingCell?.id === asset.id && editingCell?.field === 'quantity' ? (
                                                    <input
                                                        autoFocus
                                                        type="number"
                                                        step="any"
                                                        value={editValues[asset.id]?.quantity ?? asset.quantity}
                                                        onChange={(e) => setEditValues({ ...editValues, [asset.id]: { ...editValues[asset.id], quantity: e.target.value } })}
                                                        onBlur={() => handleAssetUpdate(asset.id, 'quantity', editValues[asset.id]?.quantity)}
                                                        onKeyDown={(e) => e.key === 'Enter' && handleAssetUpdate(asset.id, 'quantity', editValues[asset.id]?.quantity)}
                                                        className="inline-input"
                                                        style={{ width: '80px', textAlign: 'right' }}
                                                    />
                                                ) : (
                                                    <span
                                                        onClick={() => {
                                                            if (!isInlineEditEnabled) return;
                                                            setEditingCell({ id: asset.id, field: 'quantity' });
                                                            setEditValues({ ...editValues, [asset.id]: { ...editValues[asset.id], quantity: asset.quantity } });
                                                        }}
                                                        style={{
                                                            cursor: isInlineEditEnabled ? 'pointer' : 'default',
                                                            borderBottom: isInlineEditEnabled ? '1px dashed var(--text-muted)' : 'none'
                                                        }}
                                                    >
                                                        {asset.quantity}
                                                    </span>
                                                )}
                                            </td>
                                            <td style={{ padding: '16px 8px', textAlign: 'right' }}>
                                                {editingCell?.id === asset.id && editingCell?.field === 'averagePurchasePrice' ? (
                                                    <input
                                                        autoFocus
                                                        type="number"
                                                        step="any"
                                                        value={editValues[asset.id]?.averagePurchasePrice ?? asset.averagePurchasePrice}
                                                        onChange={(e) => setEditValues({ ...editValues, [asset.id]: { ...editValues[asset.id], averagePurchasePrice: e.target.value } })}
                                                        onBlur={() => handleAssetUpdate(asset.id, 'averagePurchasePrice', editValues[asset.id]?.averagePurchasePrice)}
                                                        onKeyDown={(e) => e.key === 'Enter' && handleAssetUpdate(asset.id, 'averagePurchasePrice', editValues[asset.id]?.averagePurchasePrice)}
                                                        className="inline-input"
                                                        style={{ width: '100px', textAlign: 'right' }}
                                                    />
                                                ) : (
                                                    <span
                                                        onClick={() => {
                                                            if (!isInlineEditEnabled) return;
                                                            setEditingCell({ id: asset.id, field: 'averagePurchasePrice' });
                                                            setEditValues({ ...editValues, [asset.id]: { ...editValues[asset.id], averagePurchasePrice: asset.averagePurchasePrice } });
                                                        }}
                                                        style={{
                                                            cursor: isInlineEditEnabled ? 'pointer' : 'default',
                                                            borderBottom: isInlineEditEnabled ? '1px dashed var(--text-muted)' : 'none'
                                                        }}
                                                    >
                                                        ₩{asset.averagePurchasePrice?.toLocaleString()}
                                                    </span>
                                                )}
                                            </td>
                                            <td style={{ padding: '16px 8px', textAlign: 'right' }}>₩{asset.currentPrice?.toLocaleString()}</td>
                                            <td style={{
                                                padding: '16px 8px', textAlign: 'right', fontWeight: '700',
                                                color: asset.returnRate >= 0 ? 'var(--success)' : 'var(--danger)'
                                            }}>
                                                {asset.returnRate >= 0 ? '+' : ''}{asset.returnRate}%
                                            </td>
                                            <td style={{ padding: '16px 8px', textAlign: 'center' }}>
                                                <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                                                    <button
                                                        onClick={() => handleRefreshAssetPrice(currentAccountId, asset.id)}
                                                        disabled={refreshingAssetIds.includes(asset.id) || refreshingAll}
                                                        style={{
                                                            background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '4px',
                                                            transition: 'all 0.2s',
                                                            opacity: (refreshingAssetIds.includes(asset.id) || (refreshingAll && asset.code?.startsWith('KRX:'))) ? 0.6 : 1
                                                        }}
                                                        className="hover:text-primary"
                                                        title="시세 조회"
                                                    >
                                                        <RefreshCw
                                                            size={14}
                                                            className={(refreshingAssetIds.includes(asset.id) || (refreshingAll && asset.code?.startsWith('KRX:'))) ? 'animate-spin' : ''}
                                                        />
                                                    </button>
                                                    <button onClick={() => handleDeleteAsset(currentAccountId, asset.id)} style={{
                                                        background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '4px'
                                                    }} className="hover:text-danger">
                                                        <Trash2 size={14} />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* 뉴스 섹션 - 그리드 내부로 이동하여 전체 너비로 확장 */}
                    <div id="news-section" className="glass-card" style={{ gridColumn: '1 / -1', marginTop: '8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                <ListIcon size={20} color="var(--primary)" />
                                <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                                    <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: '600' }}>종목 관련 소식</span>
                                    {lastNewsFetchedTime && (
                                        <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem', fontWeight: '400', opacity: 0.7 }}>
                                            (마지막 업데이트: {lastNewsFetchedTime})
                                        </span>
                                    )}
                                </div>
                            </div>
                            <button
                                onClick={() => fetchNews(true)}
                                disabled={loadingNews}
                                className="hover-lift"
                                style={{
                                    display: 'flex', alignItems: 'center', gap: '6px',
                                    padding: '6px 12px', borderRadius: '8px',
                                    background: 'rgba(99, 102, 241, 0.1)', border: '1px solid rgba(99, 102, 241, 0.2)',
                                    color: 'var(--primary)', fontSize: '0.8rem', fontWeight: '700', cursor: 'pointer',
                                    transition: 'all 0.2s ease', opacity: loadingNews ? 0.6 : 1
                                }}
                            >
                                <RefreshCw size={14} className={loadingNews ? 'animate-spin' : ''} />
                                <span>새로고침</span>
                            </button>
                        </div>

                        {loadingNews ? (
                            <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
                                <RefreshCcw size={20} className="animate-spin" style={{ margin: '0 auto 12px' }} />
                                <p>뉴스를 불러오는 중입니다...</p>
                            </div>
                        ) : news.length > 0 ? (
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
                                {news.map((item, idx) => (
                                    <a
                                        key={idx}
                                        href={item.link}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className={`hover-lift ${highlightedAsset === item.relatedAsset ? 'highlight-pulse' : ''}`}
                                        style={{
                                            display: 'flex', flexDirection: 'column', gap: '8px', padding: '16px',
                                            borderRadius: '12px', background: 'rgba(255,255,255,0.02)',
                                            border: highlightedAsset === item.relatedAsset ? '2px solid var(--primary)' : '1px solid var(--border)',
                                            textDecoration: 'none',
                                            transition: 'all 0.3s ease',
                                            boxShadow: highlightedAsset === item.relatedAsset ? '0 0 20px rgba(99, 102, 241, 0.3)' : 'none',
                                            transform: highlightedAsset === item.relatedAsset ? 'scale(1.02)' : 'none'
                                        }}
                                        onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.05)'}
                                        onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.02)'}
                                    >
                                        {item.relatedAsset && (
                                            <div style={{
                                                alignSelf: 'flex-start',
                                                fontSize: '0.7rem',
                                                fontWeight: '800',
                                                color: 'white',
                                                backgroundColor: 'var(--primary)',
                                                padding: '2px 8px',
                                                borderRadius: '4px',
                                                marginBottom: '8px',
                                                textTransform: 'uppercase'
                                            }}>
                                                {item.relatedAsset}
                                            </div>
                                        )}
                                        <div style={{
                                            fontSize: '0.95rem', fontWeight: '600', color: 'var(--text-main)',
                                            lineHeight: '1.5', overflow: 'hidden', display: '-webkit-box',
                                            WebkitLineClamp: 2, WebkitBoxOrient: 'vertical'
                                        }}>
                                            {item.title.split(' - ')[0]}
                                        </div>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 'auto' }}>
                                            <span style={{ fontSize: '0.75rem', color: 'var(--primary)', fontWeight: '600' }}>
                                                {item.title.split(' - ').length > 1 ? item.title.split(' - ').pop() : '뉴스'}
                                            </span>
                                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                                {new Date(item.pubDate).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                                            </span>
                                        </div>
                                    </a>
                                ))}
                            </div>
                        ) : (
                            <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
                                보유 종목과 관련된 최신 뉴스가 없습니다.
                            </div>
                        )}
                    </div>
                </div>
            )}

            <AccountModal
                isOpen={isAccountModalOpen}
                onClose={() => {
                    setIsAccountModalOpen(false);
                    setEditingAccount(null);
                }}
                onAccountCreated={handleAccountCreated}
                initialAccount={editingAccount}
            />

            <AssetInputModal
                isOpen={isAssetModalOpen}
                onClose={() => setIsAssetModalOpen(false)}
                accountId={currentAccountId}
                onAssetAdded={handleAssetAdded}
            />

            {/* Toast Notification */}
            {toast.show && (
                <div
                    className="animate-fade-in-down"
                    style={{
                        position: 'fixed', top: '32px', left: '50%',
                        transform: 'translateX(-50%)',
                        padding: '12px 24px', borderRadius: '100px',
                        backgroundColor: toast.type === 'success' ? 'rgba(16, 185, 129, 0.9)' : 'rgba(239, 68, 68, 0.9)',
                        backdropFilter: 'blur(8px)',
                        color: 'white', display: 'flex', alignItems: 'center', gap: '12px',
                        boxShadow: '0 10px 40px -10px rgba(0, 0, 0, 0.5)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        zIndex: 2000
                    }}
                >
                    {toast.type === 'success' ? <CheckCircle size={18} /> : <XCircle size={18} />}
                    <span style={{ fontWeight: '600', fontSize: '0.95rem' }}>{toast.message}</span>
                </div>
            )}
        </div>
    );
};

export default Dashboard;
