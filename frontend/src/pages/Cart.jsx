import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api, { coverUrl } from '../api';
import './Cart.css';

export default function Cart() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [cart, setCart] = useState([]);
  const [loading, setLoading] = useState(true);
  const [checkingOut, setCheckingOut] = useState(false);
  const [result, setResult] = useState(null); // { kind: 'success'|'error', message, skipped? }

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    loadCart();
  }, [user]);

  async function loadCart() {
    setLoading(true);
    try {
      const items = await api.getCart(user.id);
      setCart(items);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function handleRemove(bookId) {
    try {
      await api.removeFromCart(bookId, user.id);
      setCart((prev) => prev.filter((b) => b.id !== bookId));
    } catch (err) {
      console.error(err);
    }
  }

  async function handleCheckout() {
    setCheckingOut(true);
    setResult(null);
    try {
      const res = await api.checkout(user.id);
      const skipped = res?.skipped || [];
      setResult({
        kind: 'success',
        message: `Checked out ${res?.loans?.length || 0} book(s).`,
        skipped,
      });
      setCart([]);
    } catch (err) {
      setResult({ kind: 'error', message: 'Checkout failed: ' + err.message });
    } finally {
      setCheckingOut(false);
    }
  }

  if (!user) return null;

  return (
    <div className="cart-page container">
      <header className="cart-header slide-up">
        <p className="eyebrow">Step 1 of 1</p>
        <h1 className="cart-title">Borrowing cart</h1>
        <p className="cart-subtitle">Review your selections, then check out to submit borrow requests.</p>
      </header>

      {loading ? (
        <div className="cart-grid">
          <div className="cart-items">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="cart-item-skeleton">
                <div className="skeleton" style={{ width: 64, height: 96 }} />
                <div style={{ flex: 1 }}>
                  <div className="skeleton" style={{ height: 16, width: '60%', marginBottom: 8 }} />
                  <div className="skeleton" style={{ height: 12, width: '40%' }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : cart.length === 0 ? (
        <div className="cart-empty slide-up">
          <div className="cart-empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
              <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6" />
            </svg>
          </div>
          <h2 className="cart-empty-title">Your cart is empty.</h2>
          <p className="cart-empty-sub">Browse the catalog and add books to borrow.</p>
          <Link to="/" className="btn btn-primary">Browse the Library</Link>
          {result?.kind === 'success' && (
            <div className="checkout-success">
              <p>{result.message} <Link to="/loans">View your loans →</Link></p>
              {result.skipped?.length > 0 && (
                <ul className="skipped-list">
                  {result.skipped.map((s, i) => (
                    <li key={i}>Book #{s.book_id}: {s.reason}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="cart-grid slide-up">
          <div className="cart-items">
            {cart.map((book) => (
              <div key={book.id} className="cart-item">
                <Link to={`/works/${(book.openlibrary_key || '').replace('/works/', '')}`} className="cart-cover-link">
                  <img
                    src={coverUrl(book.cover_url)}
                    alt={book.title}
                    className="cart-item-cover"
                  />
                </Link>
                <div className="cart-item-info">
                  <Link to={`/works/${(book.openlibrary_key || '').replace('/works/', '')}`} className="cart-item-title">
                    {book.title}
                  </Link>
                  <p className="cart-item-author">{book.author || '—'}</p>
                  <span className={`avail-pill ${book.available_copies > 0 ? 'avail-on' : 'avail-off'}`}>
                    {book.available_copies > 0
                      ? `${book.available_copies} of ${book.total_copies} available`
                      : 'Unavailable'}
                  </span>
                </div>
                <button className="remove-btn" onClick={() => handleRemove(book.id)} title="Remove from cart">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>
            ))}
          </div>

          <aside className="cart-summary">
            <h3 className="summary-title">Order summary</h3>
            <dl className="summary-list">
              <div className="summary-row">
                <dt>Books</dt>
                <dd>{cart.length}</dd>
              </div>
              <div className="summary-row">
                <dt>Loan period</dt>
                <dd>14 days</dd>
              </div>
              <div className="summary-row">
                <dt>Approval</dt>
                <dd>Manual review</dd>
              </div>
            </dl>
            <div className="summary-total">
              <span>Cost</span>
              <span className="free">Free</span>
            </div>
            <button
              className="btn btn-primary checkout-btn"
              onClick={handleCheckout}
              disabled={checkingOut}
            >
              {checkingOut ? 'Submitting…' : 'Submit Borrow Requests'}
            </button>
            {result && (
              <div className={`cart-msg ${result.kind === 'error' ? 'msg-error' : 'msg-success'}`}>
                {result.message}
                {result.skipped?.length > 0 && (
                  <ul className="skipped-list">
                    {result.skipped.map((s, i) => (
                      <li key={i}>Book #{s.book_id}: {s.reason}</li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}
