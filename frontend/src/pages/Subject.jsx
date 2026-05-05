import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import api from '../api';
import BookCard, { BookCardSkeleton } from '../components/BookCard';
import './Subject.css';

export default function Subject() {
  const { subject } = useParams();
  const [books, setBooks] = useState([]);
  const [total, setTotal] = useState(0);
  const [subjectName, setSubjectName] = useState('');
  const [loading, setLoading] = useState(true);
  const [offset, setOffset] = useState(0);
  const limit = 24;

  useEffect(() => {
    setOffset(0);
    loadSubject(0);
  }, [subject]);

  async function loadSubject(off) {
    setLoading(true);
    try {
      const data = await api.getSubject(subject, limit, off);
      setBooks(data.books || []);
      setTotal(data.total || 0);
      setSubjectName(data.subject || subject);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const page = Math.floor(offset / limit) + 1;
  const totalPages = Math.ceil(total / limit);

  const changePage = (newPage) => {
    const newOffset = (newPage - 1) * limit;
    setOffset(newOffset);
    loadSubject(newOffset);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const displayName = subjectName.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());

  return (
    <div className="subject-page container">
      <div className="subject-header slide-up">
        <h1 className="subject-page-title">{displayName}</h1>
        <p className="subject-count">{total.toLocaleString()} books</p>
      </div>

      <div className="book-grid">
        {loading
          ? Array.from({ length: 12 }).map((_, i) => <BookCardSkeleton key={i} />)
          : books.map((book, i) => (
              <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.03}s` }} />
            ))}
      </div>

      {!loading && books.length === 0 && (
        <div className="no-results">
          <p>No books found for this subject.</p>
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button className="page-btn" disabled={page <= 1} onClick={() => changePage(page - 1)}>← Previous</button>
          <span className="page-info">Page {page} of {totalPages}</span>
          <button className="page-btn" disabled={page >= totalPages} onClick={() => changePage(page + 1)}>Next →</button>
        </div>
      )}
    </div>
  );
}
