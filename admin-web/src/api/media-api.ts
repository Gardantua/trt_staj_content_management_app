import { ApiClient } from "./client";

export interface MediaAsset {
  id: string;
  mediaType: string;
  mimeType: string;
  byteSize: number;
  width: number;
  height: number;
  contentUrl: string;
}

export class MediaApi {
  constructor(private readonly client: ApiClient) {}

  uploadImage(file: File): Promise<MediaAsset> {
    const formData = new FormData();
    formData.append("file", file);
    return this.client.requestForm("/api/v1/admin/media/images", formData, { method: "POST" });
  }
}
