import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { apiClient } from '../api/client';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await apiClient.post('/auth/reset-password', { token, password });
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2000);
    } catch {
      setError('Invalid or expired token');
    }
  };

  if (success) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary px-4">
        <div className="w-full max-w-sm text-center">
          <h1 className="mb-2 text-2xl font-heading text-success">Password Reset!</h1>
          <p className="text-text-secondary">Redirecting to login…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-2 text-center text-2xl font-heading text-text-primary">Reset Password</h1>
        <p className="mb-6 text-center text-sm text-text-secondary">
          Enter the token from your email and a new password
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="Reset token"
            required
            className="w-full rounded border border-bg-elevated bg-bg-primary px-4 py-2.5 text-sm text-text-primary outline-none focus:border-accent"
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="New password (min 6 chars)"
            required
            minLength={6}
            className="w-full rounded border border-bg-elevated bg-bg-primary px-4 py-2.5 text-sm text-text-primary outline-none focus:border-accent"
          />

          <button
            type="submit"
            className="w-full rounded bg-accent py-2.5 text-sm font-semibold text-white hover:bg-accent/80"
          >
            Reset Password
          </button>
        </form>

        {error && <p className="mt-2 text-center text-sm text-danger">{error}</p>}

        <p className="mt-6 text-center text-sm text-text-secondary">
          <Link to="/login" className="text-accent hover:text-accent/80">
            Back to Login
          </Link>
        </p>
      </div>
    </div>
  );
}
