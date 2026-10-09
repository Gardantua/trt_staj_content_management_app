import { useEffect } from "react";

export function useUnsavedChanges(isDirty: boolean, confirmationMessage: string) {
  useEffect(() => {
    if (!isDirty) return;
    const beforeUnload = (event: BeforeUnloadEvent) => event.preventDefault();
    const guardAnchorNavigation = (event: MouseEvent) => {
      const target = event.target;
      const anchor = target instanceof Element ? target.closest("a[href]") : null;
      if (!anchor || anchor.getAttribute("target") === "_blank" || anchor.hasAttribute("download")
        || window.confirm(confirmationMessage)) return;
      event.preventDefault();
      event.stopImmediatePropagation();
    };
    window.addEventListener("beforeunload", beforeUnload);
    document.addEventListener("click", guardAnchorNavigation, true);
    return () => {
      window.removeEventListener("beforeunload", beforeUnload);
      document.removeEventListener("click", guardAnchorNavigation, true);
    };
  }, [confirmationMessage, isDirty]);
}
