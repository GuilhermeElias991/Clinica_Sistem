# 🐳 Docker

## 1. Arquivos

| Arquivo | Função |
|---|---|
| `Dockerfile` | Build em 2 etapas: **node:20-alpine** compila o front, **nginx:1.27-alpine** serve os arquivos |
| `docker/nginx.conf` | Template: porta **8080**, fallback de SPA (`/agenda` etc. → `index.html`), cache de `/assets`, gzip e proxy de `/api` para o back-end (`API_UPSTREAM`) |
| `backend/Dockerfile` | Build em 2 etapas: **temurin 21 JDK** compila a API, **temurin 21 JRE** roda o `.jar` |
| `.dockerignore` | Deixa de fora `node_modules`, `.git`, `.env`, docs e banco |
| `docker-compose.yml` | Sobe `web` (front) + `api` (Spring Boot) + `db` (PostgreSQL 16) |
| `.env.example` | Modelo das variáveis do compose |

## 2. Pré-requisito

Docker Desktop instalado e **aberto** (`docker --version` deve responder).

## 3. Só o front (imagem única)

```bash
docker build -t odontocare-web .
docker run --rm -p 8080:8080 odontocare-web
```
Abra **http://localhost:8080**. Para parar: `Ctrl + C`.

## 4. Sistema completo: front + API + banco (compose)

```bash
cp .env.example .env        # edite e troque POSTGRES_PASSWORD e JWT_SECRET
docker compose up -d --build
```

Ordem de subida:
1. `db` sobe e fica **healthy** (`pg_isready`).
2. `api` sobe, o **Flyway** cria as tabelas, cria o usuário admin e (com `SEED_DEMO=true`) os dados de exemplo — **http://localhost:8081/api**.
3. `web` serve o front em **http://localhost:8080** e repassa `/api` para a `api`.

### Verificar
```bash
docker compose ps
docker compose logs api             # deve mostrar "Migrating schema ... to version 1" e "Started OdontoCareApplication"
docker compose exec db psql -U odonto -d odonto -c "\dt"
```

### Parar e limpar
```bash
docker compose down        # para (mantém o banco)
docker compose down -v     # para e APAGA o volume do banco
```

## 5. Variáveis (.env)

| Variável | Padrão | Uso |
|---|---|---|
| `POSTGRES_DB` | `odonto` | nome do banco |
| `POSTGRES_USER` | `odonto` | usuário |
| `POSTGRES_PASSWORD` | **obrigatória** | senha (o compose não sobe sem ela) |
| `JWT_SECRET` | **obrigatória** | chave do token de login (mín. 32 caracteres) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@odonto.com` / `123456` | usuário criado na primeira subida |
| `SEED_DEMO` | `true` | carrega dados de exemplo se o banco estiver vazio |

O `.env` está no `.gitignore`. Nunca versione senhas.

## 6. Imagem do front sem a API

O nginx só resolve o endereço da API na hora da requisição, então a imagem do front sobe sozinha
(as telas mostram "Servidor indisponível" até a API existir). Para apontar para outro endereço:

```bash
docker run --rm -p 8080:8080 -e API_UPSTREAM=https://minha-api.run.app -e NGINX_RESOLVER=8.8.8.8 odontocare-web
```

Detalhes da API em [BACKEND.md](BACKEND.md).

## 7. Problemas comuns

| Erro | Solução |
|---|---|
| `Cannot connect to the Docker daemon` | Abra o Docker Desktop. |
| `defina POSTGRES_PASSWORD no .env` | Crie o `.env` a partir do `.env.example`. |
| `port is already allocated` (8080 ou 5432) | Pare o que usa a porta ou troque o lado esquerdo do mapeamento (`"8081:8080"`). |
| `api` reiniciando | Veja `docker compose logs api`; confira `JWT_SECRET` (mín. 32 caracteres) e se o `db` está healthy. |
| Rota `/agenda` dá 404 após F5 | Confira se o `nginx.conf` foi copiado (fallback `try_files`). |
