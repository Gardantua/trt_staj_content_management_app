import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { UserApp } from "./user/UserApp";
import { I18nProvider } from "./i18n/I18nContext";
import "./user/user-styles.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode><I18nProvider><UserApp /></I18nProvider></StrictMode>
);
