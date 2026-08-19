import { useEffect, useState, type FormEvent } from "react";
import type { ContentInput, ContentType } from "../domain/content";
import { useUnsavedChanges } from "../hooks/useUnsavedChanges";
import { useI18n } from "../i18n/I18nContext";

interface ContentFormProps {
  initialValue?: ContentInput;
  includeContentType: boolean;
  submitLabel: string;
  onSubmit: (input: ContentInput) => Promise<void>;
  onDirtyChange?: (isDirty: boolean) => void;
  manageNavigationWarning?: boolean;
}

export function ContentForm({ initialValue, includeContentType, submitLabel, onSubmit, onDirtyChange, manageNavigationWarning = true }: ContentFormProps) {
  const { t } = useI18n();
  const [title, setTitle] = useState(initialValue?.title ?? "");
  const [description, setDescription] = useState(initialValue?.description ?? "");
  const [contentType, setContentType] = useState<ContentType>(initialValue?.contentType ?? "SERIES");
  const [savedValue, setSavedValue] = useState<ContentInput>(() => normalizedInput(
    initialValue?.title ?? "", initialValue?.description ?? "", initialValue?.contentType ?? "SERIES"));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const currentValue = normalizedInput(title, description, contentType);
  const isDirty = currentValue.title !== savedValue.title
    || currentValue.description !== savedValue.description
    || currentValue.contentType !== savedValue.contentType;

  useEffect(() => { onDirtyChange?.(isDirty); }, [isDirty, onDirtyChange]);
  useEffect(() => () => onDirtyChange?.(false), [onDirtyChange]);
  useUnsavedChanges(manageNavigationWarning && isDirty, t("admin.unsavedChangesConfirm"));

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await onSubmit(currentValue);
      setSavedValue(currentValue);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="content-form stack" onSubmit={submit}>
      {includeContentType && <aside className="form-guide">
        <strong>{t("admin.formGuideTitle")}</strong>
        <p>{t("admin.formGuideBody")}</p>
      </aside>}
      <div>
        <label htmlFor="content-title">{t("common.title")}</label>
        <input id="content-title" name="title" required maxLength={200} value={title}
          placeholder={t("admin.contentTitlePlaceholder")} aria-describedby="content-title-help"
          onChange={(event) => setTitle(event.target.value)} />
        <p className="field-help" id="content-title-help">{t("admin.contentTitleHelp")}</p>
      </div>
      <div>
        <label htmlFor="content-description">{t("common.description")}</label>
        <textarea id="content-description" name="description" maxLength={2000} rows={5} value={description}
          placeholder={t("admin.contentDescriptionPlaceholder")} aria-describedby="content-description-help"
          onChange={(event) => setDescription(event.target.value)} />
        <p className="field-help" id="content-description-help">{t("admin.contentDescriptionHelp")}</p>
      </div>
      {includeContentType && (
        <div>
          <label htmlFor="content-type">{t("admin.contentType")}</label>
          <select id="content-type" value={contentType} onChange={(event) => setContentType(event.target.value as ContentType)}>
            <option value="SERIES">{t("common.series")}</option>
            <option value="FILM">{t("common.film")}</option>
          </select>
          <p className="field-help">{t("admin.contentTypeHelp")}</p>
        </div>
      )}
      <div className="form-actions"><button className="button-primary" type="submit" disabled={isSubmitting}>{isSubmitting ? t("common.saving") : submitLabel}</button></div>
    </form>
  );
}

function normalizedInput(title: string, description: string, contentType: ContentType): ContentInput {
  return { title: title.trim(), description: description.trim(), contentType };
}
