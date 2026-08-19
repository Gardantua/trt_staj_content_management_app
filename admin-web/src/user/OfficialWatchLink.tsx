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

export function OfficialWatchLink({ watchUrl, label }: { watchUrl: string | null; label: string }) {
  if (!watchUrl || !isSafeOfficialWatchUrl(watchUrl)) return null;
  return <a className="watch-on-tabii" href={watchUrl} target="_blank" rel="noopener noreferrer">
    <span aria-hidden="true">▶</span> {label}
  </a>;
}
