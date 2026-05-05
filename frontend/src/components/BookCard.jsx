import { Link } from 'react-router-dom';
import './BookCard.css';

const PLACEHOLDER = 'https://covers.openlibrary.org/b/id/0-M.jpg';

export default function BookCard({ book, style }) {
  const workId = book.openlibrary_key?.replace('/works/', '') || '';
  const coverUrl = book.cover_url || PLACEHOLDER;
  const hasCover = !!book.cover_url;

  return (
    <Link
      to={`/works/${workId}`}
      className="book-card"
      style={style}
    >
      <div className="book-card-cover-wrap">
        {hasCover ? (
          <img
            src={coverUrl}
            alt={book.title}
            className="book-card-cover"
            loading="lazy"
            onError={(e) => {
              e.target.style.display = 'none';
              e.target.nextElementSibling.style.display = 'flex';
            }}
          />
        ) : null}
        <div
          className="book-card-placeholder"
          style={{ display: hasCover ? 'none' : 'flex' }}
        >
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
          </svg>
        </div>
      </div>
      <div className="book-card-info">
        <h3 className="book-card-title">{book.title}</h3>
        <p className="book-card-author">
          {Array.isArray(book.authors)
            ? book.authors.slice(0, 2).join(', ')
            : book.author || 'Unknown'}
        </p>
        {book.publish_year && (
          <span className="book-card-year">{book.publish_year}</span>
        )}
      </div>
    </Link>
  );
}

export function BookCardSkeleton() {
  return (
    <div className="book-card book-card-skeleton">
      <div className="book-card-cover-wrap skeleton" />
      <div className="book-card-info">
        <div className="skeleton" style={{ height: 16, width: '80%', marginBottom: 8 }} />
        <div className="skeleton" style={{ height: 12, width: '60%' }} />
      </div>
    </div>
  );
}
