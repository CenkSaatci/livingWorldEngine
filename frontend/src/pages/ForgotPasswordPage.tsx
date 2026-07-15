import { useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [result, setResult] = useState<{ message: string; token?: string } | null>(null);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setResult(null);
    try {
      const res = await apiClient.post('/auth/forgot-password', { email });
      setResult(res.data);
    } catch {
      setError('Failed to request reset');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-2 text-center text-2xl font-heading text-text-primary">
          Forgot Password
        </h1>
        <p className="mb-6 text-center text-sm text-text-secondary">
          Enter your email to receive a reset token
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Your email address"
            required
            className="w-full rounded border border-bg-elevated bg-bg-primary px-4 py-2.5 text-sm text-text-primary outline-none focus:border-accent"
          />

          <button
            type="submit"
            className="w-full rounded bg-accent py-2.5 text-sm font-semibold text-white hover:bg-accent/80"
          >
            Send Reset Token
          </button>
        </form>

        {result && (
          <div className="mt-4 rounded bg-accent/10 p-3 text-sm">
            <p className="text-text-primary mb-2">{result.message}</p>
            {result.token && (
              <div className="flex gap-2">
                <code className="flex-1 rounded bg-bg-elevated px-2 py-1 text-xs text-accent break-all">
                  {result.token}
                </code>
                <button
                  onClick={() => navigator.clipboard.writeText(result.token ?? '')}
                  className="shrink-0 rounded bg-accent px-2 py-1 text-xs text-white"
                >
                  Copy
                </button>
              </div>
            )}
          </div>
        )}

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
