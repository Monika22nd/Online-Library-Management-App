import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import './AdminPanel.css';

const STATUS_TABS = ['ALL', 'PENDING', 'APPROVED', 'RETURNED', 'CANCELLED'];

export default function AdminPanel() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loans, setLoans] = useState([]);
  const [tab, setTab] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(new Set());
  const [auditLog, setAuditLog] = useState([]);
  const [showAudit, setShowAudit] = useState(false);
  const [actionMsg, setActionMsg] = useState('');

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (user.role !== 'ADMIN') {
      navigate('/');
      return;
    }
  }, [user]);

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      loadLoans(tab);
    }
  }, [user, tab]);

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
    try {
      const data = await api.getAuditLog(50);
      setAuditLog(data || []);
      setShowAudit(true);
    } catch (err) {
      console.error(err);
    }
  }

  async function handleApprove(loanId) {
    try {
      await api.approveLoan(loanId, user.id);
      setActionMsg(`Loan #${loanId} approved.`);
      loadLoans(tab);
    } catch (err) {
      setActionMsg(`Error: ${err.message}`);
    }
  }

  async function handleReject(loanId) {
    try {
      await api.rejectLoan(loanId);
      setActionMsg(`Loan #${loanId} rejected.`);
      loadLoans(tab);
    } catch (err) {
      setActionMsg(`Error: ${err.message}`);
    }
  }

  async function handleBulkApprove() {
    if (selected.size === 0) return;
    try {
      const ids = Array.from(selected);
      await api.bulkApproveLoans(ids, user.id);
      setActionMsg(`${ids.length} loans approved.`);
      setSelected(new Set());
      loadLoans(tab);
    } catch (err) {
      setActionMsg(`Error: ${err.message}`);
    }
  }

  function toggleSelect(id) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleSelectAll() {
    const pendingLoans = loans.filter((l) => l.status === 'PENDING');
    if (selected.size === pendingLoans.length && pendingLoans.length > 0) {
      setSelected(new Set());
    } else {
      setSelected(new Set(pendingLoans.map((l) => l.id)));
    }
  }

  const statusColor = (s) => {
    switch (s) {
      case 'APPROVED': return 'status-approved';
      case 'PENDING': return 'status-pending';
      case 'RETURNED': return 'status-returned';
      case 'CANCELLED': case 'REJECTED': return 'status-cancelled';
      default: return '';
    }
  };

  // Stats
  const stats = {
    total: loans.length,
    pending: loans.filter((l) => l.status === 'PENDING').length,
    approved: loans.filter((l) => l.status === 'APPROVED').length,
    returned: loans.filter((l) => l.status === 'RETURNED').length,
  };

  const pendingLoans = loans.filter((l) => l.status === 'PENDING');

  if (!user || user.role !== 'ADMIN') return null;

  return (
    <div className="admin-page container">
      <div className="admin-header slide-up">
        <div>
          <h1 className="admin-title">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
            Admin Panel
          </h1>
          <p className="admin-subtitle">Manage loan requests and monitor activity</p>
        </div>
        <button className="audit-toggle-btn" onClick={showAudit ? () => setShowAudit(false) : loadAudit}>
          {showAudit ? 'Hide Audit Log' : 'View Audit Log'}
        </button>
      </div>

      {/* Stats */}
      <div className="stats-grid slide-up">
        <div className="stat-card">
          <span className="stat-value">{stats.total}</span>
          <span className="stat-label">Total Loans</span>
        </div>
        <div className="stat-card stat-pending">
          <span className="stat-value">{stats.pending}</span>
          <span className="stat-label">Pending</span>
        </div>
        <div className="stat-card stat-approved">
          <span className="stat-value">{stats.approved}</span>
          <span className="stat-label">Approved</span>
        </div>
        <div className="stat-card stat-returned">
          <span className="stat-value">{stats.returned}</span>
          <span className="stat-label">Returned</span>
        </div>
      </div>

      {actionMsg && (
        <div className={`action-msg ${actionMsg.startsWith('Error') ? 'error' : 'success'}`}>
          {actionMsg}
          <button className="msg-close" onClick={() => setActionMsg('')}>×</button>
        </div>
      )}

      {/* Tabs + Bulk Actions */}
      <div className="admin-toolbar">
        <div className="admin-tabs">
          {STATUS_TABS.map((s) => (
            <button
              key={s}
              className={`tab-btn ${tab === s ? 'active' : ''}`}
              onClick={() => setTab(s)}
            >
              {s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
        {pendingLoans.length > 0 && (tab === 'ALL' || tab === 'PENDING') && (
          <div className="bulk-actions">
            <button className="select-all-btn" onClick={toggleSelectAll}>
              {selected.size === pendingLoans.length ? 'Deselect All' : 'Select All Pending'}
            </button>
            <button
              className="bulk-approve-btn"
              onClick={handleBulkApprove}
              disabled={selected.size === 0}
            >
              Approve Selected ({selected.size})
            </button>
          </div>
        )}
      </div>

      {/* Loans Table */}
      {loading ? (
        <div className="admin-loading">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="table-row-skeleton">
              <div className="skeleton" style={{ width: 40, height: 16 }} />
              <div className="skeleton" style={{ width: '30%', height: 16 }} />
              <div className="skeleton" style={{ width: '20%', height: 16 }} />
              <div className="skeleton" style={{ width: 80, height: 16 }} />
            </div>
          ))}
        </div>
      ) : loans.length === 0 ? (
        <div className="admin-empty">
          <p>No loans found for this filter.</p>
        </div>
      ) : (
        <div className="loans-table-wrap">
          <table className="loans-table">
            <thead>
              <tr>
                <th className="th-check"></th>
                <th>ID</th>
                <th>Book</th>
                <th>Borrower</th>
                <th>Requested</th>
                <th>Due Date</th>
                <th>Status</th>
                <th>Actions</th>
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
                        className="loan-checkbox"
                      />
                    )}
                  </td>
                  <td className="td-id">#{loan.id}</td>
                  <td className="td-book">
                    <div className="book-cell">
                      <img
                        src={loan.cover_url || 'https://covers.openlibrary.org/b/id/0-S.jpg'}
                        alt=""
                        className="table-cover"
                      />
                      <span className="table-book-title">{loan.title || `Book #${loan.book_id}`}</span>
                    </div>
                  </td>
                  <td className="td-user">{loan.username || `User #${loan.user_id}`}</td>
                  <td className="td-date">{loan.request_date || '—'}</td>
                  <td className="td-date">{loan.due_date || '—'}</td>
                  <td>
                    <span className={`status-badge ${statusColor(loan.status)}`}>
                      {loan.status}
                    </span>
                  </td>
                  <td className="td-actions">
                    {loan.status === 'PENDING' && (
                      <>
                        <button className="action-approve" onClick={() => handleApprove(loan.id)}>
                          Approve
                        </button>
                        <button className="action-reject" onClick={() => handleReject(loan.id)}>
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

      {/* Audit Log */}
      {showAudit && (
        <div className="audit-section slide-up">
          <h2 className="audit-title">Audit Log</h2>
          {auditLog.length === 0 ? (
            <p className="audit-empty">No audit entries found.</p>
          ) : (
            <div className="audit-list">
              {auditLog.map((entry, i) => (
                <div key={i} className="audit-entry">
                  <div className="audit-meta">
                    <span className="audit-table">{entry.table_name}</span>
                    <span className="audit-action">{entry.action}</span>
                    <span className="audit-time">{entry.created_at}</span>
                  </div>
                  <div className="audit-change">
                    Record #{entry.record_id}: <span className="old-val">{entry.old_value}</span>
                    {' → '}
                    <span className="new-val">{entry.new_value}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
