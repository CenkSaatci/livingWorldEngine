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
import deSystemWizard from './locales/de/systemWizard.json';

import enAuth from './locales/en/auth.json';
import enCharacter from './locales/en/character.json';
import enChat from './locales/en/chat.json';
import enCommon from './locales/en/common.json';
import enDm from './locales/en/dm.json';
import enErrors from './locales/en/errors.json';
import enMap from './locales/en/map.json';
import enSystemWizard from './locales/en/systemWizard.json';

import frCommon from './locales/fr/common.json';
import frAuth from './locales/fr/auth.json';
import frCharacter from './locales/fr/character.json';
import frMap from './locales/fr/map.json';
import frChat from './locales/fr/chat.json';
import frDm from './locales/fr/dm.json';
import frErrors from './locales/fr/errors.json';
import frSystemWizard from './locales/fr/systemWizard.json';

import itCommon from './locales/it/common.json';
import itAuth from './locales/it/auth.json';
import itCharacter from './locales/it/character.json';
import itMap from './locales/it/map.json';
import itChat from './locales/it/chat.json';
import itDm from './locales/it/dm.json';
import itErrors from './locales/it/errors.json';
import itSystemWizard from './locales/it/systemWizard.json';

import esCommon from './locales/es/common.json';
import esAuth from './locales/es/auth.json';
import esCharacter from './locales/es/character.json';
import esMap from './locales/es/map.json';
import esChat from './locales/es/chat.json';
import esDm from './locales/es/dm.json';
import esErrors from './locales/es/errors.json';
import esSystemWizard from './locales/es/systemWizard.json';

import trCommon from './locales/tr/common.json';
import trAuth from './locales/tr/auth.json';
import trCharacter from './locales/tr/character.json';
import trMap from './locales/tr/map.json';
import trChat from './locales/tr/chat.json';
import trDm from './locales/tr/dm.json';
import trErrors from './locales/tr/errors.json';
import trSystemWizard from './locales/tr/systemWizard.json';

export const SUPPORTED_LOCALES = ['de', 'en', 'fr', 'it', 'es', 'tr'] as const;
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
  'systemWizard',
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
        systemWizard: deSystemWizard,
      },
      en: {
        common: enCommon,
        auth: enAuth,
        character: enCharacter,
        map: enMap,
        chat: enChat,
        dm: enDm,
        errors: enErrors,
        systemWizard: enSystemWizard,
      },
      fr: {
        common: frCommon, auth: frAuth, character: frCharacter,
        map: frMap, chat: frChat, dm: frDm, errors: frErrors,
        systemWizard: frSystemWizard,
      },
      it: {
        common: itCommon, auth: itAuth, character: itCharacter,
        map: itMap, chat: itChat, dm: itDm, errors: itErrors,
        systemWizard: itSystemWizard,
      },
      es: {
        common: esCommon, auth: esAuth, character: esCharacter,
        map: esMap, chat: esChat, dm: esDm, errors: esErrors,
        systemWizard: esSystemWizard,
      },
      tr: {
        common: trCommon, auth: trAuth, character: trCharacter,
        map: trMap, chat: trChat, dm: trDm, errors: trErrors,
        systemWizard: trSystemWizard,
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
