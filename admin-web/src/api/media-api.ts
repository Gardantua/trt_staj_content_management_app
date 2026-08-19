import { ApiClient } from "./client";
import { readStoredLanguage, translate } from "../i18n/I18nContext";

export interface MediaAsset {
  id: string;
  mediaType: string;
  mimeType: string;
  byteSize: number;
  width: number;
  height: number;
  contentUrl: string;
  createdAt: string;
}

export class MediaApi {
  private readonly imageBlobRequests = new Map<string, Promise<Blob>>();

  constructor(private readonly client: ApiClient) {}

  uploadImage(file: File): Promise<MediaAsset> {
    const formData = new FormData();
    formData.append("file", file);
    return this.client.requestForm("/api/v1/admin/media/images", formData, { method: "POST" });
  }

  async loadImageObjectUrl(contentUrl: string): Promise<string> {
    return URL.createObjectURL(await this.loadImageBlob(contentUrl));
  }

  async loadImageDataUrl(contentUrl: string): Promise<string> {
    const imageBlob = await this.loadImageBlob(contentUrl);
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => typeof reader.result === "string"
        ? resolve(reader.result)
        : reject(new Error(translate(readStoredLanguage(), "error.pdfImageUnavailable")));
      reader.onerror = () => reject(reader.error ?? new Error(translate(readStoredLanguage(), "error.pdfImageUnavailable")));
      reader.readAsDataURL(imageBlob);
    });
  }

  private loadImageBlob(contentUrl: string): Promise<Blob> {
    const cachedRequest = this.imageBlobRequests.get(contentUrl);
    if (cachedRequest) return cachedRequest;

    const imageRequest = this.client.requestBlob(contentUrl).catch((error: unknown) => {
      this.imageBlobRequests.delete(contentUrl);
      throw error;
    });
    this.imageBlobRequests.set(contentUrl, imageRequest);
    return imageRequest;
  }
}
