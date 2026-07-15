import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import type { AxiosError } from 'axios';
import { apiClient } from '../api/client';
import { useAuthStore } from '../store/authStore';
import { AuthForm } from '../components/auth/AuthForm';

export default function LoginPage() {
  const { t } = useTranslation('auth');
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [error, setError] = useState<string | null>(null);

  const handleLogin = async (fields: Record<string, string>) => {
    setError(null);
    try {
      const res = await apiClient.post('/auth/login', {
        email: fields.email,
        password: fields.password,
      });
      login({
        user: {
          id: res.data.id,
          email: res.data.email,
          username: res.data.username,
          role: res.data.role,
          locale: res.data.locale,
          emailVerified: res.data.emailVerified,
        },
        accessToken: res.data.accessToken,
        refreshToken: res.data.refreshToken,
      });
      navigate('/dashboard');
    } catch (err: unknown) {
      const axiosErr = err as AxiosError<{ error?: { code?: string } }>;
      const code = axiosErr.response?.data?.error?.code;
      setError(t(code ?? 'login.error_generic'));
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-2 text-center text-2xl font-heading text-text-primary">
          Living World Engine
        </h1>
        <p className="mb-8 text-center text-sm text-text-secondary">{t('login.title')}</p>

        <div className="rounded-lg bg-bg-surface p-6 shadow-lg">
          <AuthForm mode="login" onSubmit={handleLogin} error={error} />
        </div>

        <p className="mt-6 text-center text-sm text-text-secondary">
          <Link to="/forgot-password" className="text-text-secondary hover:text-accent">
            Forgot password?
          </Link>
          <span className="mx-2 text-text-secondary">·</span>
          <Link to="/register" className="text-accent hover:underline">
            {t('register.title')}
          </Link>
        </p>
      </div>
    </div>
  );
}
