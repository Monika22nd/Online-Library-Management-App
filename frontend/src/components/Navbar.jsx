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
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const inputRef = useRef(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
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
    <nav className={`navbar ${scrolled ? 'scrolled' : ''}`}>
      <div className="navbar-inner container">
        <Link to="/" className="navbar-logo">
          <span className="logo-icon">📚</span>
          <span className="logo-text">OpenLibrary</span>
        </Link>

        <div className="navbar-subjects">
          {SUBJECTS.slice(0, 5).map((s) => (
            <Link key={s.slug} to={`/subjects/${s.slug}`} className="subject-link">
              {s.label}
            </Link>
          ))}
          <div className="subject-more">
            <button className="subject-link" onClick={() => setMenuOpen(!menuOpen)}>
              More ▾
            </button>
            {menuOpen && (
              <div className="subject-dropdown" onMouseLeave={() => setMenuOpen(false)}>
                {SUBJECTS.slice(5).map((s) => (
                  <Link
                    key={s.slug}
                    to={`/subjects/${s.slug}`}
                    className="dropdown-item"
                    onClick={() => setMenuOpen(false)}
                  >
                    {s.label}
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>

        <form className="navbar-search" onSubmit={handleSearch}>
          <svg className="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            ref={inputRef}
            type="text"
            placeholder="Search books, authors, ISBNs..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="search-input"
          />
        </form>

        <div className="navbar-actions">
          {user ? (
            <>
              <Link to="/cart" className="nav-action" title="Cart">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
                  <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6" />
                </svg>
              </Link>
              <Link to="/loans" className="nav-action" title="My Loans">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 016.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
                </svg>
              </Link>
              <div className="user-menu">
                <button className="user-btn">
                  <span className="user-avatar">{user.username[0].toUpperCase()}</span>
                </button>
                <div className="user-dropdown">
                  <div className="user-info">
                    <span className="user-name">{user.username}</span>
                    <span className="user-role">{user.role}</span>
                  </div>
                  <button className="dropdown-item" onClick={() => { logout(); navigate('/'); }}>
                    Sign Out
                  </button>
                </div>
              </div>
            </>
          ) : (
            <Link to="/login" className="nav-login-btn">Sign In</Link>
          )}
        </div>
      </div>
    </nav>
  );
}
