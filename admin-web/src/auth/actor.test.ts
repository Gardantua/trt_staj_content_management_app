import { describe, expect, it } from "vitest";
import { canManageContent, localActorFromEnvironment } from "./actor";

describe("local actor guard", () => {
  it("allows an explicitly configured editor", () => {
    expect(canManageContent(localActorFromEnvironment({ VITE_LOCAL_ACTOR_ID: "actor-1", VITE_LOCAL_ACTOR_ROLES: "USER, EDITOR" }))).toBe(true);
  });

  it("does not treat a USER role as management access", () => {
    expect(canManageContent(localActorFromEnvironment({ VITE_LOCAL_ACTOR_ID: "actor-1", VITE_LOCAL_ACTOR_ROLES: "USER" }))).toBe(false);
  });
});
