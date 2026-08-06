import type { ApiRequestError } from "../api/client";

interface ApiErrorNoticeProps {
  error: ApiRequestError | null;
  onRetry?: () => void;
}

export function ApiErrorNotice({ error, onRetry }: ApiErrorNoticeProps) {
  if (!error) return null;

  return (
    <section className="api-error" aria-live="assertive" aria-atomic="true" role="alert">
      <h2>İşlem tamamlanamadı</h2>
      <p>{error.message}</p>
      <dl>
        <div><dt>Hata kodu</dt><dd>{error.code}</dd></div>
        <div><dt>İz kimliği</dt><dd>{error.traceId}</dd></div>
        <div><dt>HTTP durumu</dt><dd>{error.status || "ulaşılamadı"}</dd></div>
      </dl>
      {onRetry ? <button type="button" onClick={onRetry}>Tekrar dene</button> : null}
    </section>
  );
}
