import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import './Navbar.css';

const SUBJECTS = [
  { label: 'Fiction', slug: 'fiction' },
  { label: 'Science', slug: 'science' },
  { label: 'History', slug: 'history' },
  { label: 'Fantasy', slug: 'fantasy' },
  { label: 'Romance', slug: 'romance' },
  { label: 'Horror', slug: 'horror' },
  { label: 'Biography', slug: 'biography' },
  { label: 'Philosophy', slug: 'philosophy' },
];

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [browseOpen, setBrowseOpen] = useState(false);
  const [userOpen, setUserOpen] = useState(false);
  const browseRef = useRef(null);
  const userRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    function handler(e) {
      if (browseRef.current && !browseRef.current.contains(e.target)) setBrowseOpen(false);
      if (userRef.current && !userRef.current.contains(e.target)) setUserOpen(false);
    }
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    if (query.trim()) {
      navigate(`/search?q=${encodeURIComponent(query.trim())}`);
      setQuery('');
      inputRef.current?.blur();
    }
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner container">
        <Link to="/" className="navbar-logo">
          <span className="logo-text">The Library</span>
        </Link>

        <div className="navbar-mid">
          <div className="browse-menu" ref={browseRef}>
            <button
              className={`navbar-link ${browseOpen ? 'active' : ''}`}
              onClick={() => setBrowseOpen((v) => !v)}
            >
              Browse
              <svg className="caret" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            {browseOpen && (
              <div className="browse-dropdown">
                <div className="dropdown-header">Subjects</div>
                <div className="dropdown-grid">
                  {SUBJECTS.map((s) => (
                    <Link
                      key={s.slug}
                      to={`/subjects/${s.slug}`}
                      className="dropdown-link"
                      onClick={() => setBrowseOpen(false)}
                    >
                      {s.label}
                    </Link>
                  ))}
                </div>
              </div>
            )}
          </div>

          <Link to="/search" className="navbar-link">Search</Link>
          {user && <Link to="/loans" className="navbar-link">My Loans</Link>}
        </div>

        <form className="navbar-search" onSubmit={handleSearch}>
          <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            ref={inputRef}
            type="text"
            placeholder="Search books, authors…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="search-input"
          />
        </form>

        <div className="navbar-actions">
          {user ? (
            <>
              <Link to="/cart" className="icon-link" title="Cart">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
                  <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6" />
                </svg>
              </Link>
              <div className="user-menu" ref={userRef}>
                <button className="user-btn" onClick={() => setUserOpen((v) => !v)}>
                  <span className="user-avatar">{user.username[0].toUpperCase()}</span>
                </button>
                {userOpen && (
                  <div className="user-dropdown">
                    <div className="user-info">
                      <span className="user-name">{user.username}</span>
                      <span className="user-role">{user.role}</span>
                    </div>
                    <Link to="/loans" className="dropdown-link" onClick={() => setUserOpen(false)}>
                      My Loans
                    </Link>
                    <Link to="/cart" className="dropdown-link" onClick={() => setUserOpen(false)}>
                      Cart
                    </Link>
                    {user.role === 'ADMIN' && (
                      <Link to="/admin" className="dropdown-link" onClick={() => setUserOpen(false)}>
                        Admin Panel
                      </Link>
                    )}
                    <button
                      className="dropdown-link signout"
                      onClick={() => { setUserOpen(false); logout(); navigate('/'); }}
                    >
                      Sign Out
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <Link to="/login" className="btn btn-primary nav-signin">Sign In</Link>
          )}
        </div>
      </div>
    </nav>
  );
}
