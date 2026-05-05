const API_BASE = '/api';

async function request(url, options = {}) {
  const res = await fetch(`${API_BASE}${url}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: 'Request failed' }));
    throw new Error(err.detail || 'Request failed');
  }
  return res.json();
}

export const api = {
  // OpenLibrary proxy
  search: (q, limit = 20, page = 1) =>
    request(`/search?q=${encodeURIComponent(q)}&limit=${limit}&page=${page}`),

  trending: (limit = 20) =>
    request(`/trending?limit=${limit}`),

  getWork: (workId) =>
    request(`/works/${workId}`),

  getSubject: (subject, limit = 20, offset = 0) =>
    request(`/subjects/${subject}?limit=${limit}&offset=${offset}`),

  getAuthor: (authorKey) =>
    request(`/authors/${authorKey}`),

  // Local books
  listBooks: (limit = 50, offset = 0) =>
    request(`/books?limit=${limit}&offset=${offset}`),

  getBook: (id) =>
    request(`/books/${id}`),

  importBooks: (query = 'programming', limit = 50) =>
    request(`/books/import?query=${query}&limit=${limit}`, { method: 'POST' }),

  // Auth
  login: (data) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify(data) }),

  register: (data) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify(data) }),

  // Cart
  addToCart: (bookId, userId) =>
    request(`/cart/add/${bookId}?user_id=${userId}`, { method: 'POST' }),

  removeFromCart: (bookId, userId) =>
    request(`/cart/remove/${bookId}?user_id=${userId}`, { method: 'DELETE' }),

  getCart: (userId) =>
    request(`/cart?user_id=${userId}`),

  checkout: (userId) =>
    request(`/cart/checkout?user_id=${userId}`, { method: 'POST' }),

  // Loans
  getUserLoans: (userId, status) => {
    const q = status ? `?status=${status}` : '';
    return request(`/loans/user/${userId}${q}`);
  },

  returnLoan: (loanId) =>
    request(`/loans/${loanId}/return`, { method: 'POST' }),

  cancelLoan: (loanId) =>
    request(`/loans/${loanId}/cancel`, { method: 'POST' }),

  // Admin
  getAllLoans: (status) => {
    const q = status ? `?status=${status}` : '';
    return request(`/loans${q}`);
  },

  approveLoan: (loanId, adminId) =>
    request(`/loans/${loanId}/approve?admin_id=${adminId}`, { method: 'POST' }),

  rejectLoan: (loanId) =>
    request(`/loans/${loanId}/cancel`, { method: 'POST' }),

  bulkApproveLoans: (loanIds, adminId) =>
    request(`/loans/bulk-approve?admin_id=${adminId}`, {
      method: 'POST',
      body: JSON.stringify({ loan_ids: loanIds }),
    }),

  getAuditLog: (limit = 50) =>
    request(`/audit?limit=${limit}`),

  // Health
  health: () => request('/health'),
};

export default api;
