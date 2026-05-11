import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api, { coverUrl } from '../api';
import './AdminPanel.css';

const STATUS_TABS = [
  { key: 'ALL',       label: 'All' },
  { key: 'PENDING',   label: 'Pending' },
  { key: 'APPROVED',  label: 'Approved' },
  { key: 'RETURNED',  label: 'Returned' },
  { key: 'CANCELLED', label: 'Cancelled' },
];

function statusBadgeClass(s) {
  switch (s) {
    case 'APPROVED': return 'badge badge-approved';
    case 'PENDING':  return 'badge badge-pending';
    case 'RETURNED': return 'badge badge-returned';
    case 'CANCELLED':
    case 'REJECTED': return 'badge badge-cancelled';
    default: return 'badge';
  }
}

export default function AdminPanel() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState('loans'); // 'loans' | 'audit'
  const [loans, setLoans] = useState([]);
  const [tab, setTab] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(new Set());
  const [auditLog, setAuditLog] = useState([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [actionMsg, setActionMsg] = useState(null);

  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    if (user.role !== 'ADMIN') { navigate('/'); return; }
  }, [user]);

  useEffect(() => {
    if (user?.role === 'ADMIN' && activeView === 'loans') loadLoans(tab);
    if (user?.role === 'ADMIN' && activeView === 'audit') loadAudit();
  }, [user, tab, activeView]);

  async function loadLoans(status) {
    setLoading(true);
    setSelected(new Set());
    try {
      const data = await api.getAllLoans(status === 'ALL' ? null : status);
      setLoans(data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function loadAudit() {
    setAuditLoading(true);
    try {
      const data = await api.getAuditLog(50);
      setAuditLog(data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setAuditLoading(false);
    }
  }

  async function handleApprove(loanId) {
    try {
      const res = await api.approveLoan(loanId, user.id);
      if (res?.status === 'APPROVED') {
        setActionMsg({ kind: 'success', text: res.message || `Loan #${loanId} approved.` });
      } else if (res?.status === 'REJECTED') {
        // sp_approve_loan auto-rejected because no copy was available;
        // the loan state changed, just not in the user's favor.
        setActionMsg({ kind: 'error',
          text: `Loan #${loanId} auto-rejected: ${res.message || 'no copies available'}.` });
      } else {
        setActionMsg({ kind: 'error', text: `Loan #${loanId}: ${res?.message || 'no change'}.` });
      }
      loadLoans(tab);
    } catch (err) {
      setActionMsg({ kind: 'error', text: `Approve failed: ${err.message}` });
    }
  }

  async function handleReject(loanId) {
    try {
      await api.rejectLoan(loanId);
      setActionMsg({ kind: 'success', text: `Loan #${loanId} rejected.` });
      loadLoans(tab);
    } catch (err) {
      setActionMsg({ kind: 'error', text: `Reject failed: ${err.message}` });
    }
  }

  async function handleBulkApprove() {
    if (selected.size === 0) return;
    const ids = Array.from(selected);
    try {
      const res = await api.bulkApproveLoans(ids, user.id);
      setActionMsg({ kind: 'success', text: `Approved ${res?.approved ?? ids.length} of ${ids.length} loan(s).` });
      setSelected(new Set());
      loadLoans(tab);
    } catch (err) {
      setActionMsg({ kind: 'error', text: `Bulk approve failed: ${err.message}` });
    }
  }

  function toggleSelect(id) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  function toggleSelectAll() {
    const pendingIds = loans.filter((l) => l.status === 'PENDING').map((l) => l.id);
    if (selected.size === pendingIds.length && pendingIds.length > 0) {
      setSelected(new Set());
    } else {
      setSelected(new Set(pendingIds));
    }
  }

  const stats = useMemo(() => ({
    total: loans.length,
    pending:  loans.filter((l) => l.status === 'PENDING').length,
    approved: loans.filter((l) => l.status === 'APPROVED').length,
    returned: loans.filter((l) => l.status === 'RETURNED').length,
    cancelled: loans.filter((l) => l.status === 'CANCELLED' || l.status === 'REJECTED').length,
  }), [loans]);

  const pendingCount = stats.pending;
  const showBulkBar = pendingCount > 0 && (tab === 'ALL' || tab === 'PENDING');

  if (!user || user.role !== 'ADMIN') return null;

  return (
    <div className="admin-page container">
      <header className="admin-header slide-up">
        <div>
          <p className="eyebrow">Administrator</p>
          <h1 className="admin-title">Library admin</h1>
          <p className="admin-subtitle">Review borrow requests, return books, and audit changes.</p>
        </div>
        <nav className="admin-view-nav">
          <button
            className={`view-tab ${activeView === 'loans' ? 'active' : ''}`}
            onClick={() => setActiveView('loans')}
          >
            Loans
          </button>
          <button
            className={`view-tab ${activeView === 'audit' ? 'active' : ''}`}
            onClick={() => setActiveView('audit')}
          >
            Audit log
          </button>
        </nav>
      </header>

      {actionMsg && (
        <div className={`action-msg msg-${actionMsg.kind}`}>
          <span>{actionMsg.text}</span>
          <button className="msg-close" onClick={() => setActionMsg(null)}>×</button>
        </div>
      )}

      {activeView === 'loans' && (
        <>
          <div className="stats-grid">
            <div className="stat-card">
              <span className="stat-num">{stats.total}</span>
              <span className="stat-lbl">Total in view</span>
            </div>
            <div className="stat-card stat-pending">
              <span className="stat-num">{stats.pending}</span>
              <span className="stat-lbl">Pending</span>
            </div>
            <div className="stat-card stat-approved">
              <span className="stat-num">{stats.approved}</span>
              <span className="stat-lbl">Approved</span>
            </div>
            <div className="stat-card stat-returned">
              <span className="stat-num">{stats.returned}</span>
              <span className="stat-lbl">Returned</span>
            </div>
            <div className="stat-card stat-cancelled">
              <span className="stat-num">{stats.cancelled}</span>
              <span className="stat-lbl">Cancelled</span>
            </div>
          </div>

          <div className="admin-toolbar">
            <div className="admin-tabs">
              {STATUS_TABS.map((s) => (
                <button
                  key={s.key}
                  className={`loan-tab ${tab === s.key ? 'active' : ''}`}
                  onClick={() => setTab(s.key)}
                >
                  {s.label}
                </button>
              ))}
            </div>
            {showBulkBar && (
              <div className="bulk-actions">
                <button className="btn btn-ghost" onClick={toggleSelectAll}>
                  {selected.size === pendingCount ? 'Clear selection' : `Select all pending (${pendingCount})`}
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleBulkApprove}
                  disabled={selected.size === 0}
                >
                  Approve selected ({selected.size})
                </button>
              </div>
            )}
          </div>

          {loading ? (
            <div className="admin-table-wrap">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="row-skeleton">
                  <div className="skeleton" style={{ width: 24, height: 18 }} />
                  <div className="skeleton" style={{ width: 40, height: 60 }} />
                  <div className="skeleton" style={{ flex: 1, height: 16 }} />
                  <div className="skeleton" style={{ width: 90, height: 22 }} />
                </div>
              ))}
            </div>
          ) : loans.length === 0 ? (
            <div className="admin-empty">
              <h3>No loans for this filter.</h3>
              <p>Try a different status tab.</p>
            </div>
          ) : (
            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th className="th-check"></th>
                    <th>ID</th>
                    <th>Book</th>
                    <th>Borrower</th>
                    <th>Requested</th>
                    <th>Due</th>
                    <th>Status</th>
                    <th className="th-actions">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loans.map((loan) => (
                    <tr key={loan.id} className={selected.has(loan.id) ? 'row-selected' : ''}>
                      <td className="td-check">
                        {loan.status === 'PENDING' && (
                          <input
                            type="checkbox"
                            checked={selected.has(loan.id)}
                            onChange={() => toggleSelect(loan.id)}
                          />
                        )}
                      </td>
                      <td className="td-id">#{loan.id}</td>
                      <td className="td-book">
                        <img
                          src={coverUrl(loan.cover_url)}
                          alt=""
                          className="row-cover"
                        />
                        <span className="row-title">
                          {loan.title || `Book #${loan.requested_book_id || '?'}`}
                        </span>
                      </td>
                      <td className="td-borrower">{loan.username || `Member #${loan.member_id}`}</td>
                      <td className="td-date">{loan.request_date || '—'}</td>
                      <td className="td-date">{loan.due_date || '—'}</td>
                      <td><span className={statusBadgeClass(loan.status)}>{loan.status}</span></td>
                      <td className="td-actions">
                        {loan.status === 'PENDING' && (
                          <>
                            <button className="btn btn-secondary btn-sm" onClick={() => handleApprove(loan.id)}>
                              Approve
                            </button>
                            <button className="btn btn-ghost btn-sm danger" onClick={() => handleReject(loan.id)}>
                              Reject
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {activeView === 'audit' && (
        <section className="audit-section">
          <div className="audit-header">
            <h2 className="audit-title">Recent activity</h2>
            <button className="btn btn-ghost" onClick={loadAudit}>Refresh</button>
          </div>
          {auditLoading ? (
            <div className="audit-list">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="audit-entry">
                  <div className="skeleton" style={{ height: 14, width: '40%', marginBottom: 8 }} />
                  <div className="skeleton" style={{ height: 12, width: '70%' }} />
                </div>
              ))}
            </div>
          ) : auditLog.length === 0 ? (
            <div className="admin-empty">
              <h3>No audit entries yet.</h3>
              <p>Loan status changes will appear here automatically.</p>
            </div>
          ) : (
            <div className="audit-list">
              {auditLog.map((entry) => (
                <div key={entry.id} className="audit-entry">
                  <div className="audit-meta">
                    <span className="audit-table">{entry.table_name}</span>
                    <span className="audit-action">{entry.action}</span>
                    <span className="audit-record">#{entry.record_id}</span>
                    <span className="audit-time">{entry.changed_at}</span>
                  </div>
                  <div className="audit-change">
                    <span className="old-val">{entry.old_value || '—'}</span>
                    <span className="audit-arrow">→</span>
                    <span className="new-val">{entry.new_value || '—'}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
