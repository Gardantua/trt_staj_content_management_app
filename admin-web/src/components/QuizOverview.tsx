import { useEffect, useMemo, useState } from "react";
import { ApiRequestError } from "../api/client";
import type { ContentApi } from "../api/content-api";
import type { MediaApi } from "../api/media-api";
import type { QuizApi } from "../api/quiz-api";
import type { Content } from "../domain/content";
import type { Quiz, QuizSummary, QuizVersion } from "../domain/quiz";
import { ApiErrorNotice } from "./ApiErrorNotice";
import { downloadQuizPdf } from "./quiz-pdf";
import { readStoredLanguage, translate, useI18n } from "../i18n/I18nContext";

interface LoadedQuiz {
  content: Content;
  quiz: Quiz;
  summary: QuizSummary;
}

interface QuizOverviewProps {
  mode: "active" | "history";
  contentApi: ContentApi;
  quizApi: QuizApi;
  mediaApi: MediaApi;
  navigate: (path: string) => void;
}

export function QuizOverview({ mode, contentApi, quizApi, mediaApi, navigate }: QuizOverviewProps) {
  const { language, t } = useI18n();
  const [loadedQuizzes, setLoadedQuizzes] = useState<LoadedQuiz[]>([]);
  const [error, setError] = useState<ApiRequestError | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError(null);
    contentApi.list(0, 100).then(async (page) => {
      const contentQuizzes = await Promise.all(page.items.map(async (summary) => {
        const [content, quizzes] = await Promise.all([
          contentApi.get(summary.id),
          quizApi.listForContent(summary.id)
        ]);
        return Promise.all(quizzes.map(async (quizSummary) => ({
          content,
          summary: quizSummary,
          quiz: await quizApi.get(quizSummary.id)
        })));
      }));
      if (active) setLoadedQuizzes(contentQuizzes.flat());
    }).catch((reason: unknown) => {
      if (active) setError(toApiError(reason));
    }).finally(() => {
      if (active) setIsLoading(false);
    });
    return () => { active = false; };
  }, [contentApi, quizApi]);

  const activeQuizzes = useMemo(
    () => loadedQuizzes.filter(({ summary }) => summary.status !== "ARCHIVED"),
    [loadedQuizzes]
  );
  const historyEntries = useMemo(
    () => loadedQuizzes.flatMap((loadedQuiz) => loadedQuiz.quiz.versions
      .filter((version) => version.status === "ARCHIVED")
      .map((version) => ({ ...loadedQuiz, version }))),
    [loadedQuizzes]
  );

  return <section className="quiz-overview">
    <div className="editor-heading">
      <p className="eyebrow">{t(mode === "active" ? "admin.quizOperations" : "admin.publicationHistory")}</p>
      <h1>{t(mode === "active" ? "admin.quizzes" : "admin.quizHistory")}</h1>
      <p>{t(mode === "active" ? "admin.activeQuizIntro" : "admin.historyIntro")}</p>
    </div>
    <ApiErrorNotice error={error} />
    {isLoading ? <p aria-live="polite">{t("viewer.quizzesLoading")}</p> : mode === "active"
      ? <ActiveQuizList quizzes={activeQuizzes} navigate={navigate} />
      : <QuizHistoryList entries={historyEntries} language={language} onDownload={async (entry, index) => {
        try { await downloadQuizPdf(entry.content, entry.quiz, entry.version, index + 1, mediaApi, language); }
        catch (reason) { setError(toApiError(reason)); }
      }} />}
  </section>;
}

function ActiveQuizList({ quizzes, navigate }: { quizzes: LoadedQuiz[]; navigate: (path: string) => void }) {
  const { t } = useI18n();
  if (quizzes.length === 0) return <p className="notice">{t("admin.noActiveQuiz")}</p>;
  return <ul className="overview-list">{quizzes.map(({ content, summary }) => <li key={summary.id}><article>
    <div><span>{t(content.contentType === "SERIES" ? "common.series" : "common.film")} · {content.title}</span><h2>{summary.title}</h2><p>{t("common.questions", { count: summary.questionCount })} · {t(summary.status === "PUBLISHED" ? "admin.available" : "admin.preparing")}</p></div>
    <button className="button-primary" onClick={() => navigate(`/admin/contents/${content.id}?tab=quiz&quiz=${summary.id}`)}>{t("admin.openQuiz")}</button>
  </article></li>)}</ul>;
}

interface HistoryEntry extends LoadedQuiz { version: QuizVersion; }

function QuizHistoryList({ entries, language, onDownload }: { entries: HistoryEntry[]; language: "tr" | "en"; onDownload: (entry: HistoryEntry, index: number) => Promise<void> }) {
  const { t } = useI18n();
  if (entries.length === 0) return <p className="notice">{t("admin.noQuizHistory")}</p>;
  return <ul className="overview-list">{entries.map((entry, index) => <li key={entry.version.id}><article>
    <div><span>{entry.content.title} · {t("admin.version", { number: entry.version.versionNumber })}</span><h2>{entry.version.title}</h2><p>{t("common.questions", { count: entry.version.questions.length })} · {formatDate(entry.version.archivedAt, language, t("admin.pastRecord"))}</p></div>
    <button className="button-secondary" onClick={() => void onDownload(entry, index)}>{t("admin.downloadAnswerKey")}</button>
  </article></li>)}</ul>;
}

function formatDate(value: string | null, language: "tr" | "en", fallback: string) {
  return value ? new Intl.DateTimeFormat(language === "tr" ? "tr-TR" : "en-US", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : fallback;
}

function toApiError(reason: unknown) {
  return reason instanceof ApiRequestError ? reason : new ApiRequestError({ code: "UNEXPECTED_CLIENT_ERROR", message: translate(readStoredLanguage(), "admin.quizLoadUnexpected"), traceId: "unavailable", status: 0 });
}
