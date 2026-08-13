export type QuizScopeType = "CONTENT" | "SEASON" | "EPISODE";
export type QuizVersionStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type QuestionDifficulty = "EASY" | "MEDIUM" | "HARD";
export type VisualRole = "INFORMATIVE" | "DECORATIVE";

export interface QuizSummary {
  id: string;
  contentId: string;
  scopeType: QuizScopeType;
  seasonId: string | null;
  episodeId: string | null;
  title: string;
  status: QuizVersionStatus;
  versionNumber: number;
  questionCount: number;
  updatedAt: string;
}

export interface AnswerOption { id: string; optionOrder: number; text: string; correct: boolean; }
export interface QuizQuestion {
  id: string;
  questionOrder: number;
  prompt: string;
  difficulty: QuestionDifficulty;
  visualMediaId: string | null;
  visualRole: VisualRole | null;
  visualAlternativeText: string | null;
  accessiblePrompt: string | null;
  answerOptions: AnswerOption[];
}
export interface QuizVersion {
  id: string;
  versionNumber: number;
  title: string;
  description: string | null;
  status: QuizVersionStatus;
  scoringPolicyVersion: string;
  createdAt: string;
  publishedAt: string | null;
  archivedAt: string | null;
  questions: QuizQuestion[];
}
export interface Quiz {
  id: string;
  contentId: string;
  scopeType: QuizScopeType;
  seasonId: string | null;
  episodeId: string | null;
  versions: QuizVersion[];
}
export interface CreateQuizInput {
  contentId: string;
  scopeType: QuizScopeType;
  seasonId: string | null;
  episodeId: string | null;
  title: string;
  description: string;
}
export interface QuestionInput {
  questionOrder: number;
  prompt: string;
  visualMediaId: string | null;
  visualRole: VisualRole | null;
  visualAlternativeText: string | null;
  accessiblePrompt: string | null;
  answerOptions: Array<{ optionOrder: number; text: string; correct: boolean }>;
}
