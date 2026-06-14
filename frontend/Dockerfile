FROM node:22-alpine AS builder

WORKDIR /app
RUN corepack enable && corepack prepare pnpm@9.15.1 --activate

COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
COPY frontend/turbo.json ./
COPY frontend/packages ./packages
COPY frontend/apps/lobby/package.json ./apps/lobby/
COPY frontend/apps/screen/package.json ./apps/screen/
COPY frontend/apps/controller/package.json ./apps/controller/

RUN pnpm install --frozen-lockfile

COPY frontend/ .
RUN pnpm build

FROM nginx:alpine
COPY --from=builder /app/apps/lobby/dist /usr/share/nginx/html/lobby
COPY --from=builder /app/apps/screen/dist /usr/share/nginx/html/screen
COPY --from=builder /app/apps/controller/dist /usr/share/nginx/html/controller
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
