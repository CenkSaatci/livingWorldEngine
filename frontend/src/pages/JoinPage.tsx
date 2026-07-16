import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckCircle, XCircle } from 'lucide-react';
import { apiClient } from '../api/client';

export default function JoinPage() {
  const { t } = useTranslation('common');
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'joining' | 'success' | 'error'>('joining');
  const [msg, setMsg] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setStatus('error');
      setMsg('No invite token provided');
      return;
    }
    apiClient
      .post(`/worlds/join?token=${token}`)
      .then(() => {
        setStatus('success');
        setMsg('You joined the world!');
      })
      .catch((err) => {
        setStatus('error');
        setMsg(err?.response?.data?.error?.message ?? 'Failed to join');
      });
  }, [searchParams]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary px-4">
      <div className="w-full max-w-sm text-center">
        {status === 'joining' && <p className="text-text-secondary">{t('status.loading')}</p>}
        {status === 'success' && (
          <div>
            <CheckCircle size={48} className="mx-auto mb-4 text-success" />
            <p className="mb-6 text-sm text-text-primary">{msg}</p>
            <button
              onClick={() => navigate('/dashboard')}
              className="rounded bg-accent px-6 py-2 text-white"
            >
              {t('welcome.start')}
            </button>
          </div>
        )}
        {status === 'error' && (
          <div>
            <XCircle size={48} className="mx-auto mb-4 text-danger" />
            <p className="mb-6 text-sm text-text-primary">{msg}</p>
            <button
              onClick={() => navigate('/dashboard')}
              className="rounded bg-accent px-6 py-2 text-white"
            >
              {t('welcome.start')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
