export type ContentType = "SERIES" | "FILM";

export interface PublicContentSummary {
  id: string;
  title: string;
  description: string | null;
  contentType: ContentType;
  coverImageUrl: string;
  coverAlternativeText: string;
}

export interface PublicContentPage {
  items: PublicContentSummary[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface PublicEpisode {
  id: string;
  episodeNumber: number;
  title: string;
  description: string | null;
}

export interface PublicSeason {
  id: string;
  seasonNumber: number;
  title: string;
  episodes: PublicEpisode[];
}

export interface PublicContent extends PublicContentSummary {
  publicationStatus: "PUBLISHED";
  seasons: PublicSeason[];
}

export interface PublishedQuiz {
  quizId: string;
  contentId: string;
  scopeType: "CONTENT" | "SEASON" | "EPISODE";
  seasonId: string | null;
  episodeId: string | null;
  versionId: string;
  versionNumber: number;
  title: string;
  description: string | null;
  scoringPolicyVersion: string;
  questions: Array<{ id: string }>;
}

export interface PublishedQuizSummary {
  quizId: string;
  contentId: string;
  scopeType: PublishedQuiz["scopeType"];
  seasonId: string | null;
  episodeId: string | null;
  title: string;
  description: string | null;
  questionCount: number;
}

export interface QuizResultSummary {
  quizId: string;
  earnedXp: number | null;
}

export interface PublicApiError {
  code: string;
  message: string;
  traceId: string;
  status: number;
}

export interface AttemptQuestion {
  questionId: string;
  questionOrder: number;
  prompt: string;
  difficulty: string;
  visual: { contentUrl: string; role: "INFORMATIVE" | "DECORATIVE"; alternativeText: string } | null;
  accessiblePrompt: string | null;
  options: Array<{ optionId: string; optionOrder: number; text: string }>;
}

export interface AnswerFeedback {
  questionId: string;
  selectedOptionId: string | null;
  correct: boolean;
  resultStatus: "CORRECT" | "INCORRECT" | "TIMED_OUT";
  correctOptionId: string;
  correctOptionText?: string | null;
  explanation?: string | null;
  awardedPoints: number;
}

export interface QuizAttempt {
  attemptId: string;
  quizId: string;
  quizVersionId: string;
  status: "ACTIVE" | "AWAITING_NEXT_QUESTION" | "COMPLETED" | "EXPIRED";
  timingPolicyVersion: "QUESTION_30_SECONDS_V1";
  score: number;
  earnedXp: number | null;
  startedAt: string;
  questionDeadline: string | null;
  completedAt: string | null;
  answeredQuestionCount: number;
  totalQuestionCount: number;
  currentQuestion: AttemptQuestion | null;
  submittedAnswers: AnswerFeedback[];
}

export interface AnswerSubmissionResult {
  attemptId: string;
  feedback: AnswerFeedback;
  attemptStatus: QuizAttempt["status"];
  score: number;
  earnedXp: number | null;
  questionDeadline: string | null;
  nextQuestion: AttemptQuestion | null;
}

export interface XpSummary { userId: string; totalXp: number; transactionCount: number; }
export interface CurrentActor { actorId: string; roles: string[]; }
export interface LeaderboardEntry { position: number; userId: string; displayName: string | null; totalXp: number; firstXpAt: string; currentUser: boolean; }
export interface Leaderboard { scope: "GLOBAL" | "CONTENT"; contentId: string | null; period: string; dataSource: "REDIS" | "POSTGRESQL_FALLBACK"; projectionGeneratedAt: string | null; participantCount: number; leaders: LeaderboardEntry[]; currentUser: LeaderboardEntry | null; }
