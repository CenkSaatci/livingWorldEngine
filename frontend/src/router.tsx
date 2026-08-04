import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import WorldEditorPage from './pages/WorldEditorPage';
import CharacterSheetPage from './pages/CharacterSheetPage';
import InventoryPage from './pages/InventoryPage';
import GameView from './pages/GameView';
import AdminPage from './pages/AdminPage';
import CombatPage from './pages/CombatPage';
import FactionPage from './pages/FactionPage';
import NpcViewPage from './pages/NpcViewPage';
import MarketPage from './pages/MarketPage';
import SettingsPage from './pages/SettingsPage';
import QuestDetailPage from './pages/QuestDetailPage';
import RegionViewPage from './pages/RegionViewPage';
import LocationViewPage from './pages/LocationViewPage';
import GameSystemPage from './pages/GameSystemPage';
import CampaignDetailPage from './pages/CampaignDetailPage';
import MapEditorPage from './pages/MapEditorPage';
import AdventureEditorPage from './pages/AdventureEditorPage';
import AdventurePlayPage from './pages/AdventurePlayPage';
import EntityListPage from './pages/EntityListPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import WelcomePage from './pages/WelcomePage';
import JoinPage from './pages/JoinPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';

export function AppRoutes() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
      <Route path="/register" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <RegisterPage />} />
      <Route path="/dashboard" element={isAuthenticated ? <DashboardPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id" element={isAuthenticated ? <GameView /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/edit" element={isAuthenticated ? <WorldEditorPage /> : <Navigate to="/login" replace />} />
      <Route path="/maps/:id" element={<Navigate to="/dashboard" replace />} />
      <Route path="/characters/:id" element={isAuthenticated ? <CharacterSheetPage /> : <Navigate to="/login" replace />} />
      <Route path="/characters/:id/inventory" element={isAuthenticated ? <InventoryPage /> : <Navigate to="/login" replace />} />
      <Route path="/admin" element={isAuthenticated ? <AdminPage /> : <Navigate to="/login" replace />} />
      <Route path="/combat/:id" element={isAuthenticated ? <CombatPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:worldId/factions" element={isAuthenticated ? <FactionPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/npcs/:npcId" element={isAuthenticated ? <NpcViewPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/locations/:locationId/market" element={isAuthenticated ? <MarketPage /> : <Navigate to="/login" replace />} />
      <Route path="/settings" element={isAuthenticated ? <SettingsPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/quests/:questId" element={isAuthenticated ? <QuestDetailPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/regions/:regionId" element={isAuthenticated ? <RegionViewPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/locations/:locationId" element={isAuthenticated ? <LocationViewPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/map" element={isAuthenticated ? <MapEditorPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/adventures" element={isAuthenticated ? <AdventureEditorPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:worldId/adventures/:adventureId" element={isAuthenticated ? <AdventurePlayPage /> : <Navigate to="/login" replace />} />
      <Route path="/worlds/:id/entities" element={isAuthenticated ? <EntityListPage /> : <Navigate to="/login" replace />} />
      <Route path="/game-systems" element={isAuthenticated ? <GameSystemPage /> : <Navigate to="/login" replace />} />
      <Route path="/campaigns/:id" element={isAuthenticated ? <CampaignDetailPage /> : <Navigate to="/login" replace />} />
      <Route path="/forgot-password" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <ForgotPasswordPage />} />
      <Route path="/reset-password" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <ResetPasswordPage />} />
      <Route path="/verify-email" element={isAuthenticated ? <VerifyEmailPage /> : <Navigate to="/login" replace />} />
      <Route path="/welcome" element={isAuthenticated ? <WelcomePage /> : <Navigate to="/login" replace />} />
      <Route path="/join" element={isAuthenticated ? <JoinPage /> : <Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
    </Routes>
  );
}
