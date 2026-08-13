import { describe, expect, it } from "vitest";
import { filterContentsBySearch, filterQuizDiscoveries, leaderboardEntryName, orderContentsByRecentViews, paginateContents, publicScopeLabel, questionTimerDeadline, quizContentTitle, quizEarnedXp, quizResultCorrectAnswerSummary, quizResultXpMessage, rememberRecentContent, resolveViewerView, sortQuizDiscoveries } from "./UserApp";

describe("questionTimerDeadline", () => {
  const deadline = "2026-08-13T12:00:30Z";

  it("stops the visible countdown as soon as an answer is being sent or shown", () => {
    expect(questionTimerDeadline(deadline, true, true)).toBeNull();
  });

  it("starts the countdown only for the active next question", () => {
    expect(questionTimerDeadline(deadline, true, false)).toBe(deadline);
    expect(questionTimerDeadline(deadline, false, false)).toBeNull();
  });
});

describe("quizResultXpMessage", () => {
  it("explains that a repeated completion is practice instead of claiming an XP gain", () => {
    expect(quizResultXpMessage(0)).toBe(
      "Bu bir alıştırma çözümüydü; ek XP kazanmadın."
    );
  });

  it("keeps positive and pending XP results explicit", () => {
    expect(quizResultXpMessage(200)).toBe("+200 XP kazandın.");
    expect(quizResultXpMessage(null)).toContain("XP işleniyor");
  });
});

describe("quizEarnedXp", () => {
  it("returns only the saved XP and preserves zero XP", () => {
    const results = [
      { quizId: "quiz-1", earnedXp: 20 },
      { quizId: "quiz-2", earnedXp: 0 }
    ];

    expect(quizEarnedXp("quiz-1", results)).toBe(20);
    expect(quizEarnedXp("quiz-2", results)).toBe(0);
    expect(quizEarnedXp("quiz-3", results)).toBeNull();
  });
});

describe("resolveViewerView", () => {
  it("keeps old leaderboard links working by opening the combined profile", () => {
    expect(resolveViewerView("leaderboard")).toBe("profile");
    expect(resolveViewerView("profile")).toBe("profile");
    expect(resolveViewerView("unknown")).toBe("home");
  });
});

describe("leaderboardEntryName", () => {
  it("shows the account name and never falls back to the technical id", () => {
    const entry = { position: 4, userId: "8345d7b5", displayName: "Yunus", totalXp: 20, firstXpAt: "2026-08-12T10:00:00Z", currentUser: true };
    expect(leaderboardEntryName(entry)).toBe("Yunus");
    expect(leaderboardEntryName({ ...entry, displayName: null })).toBe("Sen");
  });
});

describe("quizResultCorrectAnswerSummary", () => {
  it("shows correct answers against the actual variable question count", () => {
    expect(quizResultCorrectAnswerSummary({
      totalQuestionCount: 10,
      submittedAnswers: Array.from({ length: 10 }, (_, index) => ({ correct: index < 8 }))
    })).toBe("8 / 10 doğru");
  });
});

describe("publicScopeLabel", () => {
  it("gives discovery cards readable quiz scope names", () => {
    expect(publicScopeLabel({ scopeType: "CONTENT" })).toBe("İçerik geneli");
    expect(publicScopeLabel({ scopeType: "SEASON" })).toBe("Sezon quizi");
    expect(publicScopeLabel({ scopeType: "EPISODE" })).toBe("Bölüm quizi");
  });

  it("resolves a quiz to its visible season and episode", () => {
    const content = { seasons: [{ id: "season-2", seasonNumber: 2, title: "İkinci Sezon", episodes: [{ id: "episode-5", episodeNumber: 5, title: "Beşinci Bölüm", description: null }] }] };
    expect(publicScopeLabel({ scopeType: "SEASON", seasonId: "season-2", episodeId: null }, content)).toBe("2. Sezon");
    expect(publicScopeLabel({ scopeType: "EPISODE", seasonId: "season-2", episodeId: "episode-5" }, content)).toBe("2. Sezon · 5. Bölüm");
  });
});

describe("quizContentTitle", () => {
  it("shows the related content title and keeps a safe fallback", () => {
    expect(quizContentTitle({ contentId: "content-2" }, [
      { id: "content-1", title: "Teşkilat" },
      { id: "content-2", title: "Gönül Dağı" }
    ])).toBe("Gönül Dağı");
    expect(quizContentTitle({ contentId: "missing" }, [])).toBe("İçerik");
  });
});

describe("filterQuizDiscoveries", () => {
  const quizzes = [
    { contentId: "content-1", scopeType: "CONTENT" as const, title: "Bozkır bilgisi", description: "Karakterler" },
    { contentId: "content-2", scopeType: "EPISODE" as const, title: "Dağdaki bölüm", description: null }
  ];
  const contents = [{ id: "content-1", title: "Teşkilat" }, { id: "content-2", title: "Gönül Dağı" }];

  it("searches quiz text and related content title with Turkish casing", () => {
    expect(filterQuizDiscoveries(quizzes, contents, "gönül")).toEqual([quizzes[1]]);
    expect(filterQuizDiscoveries(quizzes, contents, "BOZKIR")).toEqual([quizzes[0]]);
  });

  it("keeps an empty search harmless without a scope filter", () => {
    expect(filterQuizDiscoveries(quizzes, contents, "")).toEqual(quizzes);
    expect(filterQuizDiscoveries(quizzes, contents, "bulunmayan")).toEqual([]);
  });
});

describe("catalogue discovery", () => {
  const contents = [
    { id: "content-1", title: "Teşkilat", description: "Aksiyon" },
    { id: "content-2", title: "Gönül Dağı", description: "Anadolu hikâyesi" },
    { id: "content-3", title: "Kare Takımı", description: null }
  ];

  it("searches all content without requiring a category filter", () => {
    expect(filterContentsBySearch(contents, "GÖNÜL")).toEqual([contents[1]]);
    expect(filterContentsBySearch(contents, "aksiyon")).toEqual([contents[0]]);
  });

  it("puts the latest clicked content first while preserving untouched catalogue order", () => {
    const recentIds = rememberRecentContent(rememberRecentContent([], "content-2"), "content-3");
    expect(recentIds).toEqual(["content-3", "content-2"]);
    expect(orderContentsByRecentViews(contents, recentIds).map((content) => content.id)).toEqual(["content-3", "content-2", "content-1"]);
  });

  it("paginates the locally searched catalogue", () => {
    expect(paginateContents(contents, 1, 2)).toMatchObject({ items: [contents[2]], page: 1, totalItems: 3, totalPages: 2 });
  });
});

describe("sortQuizDiscoveries", () => {
  const quizzes = [
    { contentId: "content-2", title: "Zorlu Quiz", questionCount: 5 },
    { contentId: "content-1", title: "İlk Quiz", questionCount: 10 },
    { contentId: "content-1", title: "Başlangıç Quiz", questionCount: 5 }
  ];
  const contents = [{ id: "content-1", title: "Gönül Dağı" }, { id: "content-2", title: "Teşkilat" }];

  it("sorts by content and then quiz title without mutating the loaded list", () => {
    expect(sortQuizDiscoveries(quizzes, contents, "CONTENT_TITLE").map((quiz) => quiz.title)).toEqual(["Başlangıç Quiz", "İlk Quiz", "Zorlu Quiz"]);
    expect(quizzes[0].title).toBe("Zorlu Quiz");
  });

  it("supports quiz title and descending question count", () => {
    expect(sortQuizDiscoveries(quizzes, contents, "QUIZ_TITLE").map((quiz) => quiz.title)).toEqual(["Başlangıç Quiz", "İlk Quiz", "Zorlu Quiz"]);
    expect(sortQuizDiscoveries(quizzes, contents, "QUESTION_COUNT").map((quiz) => quiz.questionCount)).toEqual([10, 5, 5]);
  });
});
