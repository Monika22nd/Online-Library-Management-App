import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api, { coverUrl } from '../api';
import './MyLoans.css';

const STATUS_TABS = [
  { key: 'ALL',       label: 'All' },
  { key: 'PENDING',   label: 'Pending' },
  { key: 'APPROVED',  label: 'Borrowed' },
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

function daysUntil(dateStr) {
  if (!dateStr) return null;
  const due = new Date(dateStr);
  const now = new Date();
  due.setHours(0, 0, 0, 0); now.setHours(0, 0, 0, 0);
  return Math.round((due - now) / 86400000);
}

export default function MyLoans() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loans, setLoans] = useState([]);
  const [tab, setTab] = useState('ALL');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    loadLoans(tab);
  }, [user, tab]);

  async function loadLoans(status) {
    setLoading(true);
    try {
      const data = await api.getUserLoans(user.id, status === 'ALL' ? null : status);
      setLoans(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function handleReturn(loanId) {
    try { await api.returnLoan(loanId); loadLoans(tab); }
    catch (err) { alert('Return failed: ' + err.message); }
  }
  async function handleCancel(loanId) {
    try { await api.cancelLoan(loanId); loadLoans(tab); }
    catch (err) { alert('Cancel failed: ' + err.message); }
  }

  const counts = useMemo(() => {
    const c = { ALL: loans.length, PENDING: 0, APPROVED: 0, RETURNED: 0, CANCELLED: 0 };
    for (const l of loans) {
      if (l.status in c) c[l.status]++;
    }
    return c;
  }, [loans]);

  if (!user) return null;

  return (
    <div className="loans-page container">
      <header className="loans-header slide-up">
        <p className="eyebrow">{user.username}'s account</p>
        <h1 className="loans-title">My loans</h1>
        <p className="loans-subtitle">Track requests, view approved borrows, and return books here.</p>
      </header>

      <div className="loans-tabs">
        {STATUS_TABS.map((s) => (
          <button
            key={s.key}
            className={`loan-tab ${tab === s.key ? 'active' : ''}`}
            onClick={() => setTab(s.key)}
          >
            {s.label}
            {tab !== 'ALL' && s.key === 'ALL' ? null
              : counts[s.key] > 0 && <span className="tab-count">{counts[s.key]}</span>}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loans-list">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="loan-row loan-skeleton">
              <div className="skeleton" style={{ width: 56, height: 84 }} />
              <div style={{ flex: 1 }}>
                <div className="skeleton" style={{ height: 16, width: '50%', marginBottom: 8 }} />
                <div className="skeleton" style={{ height: 12, width: '30%' }} />
              </div>
            </div>
          ))}
        </div>
      ) : loans.length === 0 ? (
        <div className="loans-empty slide-up">
          <h2>Nothing here yet.</h2>
          <p>{tab === 'ALL'
            ? 'Once you borrow a book, it will show up here.'
            : `No loans with status ${tab.toLowerCase()}.`}</p>
        </div>
      ) : (
        <div className="loans-list">
          {loans.map((loan) => {
            const cover = coverUrl(loan.cover_url);
            const due = loan.due_date && loan.status === 'APPROVED' ? daysUntil(loan.due_date) : null;
            const overdue = due !== null && due < 0;
            return (
              <article key={loan.id} className="loan-row fade-in">
                <img src={cover} alt={loan.title || ''} className="loan-cover" />

                <div className="loan-main">
                  <div className="loan-head">
                    <h3 className="loan-book-title">{loan.title || `Book #${loan.requested_book_id || ''}`}</h3>
                    <span className={statusBadgeClass(loan.status)}>{loan.status}</span>
                  </div>
                  <p className="loan-author">{loan.author || '—'}</p>

                  <dl className="loan-meta">
                    <div>
                      <dt>Requested</dt>
                      <dd>{loan.request_date || '—'}</dd>
                    </div>
                    {loan.borrow_date && (
                      <div>
                        <dt>Borrowed</dt>
                        <dd>{loan.borrow_date}</dd>
                      </div>
                    )}
                    {loan.due_date && (
                      <div>
                        <dt>Due</dt>
                        <dd className={overdue ? 'overdue' : ''}>
                          {loan.due_date}
                          {due !== null && (
                            <span className="due-rel">
                              {' · '}
                              {overdue ? `${Math.abs(due)}d overdue`
                               : due === 0 ? 'today'
                               : `in ${due}d`}
                            </span>
                          )}
                        </dd>
                      </div>
                    )}
                    {loan.return_date && (
                      <div>
                        <dt>Returned</dt>
                        <dd>{loan.return_date}</dd>
                      </div>
                    )}
                  </dl>
                </div>

                <div className="loan-actions">
                  {loan.status === 'APPROVED' && (
                    <button className="btn btn-secondary" onClick={() => handleReturn(loan.id)}>
                      Return
                    </button>
                  )}
                  {loan.status === 'PENDING' && (
                    <button className="btn btn-ghost" onClick={() => handleCancel(loan.id)}>
                      Cancel
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
