import { describe, expect, it } from "vitest";
import { buildQuizPdfModel } from "./quiz-pdf";

describe("buildQuizPdfModel", () => {
  it("includes content identity, quiz number and marked correct answers", () => {
    const model = buildQuizPdfModel(
      { id: "content-1", title: "Örnek Dizi", description: "İçerik", contentType: "SERIES", publicationStatus: "PUBLISHED", coverMediaId: "media-1", coverImageUrl: "/media/1", coverAlternativeText: "Kapak", seasons: [], createdAt: "", updatedAt: "" },
      { id: "quiz-1", contentId: "content-1", scopeType: "CONTENT", seasonId: null, episodeId: null, versions: [] },
      { id: "version-1", versionNumber: 2, title: "Hafıza Quizi", description: "Açıklama", status: "PUBLISHED", scoringPolicyVersion: "STANDARD_V1", createdAt: "2026-08-10T10:00:00Z", publishedAt: "2026-08-10T11:00:00Z", archivedAt: null, questions: [{ id: "question-1", questionOrder: 1, prompt: "Kim?", difficulty: "MEDIUM", visualMediaId: null, visualRole: null, visualAlternativeText: null, accessiblePrompt: null, answerOptions: [{ id: "a", optionOrder: 1, text: "Doğru", correct: true }, { id: "b", optionOrder: 2, text: "Yanlış", correct: false }, { id: "c", optionOrder: 3, text: "Yanlış 2", correct: false }, { id: "d", optionOrder: 4, text: "Yanlış 3", correct: false }] }] },
      3
    );
    expect(model.metadataLines.join(" ")).toContain("Dizi: Örnek Dizi");
    expect(model.metadataLines.join(" ")).toContain("Quiz no: 3");
    expect(model.questions[0].options[0]).toEqual({ label: "1. Doğru", correct: true });
    expect(model.questions[0].imageContentUrl).toBe("/media/1");
    expect(model.fileName).toMatch(/\.pdf$/);
  });
});
