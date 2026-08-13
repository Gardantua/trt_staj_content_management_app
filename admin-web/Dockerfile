FROM node:22-alpine AS build
WORKDIR /workspace/admin-web
COPY admin-web/package.json admin-web/package-lock.json ./
RUN npm ci
COPY admin-web ./
COPY gorseller /workspace/gorseller
RUN npm run build

FROM caddy:2-alpine
COPY admin-web/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /workspace/admin-web/dist /srv
EXPOSE 80 443
