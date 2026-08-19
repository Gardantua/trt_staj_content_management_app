import { useEffect, useState, type FormEvent } from "react";
import { useI18n } from "../i18n/I18nContext";

export function WatchUrlEditor({ watchUrl, onSave }: { watchUrl: string | null; onSave: (watchUrl: string) => Promise<void> }) {
  const { t } = useI18n();
  const [candidate, setCandidate] = useState(watchUrl ?? "");
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => setCandidate(watchUrl ?? ""), [watchUrl]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    try {
      await onSave(candidate.trim());
    } finally {
      setIsSaving(false);
    }
  }

  return <form className="watch-url-editor stack" onSubmit={submit}>
    <div>
      <label htmlFor="content-watch-url">{t("admin.watchUrl")}</label>
      <input
        id="content-watch-url"
        type="url"
        inputMode="url"
        maxLength={500}
        value={candidate}
        placeholder="https://www.tabii.com/detail/588337"
        aria-describedby="content-watch-url-help"
        onChange={(event) => setCandidate(event.target.value)}
      />
      <p className="field-help" id="content-watch-url-help">{t("admin.watchUrlHelp")}</p>
    </div>
    <div className="form-actions"><button className="button-primary" type="submit" disabled={isSaving}>{isSaving ? t("common.saving") : t("admin.saveWatchUrl")}</button></div>
  </form>;
}
