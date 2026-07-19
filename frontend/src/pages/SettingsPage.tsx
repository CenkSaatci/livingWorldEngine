import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Monitor, Volume2, VolumeX } from 'lucide-react';
import { useSettingsStore, type DiceMode, type ThemeMode } from '../store/settingsStore';
import { useAuthStore } from '../store/authStore';
import { apiClient } from '../api/client';
import { isSoundEnabled, setSoundEnabled } from '../utils/sound';

export default function SettingsPage() {
  const navigate = useNavigate();
  const { i18n } = useTranslation('common');
  const diceMode = useSettingsStore((s) => s.diceMode);
  const setDiceMode = useSettingsStore((s) => s.setDiceMode);
  const theme = useSettingsStore((s) => s.theme);
  const setTheme = useSettingsStore((s) => s.setTheme);
  const user = useAuthStore((s) => s.user);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [passwordMsg, setPasswordMsg] = useState('');
  const [passwordError, setPasswordError] = useState(false);

  const handleChangePassword = async () => {
    setPasswordMsg('');
    setPasswordError(false);
    if (!currentPassword || !newPassword) {
      setPasswordMsg('Fill both fields');
      setPasswordError(true);
      return;
    }
    if (newPassword.length < 6) {
      setPasswordMsg('Password too short');
      setPasswordError(true);
      return;
    }
    try {
      await apiClient.post('/auth/change-password', { currentPassword, newPassword });
      setPasswordMsg('Password changed');
      setCurrentPassword('');
      setNewPassword('');
    } catch {
      setPasswordMsg('Failed to change password');
      setPasswordError(true);
    }
  };

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="flex items-center gap-2 text-lg font-heading text-text-primary">
          <Monitor size={20} /> Settings
        </h1>
      </header>

      <main className="mx-auto max-w-2xl space-y-6 p-6">
        {/* Language */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">Language / Sprache</h2>
          <div className="flex gap-2">
            {['de', 'en'].map((lang) => (
              <button
                key={lang}
                onClick={() => {
                  i18n.changeLanguage(lang);
                  useAuthStore.getState().setLocale(lang);
                }}
                className={`rounded px-4 py-2 text-sm ${
                  i18n.language === lang
                    ? 'bg-accent text-white'
                    : 'bg-bg-elevated text-text-secondary hover:text-text-primary'
                }`}
              >
                {lang === 'de' ? 'Deutsch' : 'English'}
              </button>
            ))}
          </div>
        </section>

        {/* Dice Animation Mode */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">🎲 Dice Animation</h2>
          <p className="mb-3 text-xs text-text-secondary">
            CSS = lightweight, no dependencies. 3D = three.js rendered dice.
          </p>
          <div className="flex gap-2">
            {(['css', '3d'] as DiceMode[]).map((mode) => (
              <button
                key={mode}
                onClick={() => setDiceMode(mode)}
                className={`rounded px-4 py-2 text-sm ${
                  diceMode === mode
                    ? 'bg-accent text-white'
                    : 'bg-bg-elevated text-text-secondary hover:text-text-primary'
                }`}
              >
                {mode === 'css' ? '🎲 CSS Dice' : '🧊 3D Dice'}
              </button>
            ))}
          </div>
        </section>

        {/* Theme */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">Theme</h2>
          <select
            value={theme}
            onChange={(e) => setTheme(e.target.value as ThemeMode)}
            className="rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
          >
            <option value="dark">🌙 Dark</option>
            <option value="light">☀️ Light</option>
          </select>
        </section>

        {/* Account */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">Account</h2>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-text-secondary">Username</span>
              <span className="text-text-primary">{user?.username ?? '—'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-text-secondary">Role</span>
              <span className="text-text-primary">{user?.role ?? '—'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-text-secondary">Locale</span>
              <span className="text-text-primary">{user?.locale ?? '—'}</span>
            </div>
          </div>
        </section>

        {/* Sound */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">Sound</h2>
          <button
            onClick={() => setSoundEnabled(!isSoundEnabled())}
            className={`flex items-center gap-2 rounded px-3 py-2 text-sm ${
              isSoundEnabled() ? 'bg-accent/10 text-accent' : 'bg-bg-elevated text-text-secondary'
            }`}
          >
            {isSoundEnabled() ? <Volume2 size={16} /> : <VolumeX size={16} />}
            {isSoundEnabled() ? 'Sound On' : 'Sound Off'}
          </button>
        </section>

        {/* Change Password — backend endpoint /auth/change-password needs to be implemented */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">Change Password</h2>
          <div className="space-y-3">
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="Current password"
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            />
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="New password (min 6 chars)"
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            />
            <button
              onClick={handleChangePassword}
              className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent/80"
            >
              Change Password
            </button>
            {passwordMsg && (
              <p className={`text-sm ${passwordError ? 'text-danger' : 'text-success'}`}>
                {passwordMsg}
              </p>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
