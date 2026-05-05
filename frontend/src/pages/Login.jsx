import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
        setIsRegister(false);
        setError('');
        setForm(prev => ({ ...prev, password: '' }));
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
      <div className="login-card slide-up">
        <div className="login-header">
          <h1 className="login-title">{isRegister ? 'Create Account' : 'Welcome Back'}</h1>
          <p className="login-subtitle">
            {isRegister ? 'Join the library community' : 'Sign in to your account'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label className="form-label">Username</label>
            <input
              type="text"
              className="form-input"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              placeholder="Enter your username"
              required
            />
          </div>

          {isRegister && (
            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                type="email"
                className="form-input"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="Enter your email"
                required
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              className="form-input"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="Enter your password"
              required
            />
          </div>

          {error && <div className="form-error">{error}</div>}

          <button type="submit" className="submit-btn" disabled={loading}>
            {loading ? 'Please wait...' : isRegister ? 'Create Account' : 'Sign In'}
          </button>
        </form>

        <div className="login-footer">
          <p>
            {isRegister ? 'Already have an account?' : "Don't have an account?"}
            <button className="switch-btn" onClick={() => { setIsRegister(!isRegister); setError(''); }}>
              {isRegister ? 'Sign In' : 'Create Account'}
            </button>
          </p>
        </div>

        <div className="demo-accounts">
          <p className="demo-title">Demo Accounts</p>
          <div className="demo-list">
            <button className="demo-btn" onClick={() => setForm({ username: 'admin', password: 'admin123', email: '' })}>
              Admin (admin / admin123)
            </button>
            <button className="demo-btn" onClick={() => setForm({ username: 'john', password: 'user123', email: '' })}>
              User (john / user123)
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
