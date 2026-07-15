import '@testing-library/jest-dom/vitest';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import deCommon from '../i18n/locales/de/common.json';
import deAuth from '../i18n/locales/de/auth.json';

void i18n.use(initReactI18next).init({
  lng: 'de',
  fallbackLng: 'de',
  ns: ['common', 'auth'],
  defaultNS: 'common',
  resources: {
    de: { common: deCommon, auth: deAuth },
  },
  interpolation: { escapeValue: false },
});
