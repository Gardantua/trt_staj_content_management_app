import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { en } from "./en";
import { tr, type TranslationKey } from "./tr";
import type { I18nContextValue, Language, TranslationParameters } from "./types";

export const LANGUAGE_STORAGE_KEY = "app_language:v1";
const DEFAULT_LANGUAGE: Language = "tr";
const dictionaries = { tr, en } as const;

const I18nContext = createContext<I18nContextValue | null>(null);

export function isLanguage(value: unknown): value is Language {
  return value === "tr" || value === "en";
}

export function readStoredLanguage(storage?: Pick<Storage, "getItem">): Language {
  try {
    const storedLanguage = (storage ?? globalThis.localStorage)?.getItem(LANGUAGE_STORAGE_KEY);
    return isLanguage(storedLanguage) ? storedLanguage : DEFAULT_LANGUAGE;
  } catch {
    return DEFAULT_LANGUAGE;
  }
}

export function storeLanguage(language: Language, storage?: Pick<Storage, "setItem">) {
  try {
    (storage ?? globalThis.localStorage)?.setItem(LANGUAGE_STORAGE_KEY, language);
  } catch {
    // Language switching still works for the current page when storage is unavailable.
  }
}

export function translate(language: Language, key: TranslationKey, parameters?: TranslationParameters): string {
  const template = dictionaries[language][key];
  if (!parameters) return template;
  return template.replace(/\{([a-zA-Z][a-zA-Z0-9]*)\}/g, (placeholder, parameterName: string) =>
    Object.prototype.hasOwnProperty.call(parameters, parameterName) ? String(parameters[parameterName]) : placeholder
  );
}

export function applyDocumentLanguage(language: Language, targetDocument: Pick<Document, "documentElement" | "title"> = document) {
  targetDocument.documentElement.lang = language;
  targetDocument.title = language === "tr" ? "Hikâye İzi" : "Hikâye İzi | English";
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() => readStoredLanguage());

  const setLanguage = useCallback((nextLanguage: Language) => {
    setLanguageState(nextLanguage);
    storeLanguage(nextLanguage);
  }, []);

  useEffect(() => {
    applyDocumentLanguage(language);
  }, [language]);

  const t = useCallback((key: TranslationKey, parameters?: TranslationParameters) =>
    translate(language, key, parameters), [language]);
  const contextValue = useMemo(() => ({ language, setLanguage, t }), [language, setLanguage, t]);

  return <I18nContext.Provider value={contextValue}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const context = useContext(I18nContext);
  if (!context) throw new Error("useI18n must be used within I18nProvider");
  return context;
}
