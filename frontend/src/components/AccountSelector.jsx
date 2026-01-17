import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, Folder, Plus, Trash2 } from 'lucide-react';

const AccountSelector = ({ accounts, currentAccountId, onSelect, onNew, onDelete }) => {
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);
    const currentAccount = accounts.find(p => p.id === currentAccountId);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSelect = (id) => {
        onSelect(id);
        setIsOpen(false);
    };

    return (
        <div ref={dropdownRef} style={{ position: 'relative', display: 'inline-block' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div
                    onClick={() => setIsOpen(!isOpen)}
                    style={{
                        display: 'flex', alignItems: 'center', gap: '12px',
                        padding: '8px 16px', borderRadius: '12px',
                        backgroundColor: 'var(--bg-card)', border: '1px solid var(--border)',
                        cursor: 'pointer', transition: 'all 0.2s',
                        fontSize: '0.95rem', fontWeight: '600',
                        minWidth: '180px', justifyContent: 'space-between'
                    }}
                    className="hover-lift"
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <Folder size={18} color="var(--primary)" />
                        <span>{currentAccount?.name || '계좌 선택'}</span>
                    </div>
                    <ChevronDown size={16} color="var(--text-muted)" style={{ transform: isOpen ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }} />
                </div>

                <button onClick={onNew} style={{
                    padding: '8px', borderRadius: '10px',
                    backgroundColor: 'rgba(99, 102, 241, 0.1)', border: '1px solid var(--primary)',
                    color: 'var(--primary)', cursor: 'pointer', display: 'flex', alignItems: 'center'
                }} className="hover-lift" title="새 계좌">
                    <Plus size={18} />
                </button>
            </div>

            {isOpen && (
                <div className="glass-card animate-fade-in" style={{
                    position: 'absolute', top: '120%', left: 0, width: '240px',
                    padding: '8px', zIndex: 100, boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.3)'
                }}>
                    {accounts.length === 0 ? (
                        <div style={{ padding: '12px', color: 'var(--text-muted)', fontSize: '0.875rem' }}>계좌가 없습니다.</div>
                    ) : (
                        accounts.map(p => (
                            <div key={p.id} style={{
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                padding: '10px 12px', borderRadius: '8px', cursor: 'pointer',
                                backgroundColor: p.id === currentAccountId ? 'rgba(99, 102, 241, 0.15)' : 'transparent',
                                transition: 'background-color 0.2s'
                            }} onClick={() => handleSelect(p.id)} className="hover-highlight">
                                <span style={{
                                    color: p.id === currentAccountId ? 'var(--primary)' : 'var(--text-main)',
                                    fontWeight: p.id === currentAccountId ? '700' : '400',
                                    fontSize: '0.9rem'
                                }}>{p.name}</span>
                                <button onClick={(e) => { e.stopPropagation(); onDelete(p.id); }} style={{
                                    background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer',
                                    padding: '4px', borderRadius: '4px'
                                }} className="hover:text-danger">
                                    <Trash2 size={14} />
                                </button>
                            </div>
                        ))
                    )}
                </div>
            )}
        </div>
    );
};

export default AccountSelector;
