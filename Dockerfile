# ---------- etapa 1: build do front (React + Vite) ----------
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY index.html vite.config.js ./
COPY src ./src
RUN npm run build

# ---------- etapa 2: servir os arquivos estáticos com nginx ----------
FROM nginx:1.27-alpine
# o nginx aplica as variáveis de ambiente no template ao iniciar
COPY docker/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# endereço do back-end e DNS usado para resolvê-lo (127.0.0.11 = DNS interno do Docker)
ENV API_UPSTREAM=http://api:8080 \
    NGINX_RESOLVER=127.0.0.11

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8080/ >/dev/null || exit 1
CMD ["nginx", "-g", "daemon off;"]
