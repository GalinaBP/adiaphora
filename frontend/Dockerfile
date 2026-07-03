# Placeholder frontend container: serves a static page and proxies /api to the backend.
# Replace with the real SPA build (e.g. Vite) when the frontend is implemented.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
EXPOSE 80
