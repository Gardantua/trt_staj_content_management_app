import { isSafeOfficialWatchUrl } from "../security/official-watch-url";

export function OfficialWatchLink({ watchUrl, label }: { watchUrl: string | null; label: string }) {
  if (!watchUrl || !isSafeOfficialWatchUrl(watchUrl)) return null;
  return <a className="watch-on-tabii" href={watchUrl} target="_blank" rel="noopener noreferrer">
    <span aria-hidden="true">▶</span> {label}
  </a>;
}
