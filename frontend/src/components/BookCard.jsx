import { Link } from 'react-router-dom';
import './BookCard.css';

export default function BookCard({ book, style }) {
  const workId = book.openlibrary_key?.replace('/works/', '') || '';
  const hasCover = !!book.cover_url;
  const author = Array.isArray(book.authors)
    ? book.authors.slice(0, 2).join(', ')
    : (book.author || '');

  return (
    <Link to={`/works/${workId}`} className="book-card" style={style}>
      <div className="book-card-cover-wrap">
        {hasCover ? (
          <img
            src={book.cover_url}
            alt={book.title}
            className="book-card-cover"
            loading="lazy"
            onError={(e) => {
              e.currentTarget.style.display = 'none';
              e.currentTarget.nextElementSibling.style.display = 'flex';
            }}
          />
        ) : null}
        <div
          className="book-card-placeholder"
          style={{ display: hasCover ? 'none' : 'flex' }}
        >
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
          </svg>
        </div>
      </div>
      <div className="book-card-info">
        <h3 className="book-card-title">{book.title}</h3>
        {author && <p className="book-card-author">{author}</p>}
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
      <div className="book-card-cover-wrap">
        <div className="skeleton" style={{ width: '100%', height: '100%' }} />
      </div>
      <div className="book-card-info">
        <div className="skeleton" style={{ height: 14, width: '85%', marginBottom: 8 }} />
        <div className="skeleton" style={{ height: 11, width: '60%' }} />
      </div>
    </div>
  );
}
