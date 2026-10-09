const OFFICIAL_TABII_HOSTS = new Set(["tabii.com", "www.tabii.com"]);
const TABII_DETAIL_PATH = /^\/(?:[a-z]{2}(?:-[a-z]{2})?\/)?detail\/[1-9][0-9]*(?:\/[^/%]+)?\/?$/i;

export function isSafeOfficialWatchUrl(candidate: string): boolean {
  try {
    const url = new URL(candidate);
    return url.protocol === "https:"
      && OFFICIAL_TABII_HOSTS.has(url.hostname.toLowerCase())
      && url.username === ""
      && url.password === ""
      && url.port === ""
      && url.search === ""
      && url.hash === ""
      && TABII_DETAIL_PATH.test(url.pathname);
  } catch {
    return false;
  }
}
