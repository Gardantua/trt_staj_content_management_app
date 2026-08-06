export type AdminRole = "EDITOR" | "ADMIN";

export interface LocalActor {
  id: string | null;
  roles: string[];
}

type LocalActorEnvironment = Pick<ImportMetaEnv, "VITE_LOCAL_ACTOR_ID" | "VITE_LOCAL_ACTOR_ROLES">;

const acceptedRoles = new Set<string>(["EDITOR", "ADMIN"]);

export function localActorFromEnvironment(environment: LocalActorEnvironment = import.meta.env): LocalActor {
  const roles = (environment.VITE_LOCAL_ACTOR_ROLES ?? "")
    .split(",")
    .map((role) => role.trim().toUpperCase())
    .filter(Boolean);

  return {
    id: environment.VITE_LOCAL_ACTOR_ID?.trim() || null,
    roles
  };
}

export function canManageContent(actor: LocalActor): boolean {
  return actor.id !== null && actor.roles.some((role) => acceptedRoles.has(role));
}
