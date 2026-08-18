import { useState, type FormEvent } from "react";
import type { ContentInput, ContentType } from "../domain/content";
import { useI18n } from "../i18n/I18nContext";

interface ContentFormProps {
  initialValue?: ContentInput;
  includeContentType: boolean;
  submitLabel: string;
  onSubmit: (input: ContentInput) => Promise<void>;
}

export function ContentForm({ initialValue, includeContentType, submitLabel, onSubmit }: ContentFormProps) {
  const { t } = useI18n();
  const [title, setTitle] = useState(initialValue?.title ?? "");
  const [description, setDescription] = useState(initialValue?.description ?? "");
  const [contentType, setContentType] = useState<ContentType>(initialValue?.contentType ?? "SERIES");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await onSubmit({ title: title.trim(), description: description.trim(), contentType });
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
