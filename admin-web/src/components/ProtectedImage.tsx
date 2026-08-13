import { useEffect, useState } from "react";
import type { MediaApi } from "../api/media-api";

interface ProtectedImageProps {
  contentUrl: string;
  alternativeText: string;
  mediaApi: MediaApi;
  className?: string;
}

export function ProtectedImage({ contentUrl, alternativeText, mediaApi, className }: ProtectedImageProps) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    let createdUrl: string | null = null;
    mediaApi.loadImageObjectUrl(contentUrl).then((url) => {
      createdUrl = url;
      if (active) setObjectUrl(url);
      else URL.revokeObjectURL(url);
    }).catch(() => { if (active) setObjectUrl(null); });
    return () => {
      active = false;
      if (createdUrl) URL.revokeObjectURL(createdUrl);
    };
  }, [contentUrl, mediaApi]);

  return objectUrl
    ? <img className={className} src={objectUrl} alt={alternativeText} />
    : <span className={`${className ?? ""} protected-image-placeholder`} aria-hidden="true">Görsel yükleniyor…</span>;
}
