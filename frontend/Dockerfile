# Multi-stage build for the Vite React SPA. Stage 1 builds the static bundle;
# stage 2 serves it with nginx and proxies /api to the backend (see nginx.conf).
FROM node:22-alpine AS build
WORKDIR /app
# Optional absolute API base for deployments where nginx does not proxy /api
# (e.g. Railway). Vite bakes it into the bundle at build time; when unset the
# SPA falls back to the same-origin '/api/v1' default in src/api/client.ts.
ARG VITE_API_BASE
ENV VITE_API_BASE=$VITE_API_BASE
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
