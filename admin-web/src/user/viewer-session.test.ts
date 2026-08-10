import { describe, expect, it } from "vitest";
import { isLocalActorId } from "./viewer-session";

describe("local viewer actor validation", () => {
  it("accepts the canonical UUID documented by the project", () => {
    expect(isLocalActorId("11111111-1111-1111-1111-111111111111")).toBe(true);
  });

  it("rejects non-UUID input", () => {
    expect(isLocalActorId("not-a-user-id")).toBe(false);
  });
});
