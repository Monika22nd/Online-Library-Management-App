import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api';
import BookCard, { BookCardSkeleton } from '../components/BookCard';
import './Home.css';

const FEATURED_SUBJECTS = [
  { label: 'Fiction', slug: 'fiction', emoji: '📖' },
  { label: 'Science Fiction', slug: 'science_fiction', emoji: '🚀' },
  { label: 'Mystery', slug: 'mystery', emoji: '🔍' },
  { label: 'History', slug: 'history', emoji: '🏛️' },
  { label: 'Fantasy', slug: 'fantasy', emoji: '🧙' },
  { label: 'Romance', slug: 'romance', emoji: '💕' },
  { label: 'Biography', slug: 'biography', emoji: '👤' },
  { label: 'Philosophy', slug: 'philosophy', emoji: '🤔' },
  { label: 'Art', slug: 'art', emoji: '🎨' },
  { label: 'Poetry', slug: 'poetry', emoji: '✍️' },
];

export default function Home() {
  const navigate = useNavigate();
  const [trending, setTrending] = useState([]);
  const [subjectBooks, setSubjectBooks] = useState({});
  const [loading, setLoading] = useState(true);
  const [heroQuery, setHeroQuery] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const [trendingData, fictionData, scifiData] = await Promise.all([
        api.trending(12).catch(() => []),
        api.getSubject('fiction', 12).catch(() => ({ books: [] })),
        api.getSubject('science_fiction', 12).catch(() => ({ books: [] })),
      ]);
      setTrending(trendingData);
      setSubjectBooks({
        fiction: fictionData.books || [],
        science_fiction: scifiData.books || [],
      });
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const handleHeroSearch = (e) => {
    e.preventDefault();
    if (heroQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(heroQuery.trim())}`);
    }
  };

  return (
    <div className="home">
      {/* Hero */}
      <section className="hero">
        <div className="hero-bg" />
        <div className="hero-content container">
          <h1 className="hero-title">
            Discover Your Next
            <span className="hero-accent"> Great Read</span>
          </h1>
          <p className="hero-subtitle">
            Explore millions of books from the world's largest open library.
            Search, borrow, and dive into your next adventure.
          </p>
          <form className="hero-search" onSubmit={handleHeroSearch}>
            <input
              type="text"
              className="hero-search-input"
              placeholder="Search by title, author, or ISBN..."
              value={heroQuery}
              onChange={(e) => setHeroQuery(e.target.value)}
            />
            <button type="submit" className="hero-search-btn">
              Search
            </button>
          </form>
          <div className="hero-stats">
            <div className="stat">
              <span className="stat-num">20M+</span>
              <span className="stat-label">Books</span>
            </div>
            <div className="stat-divider" />
            <div className="stat">
              <span className="stat-num">6M+</span>
              <span className="stat-label">Authors</span>
            </div>
            <div className="stat-divider" />
            <div className="stat">
              <span className="stat-num">Free</span>
              <span className="stat-label">Forever</span>
            </div>
          </div>
        </div>
      </section>

      {/* Subject Chips */}
      <section className="section container">
        <h2 className="section-title">Browse by Subject</h2>
        <div className="subject-chips">
          {FEATURED_SUBJECTS.map((s) => (
            <Link key={s.slug} to={`/subjects/${s.slug}`} className="subject-chip">
              <span className="chip-emoji">{s.emoji}</span>
              <span>{s.label}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* Trending */}
      <section className="section container">
        <div className="section-header">
          <h2 className="section-title">🔥 Trending Today</h2>
        </div>
        <div className="book-grid">
          {loading
            ? Array.from({ length: 6 }).map((_, i) => <BookCardSkeleton key={i} />)
            : trending.map((book, i) => (
                <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.05}s` }} />
              ))}
        </div>
      </section>

      {/* Fiction */}
      {subjectBooks.fiction?.length > 0 && (
        <section className="section container">
          <div className="section-header">
            <h2 className="section-title">📖 Popular Fiction</h2>
            <Link to="/subjects/fiction" className="see-all">View All →</Link>
          </div>
          <div className="book-grid">
            {subjectBooks.fiction.map((book, i) => (
              <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.05}s` }} />
            ))}
          </div>
        </section>
      )}

      {/* Sci-Fi */}
      {subjectBooks.science_fiction?.length > 0 && (
        <section className="section container">
          <div className="section-header">
            <h2 className="section-title">🚀 Science Fiction</h2>
            <Link to="/subjects/science_fiction" className="see-all">View All →</Link>
          </div>
          <div className="book-grid">
            {subjectBooks.science_fiction.map((book, i) => (
              <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.05}s` }} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
