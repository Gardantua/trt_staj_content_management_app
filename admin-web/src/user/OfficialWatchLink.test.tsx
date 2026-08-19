import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { isSafeOfficialWatchUrl, OfficialWatchLink } from "./OfficialWatchLink";

describe("OfficialWatchLink", () => {
  it("opens an official tabii detail page in an isolated new tab", () => {
    const markup = renderToStaticMarkup(createElement(OfficialWatchLink, {
      watchUrl: "https://www.tabii.com/tr/detail/115660/ibi",
      label: "İzle"
    }));

    expect(markup).toContain('target="_blank"');
    expect(markup).toContain('rel="noopener noreferrer"');
    expect(markup).toContain('href="https://www.tabii.com/tr/detail/115660/ibi"');
  });

  it("accepts the official detail path without a locale prefix", () => {
    expect(isSafeOfficialWatchUrl("https://www.tabii.com/detail/588337")).toBe(true);
  });

  it("rejects executable, insecure, lookalike and redirect-shaped URLs", () => {
    expect(isSafeOfficialWatchUrl("javascript:alert(1)")).toBe(false);
    expect(isSafeOfficialWatchUrl("http://www.tabii.com/tr/detail/115660/ibi")).toBe(false);
    expect(isSafeOfficialWatchUrl("https://tabii.com.example.org/tr/detail/115660/ibi")).toBe(false);
    expect(isSafeOfficialWatchUrl("https://www.tabii.com/tr/detail/115660?next=https://evil.example")).toBe(false);
    expect(isSafeOfficialWatchUrl("https://www.tabii.com/tr/detail/115660/ibi%2Fevil")).toBe(false);
  });
});
