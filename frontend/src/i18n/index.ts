import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';

import deAuth from './locales/de/auth.json';
import deCharacter from './locales/de/character.json';
import deChat from './locales/de/chat.json';
import deCommon from './locales/de/common.json';
import deDm from './locales/de/dm.json';
import deErrors from './locales/de/errors.json';
import deMap from './locales/de/map.json';

import enAuth from './locales/en/auth.json';
import enCharacter from './locales/en/character.json';
import enChat from './locales/en/chat.json';
import enCommon from './locales/en/common.json';
import enDm from './locales/en/dm.json';
import enErrors from './locales/en/errors.json';
import enMap from './locales/en/map.json';

export const SUPPORTED_LOCALES = ['de', 'en'] as const;
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];

export const DEFAULT_LOCALE: SupportedLocale = 'de';
export const FALLBACK_LOCALE: SupportedLocale = 'en';

export const I18N_NAMESPACES = [
  'common',
  'auth',
  'character',
  'map',
  'chat',
  'dm',
  'errors',
] as const;
export type I18nNamespace = (typeof I18N_NAMESPACES)[number];

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    lng: FALLBACK_LOCALE,
    fallbackLng: FALLBACK_LOCALE,
    supportedLngs: SUPPORTED_LOCALES,
    ns: I18N_NAMESPACES,
    defaultNS: 'common',
    resources: {
      de: {
        common: deCommon,
        auth: deAuth,
        character: deCharacter,
        map: deMap,
        chat: deChat,
        dm: deDm,
        errors: deErrors,
      },
      en: {
        common: enCommon,
        auth: enAuth,
        character: enCharacter,
        map: enMap,
        chat: enChat,
        dm: enDm,
        errors: enErrors,
      },
    },
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      lookupLocalStorage: 'lwe:locale',
    },
  });

export default i18n;