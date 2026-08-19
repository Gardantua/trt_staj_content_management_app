import type { ApiRequestError } from "../api/client";
import { useI18n } from "../i18n/I18nContext";

interface ApiErrorNoticeProps {
  error: ApiRequestError | null;
  onRetry?: () => void;
}

export function ApiErrorNotice({ error, onRetry }: ApiErrorNoticeProps) {
  const { t } = useI18n();
  if (!error) return null;
  const message = error.code === "CONTENT_WATCH_URL_INVALID"
    ? t("error.invalidWatchUrl")
    : error.code === "RESOURCE_NOT_FOUND"
      ? t("error.resourceNotFound")
      : error.message;

  return (
    <section className="api-error" aria-live="assertive" aria-atomic="true" role="alert">
      <h2>{t("error.title")}</h2>
      <p>{message}</p>
      <dl>
        <div><dt>{t("error.code")}</dt><dd>{error.code}</dd></div>
        <div><dt>{t("error.traceId")}</dt><dd>{error.traceId}</dd></div>
        <div><dt>{t("error.httpStatus")}</dt><dd>{error.status || t("error.unreachable")}</dd></div>
      </dl>
      {onRetry ? <button type="button" onClick={onRetry}>{t("common.retry")}</button> : null}
    </section>
  );
}
