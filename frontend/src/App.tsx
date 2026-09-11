import { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { useSettingsStore } from './store/settingsStore';
import { useWorldStore } from './store/worldStore';
import { useSessionStore } from './store/sessionStore';
import { useCombatStore } from './store/combatStore';
import { AppRoutes } from './router';
import { ToastContainer } from './components/ui/Toast';
import { ErrorBoundary } from './components/ui/ErrorBoundary';

const routerFuture = { v7_startTransition: true } as const;

export default function App() {
  const restoreSession = useAuthStore((s) => s.restoreSession);
  const theme = useSettingsStore((s) => s.theme);

  useEffect(() => {
    if (restoreSession()) {
      useWorldStore.getState().rehydrateCurrentWorld();
      useSessionStore.getState().rehydrateSessions();
      useCombatStore.getState().rehydrateCombat();
    }
  }, [restoreSession]);

  useEffect(() => {
    const html = document.documentElement;
    html.classList.remove('dark', 'theme-cyber');
    if (theme === 'dark') html.classList.add('dark');
    else if (theme === 'cyber') html.classList.add('theme-cyber');
  }, [theme]);

  return (
    <BrowserRouter future={routerFuture}>
      <ErrorBoundary>
        <AppRoutes />
        <ToastContainer />
      </ErrorBoundary>
    </BrowserRouter>
  );
}
