import { useEffect, useMemo, useState } from "react";
import { ApiRequestError } from "../api/client";
import type { ContentApi } from "../api/content-api";
import type { MediaApi } from "../api/media-api";
import type { QuizApi } from "../api/quiz-api";
import type { Content } from "../domain/content";
import type { Quiz, QuizSummary, QuizVersion } from "../domain/quiz";
import { ApiErrorNotice } from "./ApiErrorNotice";
import { downloadQuizPdf } from "./quiz-pdf";

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
      <p className="eyebrow">{mode === "active" ? "Quiz işlemleri" : "Yayın geçmişi"}</p>
      <h1>{mode === "active" ? "Quizler" : "Quiz geçmişi"}</h1>
      <p>{mode === "active"
        ? "Hazırlanan ve kullanıma açık quizleri tek yerden görüntüleyin. Yeni quiz için ilgili dizi veya filmi açın."
        : "Kaldırılan quizleri ve sorular değiştiğinde korunan önceki sürümleri görüntüleyin."}</p>
    </div>
    <ApiErrorNotice error={error} />
    {isLoading ? <p aria-live="polite">Quizler yükleniyor…</p> : mode === "active"
      ? <ActiveQuizList quizzes={activeQuizzes} navigate={navigate} />
      : <QuizHistoryList entries={historyEntries} onDownload={async (entry, index) => {
        try { await downloadQuizPdf(entry.content, entry.quiz, entry.version, index + 1, mediaApi); }
        catch (reason) { setError(toApiError(reason)); }
      }} />}
  </section>;
}

function ActiveQuizList({ quizzes, navigate }: { quizzes: LoadedQuiz[]; navigate: (path: string) => void }) {
  if (quizzes.length === 0) return <p className="notice">Henüz aktif veya hazırlanmakta olan quiz yok. İçerik yönetiminden bir dizi ya da film açarak quiz oluşturabilirsiniz.</p>;
  return <ul className="overview-list">{quizzes.map(({ content, summary }) => <li key={summary.id}><article>
    <div><span>{content.contentType === "SERIES" ? "Dizi" : "Film"} · {content.title}</span><h2>{summary.title}</h2><p>{summary.questionCount} soru · {summary.status === "PUBLISHED" ? "Kullanıma açık" : "Hazırlanıyor"}</p></div>
    <button className="button-primary" onClick={() => navigate(`/admin/contents/${content.id}?tab=quiz&quiz=${summary.id}`)}>Quizi aç</button>
  </article></li>)}</ul>;
}

interface HistoryEntry extends LoadedQuiz { version: QuizVersion; }

function QuizHistoryList({ entries, onDownload }: { entries: HistoryEntry[]; onDownload: (entry: HistoryEntry, index: number) => Promise<void> }) {
  if (entries.length === 0) return <p className="notice">Henüz geçmişe taşınmış bir quiz veya eski sürüm yok.</p>;
  return <ul className="overview-list">{entries.map((entry, index) => <li key={entry.version.id}><article>
    <div><span>{entry.content.title} · Sürüm {entry.version.versionNumber}</span><h2>{entry.version.title}</h2><p>{entry.version.questions.length} soru · {formatDate(entry.version.archivedAt)}</p></div>
    <button className="button-secondary" onClick={() => void onDownload(entry, index)}>Cevap anahtarını indir</button>
  </article></li>)}</ul>;
}

function formatDate(value: string | null) {
  return value ? new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "Geçmiş kayıt";
}

function toApiError(reason: unknown) {
  return reason instanceof ApiRequestError ? reason : new ApiRequestError({ code: "UNEXPECTED_CLIENT_ERROR", message: "Quizler yüklenirken beklenmeyen bir hata oluştu.", traceId: "unavailable", status: 0 });
}
