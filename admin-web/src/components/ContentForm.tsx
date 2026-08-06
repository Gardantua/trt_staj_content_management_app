import { useState, type FormEvent } from "react";
import type { ContentDraftInput, ContentType } from "../domain/content";

interface ContentFormProps {
  initialValue?: ContentDraftInput;
  includeContentType: boolean;
  submitLabel: string;
  onSubmit: (input: ContentDraftInput) => Promise<void>;
}

export function ContentForm({ initialValue, includeContentType, submitLabel, onSubmit }: ContentFormProps) {
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
    <form className="stack" onSubmit={submit}>
      <div>
        <label htmlFor="content-title">Başlık</label>
        <input id="content-title" name="title" required maxLength={200} value={title}
          onChange={(event) => setTitle(event.target.value)} />
      </div>
      <div>
        <label htmlFor="content-description">Açıklama</label>
        <textarea id="content-description" name="description" maxLength={2000} rows={5} value={description}
          onChange={(event) => setDescription(event.target.value)} />
      </div>
      {includeContentType && (
        <div>
          <label htmlFor="content-type">İçerik türü</label>
          <select id="content-type" value={contentType} onChange={(event) => setContentType(event.target.value as ContentType)}>
            <option value="SERIES">Dizi</option>
            <option value="FILM">Film</option>
          </select>
        </div>
      )}
      <button type="submit" disabled={isSubmitting}>{isSubmitting ? "Kaydediliyor…" : submitLabel}</button>
    </form>
  );
}
