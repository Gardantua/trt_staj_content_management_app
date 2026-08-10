import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { UserApp } from "./user/UserApp";
import "./user/user-styles.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode><UserApp /></StrictMode>
);
