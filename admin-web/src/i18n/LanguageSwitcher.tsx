import { useI18n } from "./I18nContext";

export function LanguageSwitcher() {
  const { language, setLanguage, t } = useI18n();

  return <label className="language-switcher">
    <span className="language-switcher__icon" aria-hidden="true">文</span>
    <span className="language-switcher__label">{t("language.label")}</span>
    <select
      value={language}
      aria-label={t("language.label")}
      onChange={(event) => setLanguage(event.target.value === "en" ? "en" : "tr")}
    >
      <option value="tr">TR · Türkçe</option>
      <option value="en">EN · English</option>
    </select>
  </label>;
}
