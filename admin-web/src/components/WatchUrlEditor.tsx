import { useEffect, useState, type FormEvent } from "react";
import { useUnsavedChanges } from "../hooks/useUnsavedChanges";
import { useI18n } from "../i18n/I18nContext";
import { isSafeOfficialWatchUrl } from "../security/official-watch-url";

export function WatchUrlEditor({ watchUrl, onSave, onDirtyChange, manageNavigationWarning = true }: { watchUrl: string | null; onSave: (watchUrl: string) => Promise<void>; onDirtyChange?: (isDirty: boolean) => void; manageNavigationWarning?: boolean }) {
  const { t } = useI18n();
  const [candidate, setCandidate] = useState(watchUrl ?? "");
  const [savedValue, setSavedValue] = useState(watchUrl ?? "");
  const [isSaving, setIsSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const normalizedCandidate = candidate.trim();
  const isDirty = normalizedCandidate !== savedValue;
  const canTest = normalizedCandidate.length > 0 && isSafeOfficialWatchUrl(normalizedCandidate);

  useEffect(() => { setCandidate(watchUrl ?? ""); setSavedValue(watchUrl ?? ""); }, [watchUrl]);
  useEffect(() => { onDirtyChange?.(isDirty); }, [isDirty, onDirtyChange]);
  useEffect(() => () => onDirtyChange?.(false), [onDirtyChange]);
  useUnsavedChanges(manageNavigationWarning && isDirty, t("admin.unsavedChangesConfirm"));

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    try {
      await onSave(normalizedCandidate);
      setSavedValue(normalizedCandidate);
      setSuccessMessage(t(normalizedCandidate ? "admin.watchUrlSaved" : "admin.watchUrlRemoved"));
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
        onChange={(event) => { setCandidate(event.target.value); setSuccessMessage(null); }}
      />
      <p className="field-help" id="content-watch-url-help">{t("admin.watchUrlHelp")}</p>
    </div>
    {successMessage ? <p className="save-success" role="status">{successMessage}</p> : null}
    <div className="form-actions watch-url-actions">
      {canTest ? <a className="button-secondary button-link" href={normalizedCandidate} target="_blank" rel="noopener noreferrer">{t("admin.testWatchUrl")}</a> : null}
      <button className="button-primary" type="submit" disabled={isSaving || !isDirty}>{isSaving ? t("common.saving") : t("admin.saveWatchUrl")}</button>
    </div>
  </form>;
}
