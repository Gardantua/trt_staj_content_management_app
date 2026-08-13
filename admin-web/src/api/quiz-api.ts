import { ApiClient } from "./client";
import type { CreateQuizInput, QuestionInput, Quiz, QuizSummary } from "../domain/quiz";

export class QuizApi {
  constructor(private readonly client: ApiClient) {}

  listForContent(contentId: string): Promise<QuizSummary[]> {
    return this.client.request(`/api/v1/admin/quizzes?contentId=${contentId}`);
  }
  get(quizId: string): Promise<Quiz> { return this.client.request(`/api/v1/admin/quizzes/${quizId}`); }
  create(input: CreateQuizInput): Promise<Quiz> {
    return this.client.request("/api/v1/admin/quizzes", { method: "POST", body: JSON.stringify(input) });
  }

  deleteQuiz(quizId: string): Promise<void> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}`, { method: "DELETE" });
  }
  retireQuiz(quizId: string): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/retire`, { method: "POST" });
  }
  updateVersion(quizId: string, versionId: string, input: { title: string; description: string }): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/versions/${versionId}`, { method: "PUT", body: JSON.stringify(input) });
  }
  startEditing(quizId: string): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/versions`, { method: "POST" });
  }
  addQuestion(quizId: string, versionId: string, input: QuestionInput): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/versions/${versionId}/questions`, { method: "POST", body: JSON.stringify(input) });
  }
  updateQuestion(quizId: string, versionId: string, questionId: string, input: QuestionInput): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/versions/${versionId}/questions/${questionId}`, { method: "PUT", body: JSON.stringify(input) });
  }
  deleteQuestion(quizId: string, versionId: string, questionId: string): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/versions/${versionId}/questions/${questionId}`, { method: "DELETE" });
  }
  publish(quizId: string, versionId: string): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/versions/${versionId}/publish`, { method: "POST" });
  }
  archive(quizId: string, versionId: string): Promise<Quiz> {
    return this.client.request(`/api/v1/admin/quizzes/${quizId}/versions/${versionId}/archive`, { method: "POST" });
  }
}
