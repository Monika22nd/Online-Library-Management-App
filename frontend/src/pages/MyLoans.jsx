import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import './MyLoans.css';

const STATUS_TABS = ['ALL', 'PENDING', 'APPROVED', 'RETURNED', 'CANCELLED'];

export default function MyLoans() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loans, setLoans] = useState([]);
  const [tab, setTab] = useState('ALL');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
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
    try {
      await api.returnLoan(loanId);
      loadLoans(tab);
    } catch (err) {
      alert('Return failed: ' + err.message);
    }
  }

  async function handleCancel(loanId) {
    try {
      await api.cancelLoan(loanId);
      loadLoans(tab);
    } catch (err) {
      alert('Cancel failed: ' + err.message);
    }
  }

  const statusColor = (s) => {
    switch (s) {
      case 'APPROVED': return 'var(--success)';
      case 'PENDING': return 'var(--warning)';
      case 'RETURNED': return 'var(--info)';
      case 'CANCELLED': case 'REJECTED': return 'var(--danger)';
      default: return 'var(--text-muted)';
    }
  };

  if (!user) return null;

  return (
    <div className="loans-page container">
      <h1 className="loans-title slide-up">My Loans</h1>

      <div className="loans-tabs">
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

      {loading ? (
        <div className="loans-loading">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="loan-skeleton">
              <div className="skeleton" style={{ width: 50, height: 75 }} />
              <div style={{ flex: 1 }}>
                <div className="skeleton" style={{ height: 16, width: '50%', marginBottom: 8 }} />
                <div className="skeleton" style={{ height: 12, width: '30%' }} />
              </div>
            </div>
          ))}
        </div>
      ) : loans.length === 0 ? (
        <div className="loans-empty slide-up">
          <p>No loans found.</p>
        </div>
      ) : (
        <div className="loans-list">
          {loans.map((loan) => (
            <div key={loan.id} className="loan-card fade-in">
              <img
                src={loan.cover_url || 'https://covers.openlibrary.org/b/id/0-M.jpg'}
                alt={loan.title}
                className="loan-cover"
              />
              <div className="loan-info">
                <h3 className="loan-book-title">{loan.title}</h3>
                <p className="loan-author">{loan.author}</p>
                <div className="loan-meta">
                  <span className="loan-status" style={{ color: statusColor(loan.status) }}>
                    ● {loan.status}
                  </span>
                  <span className="loan-date">Requested: {loan.request_date}</span>
                  {loan.due_date && <span className="loan-date">Due: {loan.due_date}</span>}
                </div>
              </div>
              <div className="loan-actions">
                {loan.status === 'APPROVED' && (
                  <button className="return-btn" onClick={() => handleReturn(loan.id)}>Return</button>
                )}
                {loan.status === 'PENDING' && (
                  <button className="cancel-btn" onClick={() => handleCancel(loan.id)}>Cancel</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
