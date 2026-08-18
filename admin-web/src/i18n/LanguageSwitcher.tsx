import { useI18n } from "./I18nContext";

export function LanguageSwitcher() {
  const { language, setLanguage, t } = useI18n();

  return <div className="language-switcher" role="group" aria-label={t("language.label")}>
    {(["tr", "en"] as const).map((candidateLanguage) => {
      const label = t(candidateLanguage === "tr" ? "language.tr" : "language.en");
      return <button
        key={candidateLanguage}
        type="button"
        className={language === candidateLanguage ? "is-active" : ""}
        aria-pressed={language === candidateLanguage}
        aria-label={t("language.switchTo", { language: label })}
        onClick={() => setLanguage(candidateLanguage)}
      >{candidateLanguage.toUpperCase()}</button>;
    })}
  </div>;
}
