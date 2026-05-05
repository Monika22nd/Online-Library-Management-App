import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../AuthContext';
import './BookDetail.css';

export default function BookDetail() {
  const { workId } = useParams();
  const { user } = useAuth();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [borrowMsg, setBorrowMsg] = useState('');

  useEffect(() => {
    loadBook();
  }, [workId]);

  async function loadBook() {
    setLoading(true);
    setError('');
    try {
      const data = await api.getWork(workId);
      setBook(data);
    } catch (err) {
      setError('Could not load book details.');
    } finally {
      setLoading(false);
    }
  }

  async function handleAddToCart() {
    if (!user) {
      setBorrowMsg('Please sign in to borrow books.');
      return;
    }
    try {
      // First import the book to local DB, then add to cart
      await api.importBooks(book.title, 5);
      // Try to find the book in local DB
      const localBooks = await api.listBooks(50, 0);
      const found = localBooks.books?.find(
        (b) => b.openlibrary_key === book.openlibrary_key
      );
      if (found) {
        await api.addToCart(found.id, user.id);
        setBorrowMsg('Added to cart! Go to your cart to checkout.');
      } else {
        setBorrowMsg('Book imported. Please try adding again.');
      }
    } catch (err) {
      setBorrowMsg('Failed to add to cart: ' + err.message);
    }
  }

  if (loading) {
    return (
      <div className="book-detail container">
        <div className="detail-skeleton">
          <div className="detail-cover-wrap">
            <div className="skeleton" style={{ width: '100%', aspectRatio: '2/3' }} />
          </div>
          <div className="detail-info">
            <div className="skeleton" style={{ height: 36, width: '70%', marginBottom: 16 }} />
            <div className="skeleton" style={{ height: 20, width: '40%', marginBottom: 32 }} />
            <div className="skeleton" style={{ height: 14, width: '100%', marginBottom: 8 }} />
            <div className="skeleton" style={{ height: 14, width: '90%', marginBottom: 8 }} />
            <div className="skeleton" style={{ height: 14, width: '95%', marginBottom: 8 }} />
          </div>
        </div>
      </div>
    );
  }

  if (error || !book) {
    return (
      <div className="book-detail container">
        <div className="detail-error">
          <h2>Book Not Found</h2>
          <p>{error || 'This book could not be loaded.'}</p>
          <Link to="/" className="back-btn">← Back to Home</Link>
        </div>
      </div>
    );
  }

  const coverUrl = book.cover_url || `https://covers.openlibrary.org/b/id/0-L.jpg`;
  const hasCover = !!book.cover_url;

  return (
    <div className="book-detail fade-in">
      <div className="detail-hero">
        {hasCover && <div className="detail-hero-blur" style={{ backgroundImage: `url(${coverUrl})` }} />}
        <div className="detail-hero-overlay" />
        <div className="detail-hero-content container">
          <div className="detail-cover-wrap">
            {hasCover ? (
              <img src={coverUrl} alt={book.title} className="detail-cover" />
            ) : (
              <div className="detail-cover-placeholder">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
                </svg>
              </div>
            )}
          </div>

          <div className="detail-info">
            <h1 className="detail-title">{book.title}</h1>
            <p className="detail-authors">
              by {book.authors?.join(', ') || 'Unknown Author'}
            </p>

            {book.first_publish_date && (
              <p className="detail-meta">
                <span className="meta-label">First Published</span>
                <span className="meta-value">{book.first_publish_date}</span>
              </p>
            )}

            <div className="detail-actions">
              <button className="borrow-btn" onClick={handleAddToCart}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
                </svg>
                Borrow This Book
              </button>
              <a
                href={`https://openlibrary.org${book.openlibrary_key}`}
                target="_blank"
                rel="noopener noreferrer"
                className="external-btn"
              >
                View on OpenLibrary ↗
              </a>
            </div>

            {borrowMsg && (
              <div className={`borrow-msg ${borrowMsg.includes('Failed') ? 'error' : 'success'}`}>
                {borrowMsg}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="detail-body container">
        <div className="detail-main">
          {/* Description */}
          <section className="detail-section">
            <h2 className="section-heading">Description</h2>
            <div className="description-text">
              {book.description || 'No description available for this work.'}
            </div>
          </section>

          {/* Excerpts */}
          {book.excerpts?.length > 0 && (
            <section className="detail-section">
              <h2 className="section-heading">Excerpts</h2>
              {book.excerpts.map((ex, i) => (
                <blockquote key={i} className="excerpt-block">
                  {typeof ex === 'string' ? ex : ex.excerpt || ex.value || ''}
                </blockquote>
              ))}
            </section>
          )}

          {/* Links */}
          {book.links?.length > 0 && (
            <section className="detail-section">
              <h2 className="section-heading">External Links</h2>
              <div className="links-list">
                {book.links.map((link, i) => (
                  <a key={i} href={link.url} target="_blank" rel="noopener noreferrer" className="ext-link">
                    {link.title || link.url} ↗
                  </a>
                ))}
              </div>
            </section>
          )}
        </div>

        <div className="detail-sidebar">
          {/* Subjects */}
          {book.subject_list?.length > 0 && (
            <section className="detail-section">
              <h2 className="section-heading">Subjects</h2>
              <div className="subject-tags">
                {book.subject_list.map((sub, i) => (
                  <Link
                    key={i}
                    to={`/subjects/${sub.toLowerCase().replace(/\s+/g, '_')}`}
                    className="subject-tag"
                  >
                    {sub}
                  </Link>
                ))}
              </div>
            </section>
          )}

          <section className="detail-section">
            <h2 className="section-heading">Details</h2>
            <div className="detail-meta-list">
              <div className="meta-row">
                <span className="meta-label">OpenLibrary ID</span>
                <span className="meta-value">{workId}</span>
              </div>
              {book.created && (
                <div className="meta-row">
                  <span className="meta-label">Added</span>
                  <span className="meta-value">{new Date(book.created).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
