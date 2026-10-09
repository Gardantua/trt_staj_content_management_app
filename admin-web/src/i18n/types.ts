export type Language = "tr" | "en";

export type TranslationParameters = Readonly<Record<string, string | number>>;

export interface I18nContextValue {
  language: Language;
  setLanguage: (language: Language) => void;
  t: (key: import("./tr").TranslationKey, parameters?: TranslationParameters) => string;
}
