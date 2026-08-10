import { useEffect, useState } from "react";
import type { MediaAsset } from "../api/media-api";
import type { Content, CoverBindingInput } from "../domain/content";

interface CoverEditorProps {
  content: Content;
  onUploadImage: (file: File) => Promise<MediaAsset>;
  onBindCover: (input: CoverBindingInput) => Promise<Content>;
  onContentChanged: (content: Content) => void;
  onError: (reason: unknown) => void;
}

export function CoverEditor({ content, onUploadImage, onBindCover, onContentChanged, onError }: CoverEditorProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadedAsset, setUploadedAsset] = useState<MediaAsset | null>(null);
  const [alternativeText, setAlternativeText] = useState(content.coverAlternativeText ?? "");
  const [isUploading, setIsUploading] = useState(false);
  const [isBinding, setIsBinding] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const mediaAssetId = uploadedAsset?.id ?? content.coverMediaId;

  useEffect(() => {
    setAlternativeText(content.coverAlternativeText ?? "");
  }, [content.id, content.coverAlternativeText]);

  async function upload(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile) return;
    setMessage(null);
    setIsUploading(true);
    try {
      const asset = await onUploadImage(selectedFile);
      setUploadedAsset(asset);
      setMessage("Görsel yüklendi. Şimdi alternatif metni kontrol edip kapağa bağlayın.");
    } catch (reason) {
      onError(reason);
    } finally {
      setIsUploading(false);
    }
  }

  async function bindCover(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!mediaAssetId || !alternativeText.trim()) return;
    setMessage(null);
    setIsBinding(true);
    try {
      onContentChanged(await onBindCover({ mediaAssetId, alternativeText: alternativeText.trim() }));
      setUploadedAsset(null);
      setSelectedFile(null);
      setMessage("Kapak içeriğe bağlandı. Yayın önkoşulu güncellendi.");
    } catch (reason) {
      onError(reason);
    } finally {
      setIsBinding(false);
    }
  }

  return <section className="cover-editor" aria-labelledby="cover-heading">
    <div className="cover-editor__heading">
      <div><p className="eyebrow">Yayın hazırlığı</p><h2 id="cover-heading">Kapak görseli</h2></div>
      {content.coverMediaId && <p className="cover-status">Mevcut kapak bağlı</p>}
    </div>
    <p>Kapak, katalogda görünen görseldir. Yayın için zorunludur; önce dosyayı yükleyin, sonra içeriğe bağlayın.</p>
    {content.coverMediaId && <p className="cover-reference">Bağlı medya: <code>{content.coverMediaId}</code></p>}

    <form className="cover-editor__form" onSubmit={upload}>
      <label>Görsel dosyası
        <input type="file" accept="image/jpeg,image/png" onChange={(event) => { setSelectedFile(event.target.files?.[0] ?? null); setUploadedAsset(null); setMessage(null); }} />
      </label>
      <p className="field-help">JPEG veya PNG seçin. En fazla 5 MB ve 4096 × 4096 piksel olabilir; dosya imzası ve görsel çözümü sunucuda tekrar doğrulanır.</p>
      {selectedFile && <p className="selected-file">Seçilen dosya: <strong>{selectedFile.name}</strong> · {Math.ceil(selectedFile.size / 1024)} KB</p>}
      <button className="button-secondary" disabled={!selectedFile || isUploading} type="submit">{isUploading ? "Yükleniyor…" : "Görseli yükle"}</button>
    </form>

    {uploadedAsset && <p className="upload-result">Yüklenen dosya: {uploadedAsset.mimeType} · {uploadedAsset.width} × {uploadedAsset.height} px</p>}
    <form className="cover-editor__form cover-editor__form--binding" onSubmit={bindCover}>
      <label>Alternatif metin
        <input value={alternativeText} maxLength={500} required onChange={(event) => setAlternativeText(event.target.value)} placeholder="Örn. Boğaz kıyısında gün batımını izleyen iki karakter" />
      </label>
      <p className="field-help">Dosya adını değil, görseldeki anlamlı sahneyi kısa ve net biçimde yazın. Bu metin görseli göremeyen kullanıcılar içindir.</p>
      <button className="button-primary" disabled={!mediaAssetId || !alternativeText.trim() || isBinding} type="submit">{isBinding ? "Bağlanıyor…" : content.coverMediaId ? "Kapağı güncelle" : "Kapağı bağla"}</button>
    </form>
    {message && <p className="cover-message" role="status">{message}</p>}
  </section>;
}
