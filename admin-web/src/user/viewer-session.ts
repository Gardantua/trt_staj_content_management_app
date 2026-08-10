import { localActorFromEnvironment, type LocalActor } from "../auth/actor";

const storageKey = "content-engagement.viewer-session.v1";

export function initialViewerActor(): LocalActor | null {
  const configuredActor = localActorFromEnvironment();
  if (configuredActor.id) return configuredActor;
  try {
    const storedActor = JSON.parse(window.localStorage.getItem(storageKey) ?? "null") as LocalActor | null;
    return storedActor?.id && storedActor.roles.includes("USER") ? storedActor : null;
  } catch { return null; }
}

export function saveLocalViewerActor(actorId: string): LocalActor {
  const actor = { id: actorId, roles: ["USER"] };
  window.localStorage.setItem(storageKey, JSON.stringify(actor));
  return actor;
}

export function clearLocalViewerActor(): void {
  window.localStorage.removeItem(storageKey);
}

export function isLocalActorId(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.trim());
}
