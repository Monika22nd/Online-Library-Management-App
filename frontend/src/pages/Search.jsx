import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api';
import BookCard, { BookCardSkeleton } from '../components/BookCard';
import './Search.css';

export default function Search() {
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const [results, setResults] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (query) {
      doSearch(query, 1);
    }
  }, [query]);

  async function doSearch(q, p) {
    setLoading(true);
    try {
      const data = await api.search(q, 24, p);
      setResults(data.books || []);
      setTotal(data.total || 0);
      setPage(p);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const totalPages = Math.ceil(total / 24);

  const changePage = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      doSearch(query, newPage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  return (
    <div className="search-page container">
      {query && (
        <div className="search-header slide-up">
          <h1 className="search-title">
            Results for "<span className="search-highlight">{query}</span>"
          </h1>
          <p className="search-meta">{total.toLocaleString()} books found</p>
        </div>
      )}

      {!query && (
        <div className="search-empty slide-up">
          <h1>Search the Library</h1>
          <p>Use the search bar above to find books by title, author, or ISBN.</p>
        </div>
      )}

      <div className="book-grid">
        {loading
          ? Array.from({ length: 12 }).map((_, i) => <BookCardSkeleton key={i} />)
          : results.map((book, i) => (
              <BookCard key={book.openlibrary_key || i} book={book} style={{ animationDelay: `${i * 0.03}s` }} />
            ))}
      </div>

      {!loading && results.length === 0 && query && (
        <div className="no-results">
          <p>No books found. Try a different search term.</p>
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button
            className="page-btn"
            disabled={page <= 1}
            onClick={() => changePage(page - 1)}
          >
            ← Previous
          </button>
          <div className="page-numbers">
            {Array.from({ length: Math.min(totalPages, 7) }).map((_, i) => {
              let p;
              if (totalPages <= 7) {
                p = i + 1;
              } else if (page <= 4) {
                p = i + 1;
              } else if (page >= totalPages - 3) {
                p = totalPages - 6 + i;
              } else {
                p = page - 3 + i;
              }
              return (
                <button
                  key={p}
                  className={`page-num ${p === page ? 'active' : ''}`}
                  onClick={() => changePage(p)}
                >
                  {p}
                </button>
              );
            })}
          </div>
          <button
            className="page-btn"
            disabled={page >= totalPages}
            onClick={() => changePage(page + 1)}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
