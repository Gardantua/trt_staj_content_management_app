import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, ".", "");
  return {
  plugins: [react()],
  build: {
    rollupOptions: {
      input: {
        user: "index.html",
        admin: "admin.html",
        legacyUser: "user.html"
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      "/api": environment.VITE_PROXY_TARGET ?? "http://localhost:8081"
    }
  }
  };
});
