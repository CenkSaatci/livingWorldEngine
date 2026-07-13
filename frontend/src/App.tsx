import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import WorldEditorPage from './pages/WorldEditorPage';
import CharacterSheetPage from './pages/CharacterSheetPage';
import InventoryPage from './pages/InventoryPage';
import GameView from './pages/GameView';
import AdminPage from './pages/AdminPage';

export default function App() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const restoreSession = useAuthStore((s) => s.restoreSession);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />}
        />
        <Route
          path="/register"
          element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <RegisterPage />}
        />
        <Route
          path="/dashboard"
          element={isAuthenticated ? <DashboardPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/worlds/:id"
          element={isAuthenticated ? <WorldEditorPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/characters/:id"
          element={isAuthenticated ? <CharacterSheetPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/characters/:id/inventory"
          element={isAuthenticated ? <InventoryPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/maps/:id"
          element={isAuthenticated ? <GameView /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/admin"
          element={isAuthenticated ? <AdminPage /> : <Navigate to="/login" replace />}
        />
        <Route
          path="*"
          element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />}
        />
      </Routes>
    </BrowserRouter>
  );
}
