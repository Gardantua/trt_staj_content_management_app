import { describe, expect, it } from "vitest";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { en } from "./en";
import { applyDocumentLanguage, I18nProvider, isLanguage, readStoredLanguage, storeLanguage, translate, LANGUAGE_STORAGE_KEY } from "./I18nContext";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { tr } from "./tr";

describe("i18n dictionaries", () => {
  it("keeps Turkish and English keys identical", () => {
    expect(Object.keys(en).sort()).toEqual(Object.keys(tr).sort());
  });

  it("formats named parameters and leaves missing parameters visible", () => {
    expect(translate("en", "viewer.questionProgress", { current: 2, total: 10 })).toBe("Question 2 / 10");
    expect(translate("tr", "viewer.yourRank", { position: 4, xp: 80 })).toBe("Senin sıran: #4 · 80 XP");
    expect(translate("en", "viewer.yourRank", { position: 4 })).toContain("{xp}");
  });

  it("presents both languages through one labelled native selection control", () => {
    const markup = renderToStaticMarkup(createElement(I18nProvider, null, createElement(LanguageSwitcher)));

    expect(markup).toContain('<select aria-label="Dil">');
    expect(markup).toContain('value="tr" selected="">TR · Türkçe');
    expect(markup).toContain('value="en">EN · English');
  });
});

describe("language persistence", () => {
  it("accepts only supported languages", () => {
    expect(isLanguage("tr")).toBe(true);
    expect(isLanguage("en")).toBe(true);
    expect(isLanguage("de")).toBe(false);
  });

  it("falls back to Turkish for missing, invalid or unavailable storage", () => {
    expect(readStoredLanguage({ getItem: () => null })).toBe("tr");
    expect(readStoredLanguage({ getItem: () => "de" })).toBe("tr");
    expect(readStoredLanguage({ getItem: () => { throw new Error("blocked"); } })).toBe("tr");
  });

  it("stores only the selected language under the versioned key", () => {
    const writes: Array<[string, string]> = [];
    storeLanguage("en", { setItem: (key, value) => { writes.push([key, value]); } });
    expect(writes).toEqual([[LANGUAGE_STORAGE_KEY, "en"]]);
  });
});

describe("document language", () => {
  it("updates the accessible document language and title", () => {
    const targetDocument = { documentElement: { lang: "tr" }, title: "Hikâye İzi" };
    applyDocumentLanguage("en", targetDocument as Pick<Document, "documentElement" | "title">);
    expect(targetDocument).toEqual({ documentElement: { lang: "en" }, title: "Hikâye İzi | English" });
  });
});
