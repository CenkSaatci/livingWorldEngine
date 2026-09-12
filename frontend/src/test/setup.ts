import '@testing-library/jest-dom/vitest';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import deCommon from '../i18n/locales/de/common.json';
import deAuth from '../i18n/locales/de/auth.json';
import deCharacter from '../i18n/locales/de/character.json';
import deSystemWizard from '../i18n/locales/de/systemWizard.json';

void i18n.use(initReactI18next).init({
  lng: 'de',
  fallbackLng: 'de',
  ns: ['common', 'auth', 'character', 'systemWizard'],
  defaultNS: 'common',
  resources: {
    de: { common: deCommon, auth: deAuth, character: deCharacter, systemWizard: deSystemWizard },
  },
  interpolation: { escapeValue: false },
});

// R4: jsdom hat kein matchMedia (useMediaQuery in CombatPage/GameView).
if (typeof window !== 'undefined' && !window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }),
  });
}
