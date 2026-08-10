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
    <form className="content-form stack" onSubmit={submit}>
      {includeContentType && <aside className="form-guide">
        <strong>Bu ekran ne kaydeder?</strong>
        <p>Kullanıcının katalogda göreceği dizi veya film kaydını oluşturur. Quiz ve soru bu ekranda kaydedilmez.</p>
      </aside>}
      <div>
        <label htmlFor="content-title">Başlık</label>
        <input id="content-title" name="title" required maxLength={200} value={title}
          placeholder="Örn. Bir İstanbul Hikâyesi" aria-describedby="content-title-help"
          onChange={(event) => setTitle(event.target.value)} />
        <p className="field-help" id="content-title-help">Kullanıcının katalogda göreceği içerik adı.</p>
      </div>
      <div>
        <label htmlFor="content-description">Açıklama</label>
        <textarea id="content-description" name="description" maxLength={2000} rows={5} value={description}
          placeholder="Örn. İstanbul'da kesişen hayatları anlatan bir drama dizisi." aria-describedby="content-description-help"
          onChange={(event) => setDescription(event.target.value)} />
        <p className="field-help" id="content-description-help">Kullanıcının içerik kartında göreceği kısa özet.</p>
      </div>
      {includeContentType && (
        <div>
          <label htmlFor="content-type">İçerik türü</label>
          <select id="content-type" value={contentType} onChange={(event) => setContentType(event.target.value as ContentType)}>
            <option value="SERIES">Dizi</option>
            <option value="FILM">Film</option>
          </select>
          <p className="field-help">Dizi seçildiğinde taslak kaydedildikten sonra sezon ve bölüm eklenebilir.</p>
        </div>
      )}
      <div className="form-actions"><button className="button-primary" type="submit" disabled={isSubmitting}>{isSubmitting ? "Kaydediliyor…" : submitLabel}</button></div>
    </form>
  );
}
