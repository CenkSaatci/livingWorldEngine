import { type FormEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';

interface AuthFormProps {
  mode: 'login' | 'register';
  onSubmit: (data: Record<string, string>) => Promise<void>;
  error: string | null;
}

export function AuthForm({ mode, onSubmit, error }: AuthFormProps) {
  const { t } = useTranslation('auth');
  const [fields, setFields] = useState({ email: '', username: '', password: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onSubmit(fields);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      {error && (
        <div className="rounded bg-danger/20 p-3 text-sm text-danger" role="alert">
          {error}
        </div>
      )}

      <div>
        <label htmlFor="email" className="block text-sm font-medium text-text-secondary mb-1">
          {t('login.email_label')}
        </label>
        <input
          id="email"
          type="email"
          required
          autoComplete="email"
          value={fields.email}
          onChange={(e) => setFields({ ...fields, email: e.target.value })}
          className="w-full rounded border border-bg-elevated bg-bg-surface px-3 py-2 text-text-primary
                     placeholder:text-text-secondary/50 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
          placeholder="email@example.com"
        />
      </div>

      {mode === 'register' && (
        <div>
          <label htmlFor="username" className="block text-sm font-medium text-text-secondary mb-1">
            {t('register.username_label')}
          </label>
          <input
            id="username"
            type="text"
            required
            minLength={3}
            maxLength={100}
            value={fields.username}
            onChange={(e) => setFields({ ...fields, username: e.target.value })}
            className="w-full rounded border border-bg-elevated bg-bg-surface px-3 py-2 text-text-primary
                       placeholder:text-text-secondary/50 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
            placeholder="hero"
          />
        </div>
      )}

      <div>
        <label htmlFor="password" className="block text-sm font-medium text-text-secondary mb-1">
          {t('login.password_label')}
        </label>
        <input
          id="password"
          type="password"
          required
          minLength={6}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          value={fields.password}
          onChange={(e) => setFields({ ...fields, password: e.target.value })}
          className="w-full rounded border border-bg-elevated bg-bg-surface px-3 py-2 text-text-primary
                     placeholder:text-text-secondary/50 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
          placeholder="&bull;&bull;&bull;&bull;&bull;&bull;"
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded bg-accent px-4 py-2 font-semibold text-white transition
                   hover:bg-accent/80 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {loading
          ? t('status.loading', { ns: 'common' })
          : t(mode === 'login' ? 'login.submit' : 'register.submit')}
      </button>
    </form>
  );
}
