import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import './Cart.css';

export default function Cart() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [cart, setCart] = useState([]);
  const [loading, setLoading] = useState(true);
  const [checkingOut, setCheckingOut] = useState(false);
  const [msg, setMsg] = useState('');

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
      setCart(cart.filter((b) => b.id !== bookId));
    } catch (err) {
      console.error(err);
    }
  }

  async function handleCheckout() {
    setCheckingOut(true);
    try {
      await api.checkout(user.id);
      setMsg('Checkout successful! Check your loans.');
      setCart([]);
    } catch (err) {
      setMsg('Checkout failed: ' + err.message);
    } finally {
      setCheckingOut(false);
    }
  }

  if (!user) return null;

  return (
    <div className="cart-page container">
      <h1 className="cart-title slide-up">My Cart</h1>

      {loading ? (
        <div className="cart-loading">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="cart-item-skeleton">
              <div className="skeleton" style={{ width: 60, height: 90 }} />
              <div style={{ flex: 1 }}>
                <div className="skeleton" style={{ height: 18, width: '60%', marginBottom: 8 }} />
                <div className="skeleton" style={{ height: 14, width: '40%' }} />
              </div>
            </div>
          ))}
        </div>
      ) : cart.length === 0 ? (
        <div className="cart-empty slide-up">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6" />
          </svg>
          <h2>Your Cart is Empty</h2>
          <p>Browse our collection and add books to borrow.</p>
          <Link to="/" className="browse-btn">Browse Books</Link>
        </div>
      ) : (
        <div className="cart-content slide-up">
          <div className="cart-items">
            {cart.map((book) => (
              <div key={book.id} className="cart-item">
                <img
                  src={book.cover_url || 'https://covers.openlibrary.org/b/id/0-M.jpg'}
                  alt={book.title}
                  className="cart-item-cover"
                />
                <div className="cart-item-info">
                  <h3 className="cart-item-title">{book.title}</h3>
                  <p className="cart-item-author">{book.author}</p>
                  <span className={`avail-badge ${book.available_copies > 0 ? 'available' : 'unavailable'}`}>
                    {book.available_copies > 0 ? `${book.available_copies} available` : 'Unavailable'}
                  </span>
                </div>
                <button className="remove-btn" onClick={() => handleRemove(book.id)} title="Remove">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>
            ))}
          </div>

          <div className="cart-summary">
            <div className="summary-row">
              <span>Books</span>
              <span>{cart.length}</span>
            </div>
            <div className="summary-row">
              <span>Loan Period</span>
              <span>14 days</span>
            </div>
            <div className="summary-row total">
              <span>Cost</span>
              <span className="free-badge">Free</span>
            </div>
            <button
              className="checkout-btn"
              onClick={handleCheckout}
              disabled={checkingOut || cart.length === 0}
            >
              {checkingOut ? 'Processing...' : 'Checkout & Borrow'}
            </button>
            {msg && <p className={`cart-msg ${msg.includes('failed') ? 'error' : 'success'}`}>{msg}</p>}
          </div>
        </div>
      )}
    </div>
  );
}
