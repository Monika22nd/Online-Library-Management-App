import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api';
import BookCard, { BookCardSkeleton } from '../components/BookCard';
import './Home.css';

const FEATURED_SUBJECTS = [
  { label: 'Fiction', slug: 'fiction' },
  { label: 'Science Fiction', slug: 'science_fiction' },
  { label: 'Mystery', slug: 'mystery' },
  { label: 'History', slug: 'history' },
  { label: 'Fantasy', slug: 'fantasy' },
  { label: 'Romance', slug: 'romance' },
  { label: 'Biography', slug: 'biography' },
  { label: 'Philosophy', slug: 'philosophy' },
  { label: 'Art', slug: 'art' },
  { label: 'Poetry', slug: 'poetry' },
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
        api.getSubject('fiction', 6).catch(() => ({ books: [] })),
        api.getSubject('science_fiction', 6).catch(() => ({ books: [] })),
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

  const featured = trending[0];

  return (
    <div className="home">
      {/* ─── Hero ─── */}
      <section className="hero container">
        <div className="hero-grid">
          <div className="hero-text">
            <p className="eyebrow hero-eyebrow">Est. 2024 — Open to all</p>
            <h1 className="hero-title">
              Discover your next<br/>
              <em>great read.</em>
            </h1>
            <p className="hero-subtitle">
              Browse twenty million books from the world's largest open library.
              Search the catalog, borrow titles for fourteen days, and read freely.
            </p>
            <form className="hero-search" onSubmit={handleHeroSearch}>
              <input
                type="text"
                className="hero-search-input"
                placeholder="Search by title, author, or ISBN…"
                value={heroQuery}
                onChange={(e) => setHeroQuery(e.target.value)}
              />
              <button type="submit" className="btn btn-primary">Search</button>
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
                <span className="stat-num">14d</span>
                <span className="stat-label">Loan</span>
              </div>
            </div>
          </div>

          <div className="hero-featured">
            {featured ? (
              <Link
                to={`/works/${featured.openlibrary_key?.replace('/works/', '')}`}
                className="featured-book"
              >
                <span className="eyebrow">Trending today</span>
                <div className="featured-cover-wrap">
                  {featured.cover_url ? (
                    <img src={featured.cover_url} alt={featured.title} className="featured-cover" />
                  ) : (
                    <div className="featured-cover-placeholder" />
                  )}
                </div>
                <h3 className="featured-title">{featured.title}</h3>
                {featured.author && <p className="featured-author">by {featured.author}</p>}
              </Link>
            ) : (
              <div className="featured-book featured-loading">
                <div className="skeleton" style={{ width: '100%', aspectRatio: '2/3' }} />
              </div>
            )}
          </div>
        </div>
      </section>

      {/* ─── Subject chips ─── */}
      <section className="container">
        <div className="section-header">
          <h2 className="section-title">Browse by Subject</h2>
        </div>
        <div className="subject-chips">
          {FEATURED_SUBJECTS.map((s) => (
            <Link key={s.slug} to={`/subjects/${s.slug}`} className="subject-chip">
              {s.label}
            </Link>
          ))}
        </div>
      </section>

      {/* ─── Trending ─── */}
      <section className="container">
        <div className="section-header">
          <h2 className="section-title">Trending Today</h2>
          <Link to="/search?q=trending" className="see-all">View all →</Link>
        </div>
        <div className="book-grid">
          {loading
            ? Array.from({ length: 6 }).map((_, i) => <BookCardSkeleton key={i} />)
            : trending.slice(1, 13).map((book, i) => (
                <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.04}s` }} />
              ))}
        </div>
      </section>

      {/* ─── Fiction ─── */}
      {subjectBooks.fiction?.length > 0 && (
        <section className="container">
          <div className="section-header">
            <h2 className="section-title">Popular Fiction</h2>
            <Link to="/subjects/fiction" className="see-all">View all →</Link>
          </div>
          <div className="book-grid">
            {subjectBooks.fiction.map((book, i) => (
              <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.04}s` }} />
            ))}
          </div>
        </section>
      )}

      {/* ─── Sci-Fi ─── */}
      {subjectBooks.science_fiction?.length > 0 && (
        <section className="container">
          <div className="section-header">
            <h2 className="section-title">Science Fiction</h2>
            <Link to="/subjects/science_fiction" className="see-all">View all →</Link>
          </div>
          <div className="book-grid">
            {subjectBooks.science_fiction.map((book, i) => (
              <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.04}s` }} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
