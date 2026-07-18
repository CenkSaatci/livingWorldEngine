import { useTranslation } from 'react-i18next';
import { useToastStore } from '../../store/toastStore';

const colorMap = {
  success: 'border-success bg-success/10',
  error: 'border-danger bg-danger/10',
  info: 'border-accent bg-accent/10',
};

export function ToastContainer() {
  const { t } = useTranslation('common');
  const toasts = useToastStore((s) => s.toasts);
  const removeToast = useToastStore((s) => s.removeToast);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`toast-slide flex items-start gap-3 rounded border-l-4 px-4 py-3 shadow-lg backdrop-blur-sm ${colorMap[toast.type]}`}
          role="alert"
        >
          <span className="flex-1 text-sm text-text-primary">{toast.message}</span>
          <button
            onClick={() => removeToast(toast.id)}
            className="text-text-secondary hover:text-text-primary transition-colors leading-none text-lg"
            aria-label={t('close')}
          >
            &times;
          </button>
        </div>
      ))}
    </div>
  );
}
