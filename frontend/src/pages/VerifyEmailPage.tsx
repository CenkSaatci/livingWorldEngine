import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckCircle, XCircle } from 'lucide-react';
import { apiClient } from '../api/client';
import { useAuthStore } from '../store/authStore';

export default function VerifyEmailPage() {
  const { t } = useTranslation('auth');
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'verifying' | 'success' | 'error'>('verifying');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setStatus('error');
      setErrorMsg('No verification token provided');
      return;
    }
    apiClient
      .post('/auth/verify-email', { token })
      .then(() => {
        setStatus('success');
        const u = useAuthStore.getState().user;
        if (u) {
          useAuthStore.getState().login({
            user: { ...u, emailVerified: true },
            accessToken: localStorage.getItem('lwe:accessToken') ?? '',
            refreshToken: localStorage.getItem('lwe:refreshToken') ?? '',
          });
        }
      })
      .catch((err) => {
        setStatus('error');
        setErrorMsg(err?.response?.data?.error ?? 'Verification failed');
      });
  }, [searchParams]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary px-4">
      <div className="w-full max-w-sm text-center">
        {status === 'verifying' && (
          <div>
            <div className="mb-4 animate-spin text-4xl text-accent">⏳</div>
            <p className="text-text-secondary">{t('verify.verifying')}</p>
          </div>
        )}
        {status === 'success' && (
          <div>
            <CheckCircle size={48} className="mx-auto mb-4 text-success" />
            <h1 className="mb-2 text-xl font-heading text-text-primary">{t('verify.success')}</h1>
            <p className="mb-6 text-sm text-text-secondary">{t('verify.successMsg')}</p>
            <button
              onClick={() => navigate('/dashboard')}
              className="rounded bg-accent px-6 py-2 text-sm text-white hover:bg-accent/80"
            >
              {t('verify.goToDashboard')}
            </button>
          </div>
        )}
        {status === 'error' && (
          <div>
            <XCircle size={48} className="mx-auto mb-4 text-danger" />
            <h1 className="mb-2 text-xl font-heading text-text-primary">{t('verify.error')}</h1>
            <p className="mb-6 text-sm text-text-secondary">{errorMsg}</p>
            <button
              onClick={() => navigate('/dashboard')}
              className="rounded bg-accent px-6 py-2 text-sm text-white hover:bg-accent/80"
            >
              {t('verify.goToDashboard')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
