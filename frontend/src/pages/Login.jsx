import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import './Login.css';

export default function Login() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [isRegister, setIsRegister] = useState(false);
  const [form, setForm] = useState({ username: '', password: '', email: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isRegister) {
        await register(form.username, form.password, form.email);
        await login(form.username, form.password);
        navigate('/');
      } else {
        await login(form.username, form.password);
        navigate('/');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-grid">
        <aside className="login-aside">
          <Link to="/" className="login-brand">
            <span>The Library</span>
          </Link>
          <blockquote className="login-quote">
            <p>“A library is a hospital for the mind.”</p>
            <footer>— Anonymous, ancient Greek</footer>
          </blockquote>
        </aside>

        <main className="login-main">
          <div className="login-card slide-up">
            <header className="login-header">
              <p className="eyebrow">{isRegister ? 'New here?' : 'Welcome back'}</p>
              <h1 className="login-title">
                {isRegister ? 'Create your account' : 'Sign in to your account'}
              </h1>
              <p className="login-subtitle">
                {isRegister
                  ? 'Borrow up to five books at a time, free forever.'
                  : 'Enter your username and password to continue.'}
              </p>
            </header>

            <form onSubmit={handleSubmit} className="login-form">
              <div className="form-group">
                <label className="form-label">Username</label>
                <input
                  type="text"
                  className="input"
                  value={form.username}
                  onChange={(e) => setForm({ ...form, username: e.target.value })}
                  placeholder="your-username"
                  autoComplete="username"
                  required
                />
              </div>

              {isRegister && (
                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input
                    type="email"
                    className="input"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    placeholder="you@example.com"
                    autoComplete="email"
                    required
                  />
                </div>
              )}

              <div className="form-group">
                <label className="form-label">Password</label>
                <input
                  type="password"
                  className="input"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder="••••••••"
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                  required
                />
              </div>

              {error && <div className="form-error">{error}</div>}

              <button type="submit" className="btn btn-primary submit-btn" disabled={loading}>
                {loading ? 'Please wait…' : isRegister ? 'Create Account' : 'Sign In'}
              </button>
            </form>

            <div className="login-footer">
              {isRegister ? 'Already have an account?' : "Don't have an account?"}
              <button
                type="button"
                className="switch-btn"
                onClick={() => { setIsRegister(!isRegister); setError(''); }}
              >
                {isRegister ? 'Sign in' : 'Create one'}
              </button>
            </div>

            <div className="demo-accounts">
              <p className="demo-title">Demo accounts</p>
              <div className="demo-list">
                <button
                  type="button"
                  className="demo-btn"
                  onClick={() => setForm({ username: 'admin', password: 'admin123', email: 'admin@library.local' })}
                >
                  <span className="demo-role">Admin</span>
                  <span className="demo-creds">admin / admin123</span>
                </button>
                <button
                  type="button"
                  className="demo-btn"
                  onClick={() => setForm({ username: 'john', password: 'user123', email: 'john@library.local' })}
                >
                  <span className="demo-role">User</span>
                  <span className="demo-creds">john / user123</span>
                </button>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
