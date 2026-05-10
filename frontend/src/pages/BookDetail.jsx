import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../AuthContext';
import './BookDetail.css';

export default function BookDetail() {
  const { workId } = useParams();
  const { user } = useAuth();
  const [book, setBook] = useState(null);
  const [localBook, setLocalBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [borrowState, setBorrowState] = useState({ status: 'idle', message: '' });

  useEffect(() => {
    loadBook();
  }, [workId]);

  async function loadBook() {
    setLoading(true);
    setError('');
    setLocalBook(null);
    try {
      const data = await api.getWork(workId);
      setBook(data);
      // Try to find a local mirror so we can surface availability + add-to-cart
      // without round-tripping import. Cheap best-effort lookup.
      try {
        const local = await api.listBooks(200, 0);
        const match = local.books?.find(
          (b) => b.openlibrary_key === data.openlibrary_key
        );
        if (match) setLocalBook(match);
      } catch { /* non-fatal */ }
    } catch (err) {
      setError('Could not load book details.');
    } finally {
      setLoading(false);
    }
  }

  async function handleBorrow() {
    if (!user) {
      setBorrowState({ status: 'error', message: 'Please sign in to borrow books.' });
      return;
    }
    setBorrowState({ status: 'pending', message: 'Adding to cart…' });
    try {
      let target = localBook;
      if (!target) {
        target = await api.importOne(book.openlibrary_key);
        if (target) setLocalBook(target);
      }
      if (!target) {
        setBorrowState({ status: 'error', message: 'Could not import this book to the catalog.' });
        return;
      }
      await api.addToCart(target.id, user.id);
      setBorrowState({ status: 'success', message: 'Added to your cart.' });
    } catch (err) {
      setBorrowState({ status: 'error', message: 'Failed: ' + err.message });
    }
  }

  if (loading) {
    return (
      <div className="detail-page container">
        <div className="detail-skeleton">
          <div className="skeleton" style={{ width: 240, aspectRatio: '2/3' }} />
          <div style={{ flex: 1 }}>
            <div className="skeleton" style={{ height: 14, width: 120, marginBottom: 16 }} />
            <div className="skeleton" style={{ height: 36, width: '70%', marginBottom: 16 }} />
            <div className="skeleton" style={{ height: 18, width: '40%', marginBottom: 32 }} />
            <div className="skeleton" style={{ height: 12, width: '100%', marginBottom: 8 }} />
            <div className="skeleton" style={{ height: 12, width: '95%', marginBottom: 8 }} />
            <div className="skeleton" style={{ height: 12, width: '92%' }} />
          </div>
        </div>
      </div>
    );
  }

  if (error || !book) {
    return (
      <div className="detail-page container">
        <div className="detail-error">
          <h2>Not found</h2>
          <p>{error || 'This book could not be loaded.'}</p>
          <Link to="/" className="btn btn-secondary">← Back to Home</Link>
        </div>
      </div>
    );
  }

  const hasCover = !!book.cover_url;
  const authorList = book.authors && book.authors.length ? book.authors : null;

  // localBook fields (when present): { id, available_copies, total_copies, subjects }
  const available = localBook?.available_copies ?? null;
  const total = localBook?.total_copies ?? null;
  const inCatalog = !!localBook;

  return (
    <article className="detail-page fade-in">
      <div className="container">
        {/* ─── Header block ─── */}
        <header className="detail-header">
          <div className="detail-cover-col">
            <div className="detail-cover-wrap">
              {hasCover ? (
                <img src={book.cover_url} alt={book.title} className="detail-cover" />
              ) : (
                <div className="detail-cover-placeholder">
                  <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
                  </svg>
                </div>
              )}
            </div>
          </div>

          <div className="detail-meta-col">
            <p className="eyebrow">Work · {workId}</p>
            <h1 className="detail-title">{book.title}</h1>
            {authorList && (
              <p className="detail-authors">
                by {authorList.map((a, i) => (
                  <span key={i}>
                    <span className="author-name">{a}</span>
                    {i < authorList.length - 1 ? ', ' : ''}
                  </span>
                ))}
              </p>
            )}

            <div className="detail-facts">
              {book.first_publish_date && (
                <div className="fact">
                  <span className="fact-label">First published</span>
                  <span className="fact-value">{book.first_publish_date}</span>
                </div>
              )}
              {inCatalog && (
                <div className="fact">
                  <span className="fact-label">In catalog</span>
                  <span className="fact-value">
                    <span className={`avail-pill ${available > 0 ? 'avail-on' : 'avail-off'}`}>
                      {available > 0 ? `${available} of ${total} available` : `${total} copies, none available`}
                    </span>
                  </span>
                </div>
              )}
            </div>

            <div className="detail-actions">
              <button
                className="btn btn-primary"
                onClick={handleBorrow}
                disabled={borrowState.status === 'pending' || (inCatalog && available === 0)}
              >
                {borrowState.status === 'pending' ? 'Adding…'
                  : inCatalog ? 'Add to Cart'
                  : 'Request Borrow'}
              </button>
              <a
                href={`https://openlibrary.org${book.openlibrary_key}`}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-secondary"
              >
                View on OpenLibrary ↗
              </a>
            </div>

            {borrowState.message && (
              <div className={`detail-msg msg-${borrowState.status}`}>
                {borrowState.message}
              </div>
            )}
          </div>
        </header>

        {/* ─── Body ─── */}
        <div className="detail-body">
          <div className="detail-main">
            <section className="detail-section">
              <h2 className="section-heading">About this book</h2>
              <p className="description-text">
                {book.description || 'No description available for this work.'}
              </p>
            </section>

            {book.excerpts?.length > 0 && (
              <section className="detail-section">
                <h2 className="section-heading">Excerpts</h2>
                {book.excerpts.map((ex, i) => (
                  <blockquote key={i} className="excerpt">
                    {typeof ex === 'string' ? ex : ex.excerpt || ex.value || ''}
                  </blockquote>
                ))}
              </section>
            )}

            {book.links?.length > 0 && (
              <section className="detail-section">
                <h2 className="section-heading">External links</h2>
                <ul className="links-list">
                  {book.links.map((link, i) => (
                    <li key={i}>
                      <a href={link.url} target="_blank" rel="noopener noreferrer" className="ext-link">
                        {link.title || link.url} ↗
                      </a>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </div>

          <aside className="detail-sidebar">
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
              <dl className="detail-dl">
                <div className="dl-row">
                  <dt>OpenLibrary ID</dt>
                  <dd>{workId}</dd>
                </div>
                {book.created && (
                  <div className="dl-row">
                    <dt>Catalogued</dt>
                    <dd>{new Date(book.created).toLocaleDateString()}</dd>
                  </div>
                )}
                {inCatalog && (
                  <div className="dl-row">
                    <dt>Local copies</dt>
                    <dd>{total} <span className="dim">({available} available)</span></dd>
                  </div>
                )}
              </dl>
            </section>

            {localBook?.editions?.length > 0 && (
              <section className="detail-section">
                <h2 className="section-heading">Editions</h2>
                <ul className="editions-list">
                  {localBook.editions.map((ed) => (
                    <li key={ed.edition_id} className="edition-row">
                      <div className="edition-title">{ed.title}</div>
                      <div className="edition-meta">
                        {ed.publish_date && <span>{ed.publish_date}</span>}
                        {ed.isbn_13 && <span>ISBN {ed.isbn_13}</span>}
                        {ed.copies?.length > 0 && (
                          <span>{ed.copies.filter(c => c.circulation_status === 'available').length} / {ed.copies.length} avail</span>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </aside>
        </div>
      </div>
    </article>
  );
}
