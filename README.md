# 🦷 OdontoCare — Sistema de Agendamento para Clínica Odontológica

Projeto Integrador IV — Engenharia de Software (UNIVILLE)
Autor: **Vitor Renner**

Sistema de agendamento para clínica odontológica: front-end em **React + Vite** (animações, tema claro/escuro, todas as telas) e **back-end em Java 21 + Spring Boot** (API REST, JWT, JPA, Flyway, PostgreSQL). Veja [docs/BACKEND.md](docs/BACKEND.md).

![React](https://img.shields.io/badge/React-18-61dafb) ![Vite](https://img.shields.io/badge/Vite-5-646cff) ![Docker](https://img.shields.io/badge/Docker-ready-2496ed) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)

---

## ✨ Principais funcionalidades

| Tela | O que faz |
|---|---|
| **Login** | Acesso com validação e animação de erro |
| **Dashboard** | Indicadores do dia/mês, consultas de hoje com ações rápidas, gráficos |
| **Agenda** | Grade semanal por horário, cor por dentista, clique para agendar/editar |
| **Agendamentos** | Lista completa com filtros, busca, ordenação, status e exportação CSV |
| **Pacientes** | Cadastro com máscara/validação de CPF, histórico de consultas |
| **Dentistas** | Cadastro com CRO, especialidade, cor na agenda, ativar/desativar |
| **Procedimentos** | Tabela de serviços com duração e valor |
| **Configurações** | Horário de funcionamento, sábado, backup/restauração |

Detalhes completos em **[docs/FUNCIONALIDADES.md](docs/FUNCIONALIDADES.md)**.

---

## 🚀 Rodar rápido

Precisa de **JDK 21+** e **Node 18+**. Em dois terminais:

```bash
# 1) back-end (porta 8080) — usa banco H2 local, sem instalar nada
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# 2) front (porta 5173), na raiz do projeto
npm install
npm run dev
```

Abra **http://localhost:5173** e entre com `admin@odonto.com` / `123456`.

### 🐳 Subir tudo com Docker (front + API + banco)

Um único comando sobe os três serviços. Precisa do **Docker Desktop** aberto (`docker --version` deve responder).

**1. Clone o repositório**

```bash
git clone https://github.com/GuilhermeElias991/Clinica_Sistem.git
cd Clinica_Sistem
```

**2. Crie o arquivo `.env`** a partir do modelo

```bash
cp .env.example .env          # Windows (PowerShell): Copy-Item .env.example .env
```

Abra o `.env` e troque **obrigatoriamente**:

| Variável | O que colocar |
|---|---|
| `POSTGRES_PASSWORD` | uma senha para o banco |
| `JWT_SECRET` | texto aleatório com **no mínimo 32 caracteres** (assina o login) |

O compose não sobe sem essas duas. O `.env` está no `.gitignore`: nunca envie senhas para o GitHub.

**3. Suba os containers**

```bash
docker compose up -d --build
```

Na primeira vez demora alguns minutos (baixa imagens e compila o front e a API).

**4. Acesse**

| Serviço | Endereço |
|---|---|
| Sistema (front) | **http://localhost:8080** — login `admin@odonto.com` / `123456` |
| API | http://localhost:8081/api (saúde: `/actuator/health`) |
| Banco PostgreSQL | `localhost:5432` |

As tabelas e os dados de exemplo são criados sozinhos na primeira subida.

**Comandos úteis**

```bash
docker compose ps              # ver se os 3 containers estão "healthy"
docker compose logs -f api     # acompanhar o log da API
docker compose down            # parar tudo (mantém os dados)
docker compose down -v         # parar e APAGAR o banco
```

**Problemas comuns**

| Sintoma | Solução |
|---|---|
| `defina POSTGRES_PASSWORD no .env` / `defina JWT_SECRET` | Crie o `.env` (passo 2) e preencha as duas variáveis |
| `Cannot connect to the Docker daemon` | Abra o Docker Desktop e espere "Engine running" |
| `port is already allocated` | Outra coisa usa 8080, 8081 ou 5432: pare-a ou mude a porta à esquerda no `docker-compose.yml` |
| Login falha logo após subir | A API leva ~15 s para iniciar; confira `docker compose ps` |

### 🌐 Publicar na web

Front no **Vercel** e API + banco no **Render**: passo a passo em **[docs/DEPLOY_WEB.md](docs/DEPLOY_WEB.md)**.

Outras formas (sem Docker, banco, migrações): **[docs/COMO_RODAR.md](docs/COMO_RODAR.md)**.

---

## 🧱 Tecnologias

- **React 18** + **Vite 5** — interface e servidor de desenvolvimento
- **React Router 6** — navegação entre telas
- **Framer Motion** — animações (transições, sidebar, modais, toasts, contadores)
- **Lucide React** — ícones
- **CSS puro** com variáveis (tema claro/escuro, responsivo)
- **Docker + nginx** — imagem de produção do front
- **Spring Boot 3.5** (Java 21) — API REST, Spring Security + JWT, Spring Data JPA
- **PostgreSQL 16** (produção) / **H2** (desenvolvimento) — migrações com **Flyway** (schema também em Alembic)

---

## 📁 Estrutura

```
Sistema-clinica/
├── src/
│   ├── main.jsx              # ponto de entrada (providers + router)
│   ├── App.jsx               # rotas e transições de página
│   ├── utils.js              # datas, máscaras, CPF, formatação
│   ├── styles.css            # estilo global, tema claro/escuro
│   ├── context/              # estado global
│   │   ├── AuthContext.jsx   # login/logout
│   │   ├── DataContext.jsx   # carrega e grava os dados via API (src/api.js)
│   │   └── ToastContext.jsx  # notificações
│   ├── components/
│   │   ├── Layout.jsx        # sidebar + topbar
│   │   ├── Modal.jsx         # modal e confirmação
│   │   ├── AppointmentModal.jsx  # formulário de agendamento
│   │   └── UI.jsx            # badge, campo, contador, estado vazio
│   └── pages/                # uma tela por arquivo
├── backend/                  # API Spring Boot (controllers, services, JPA, testes)
├── database/
│   ├── alembic.ini
│   ├── migrations/versions/  # migração inicial (0001)
│   └── sql/V1__schema_inicial.sql   # mesmo schema em SQL (Flyway)
├── docker/nginx.conf
├── Dockerfile
├── docker-compose.yml
├── cloudbuild.yaml           # deploy no Google Cloud Run
└── docs/                     # toda a documentação
```

---

## 🗺️ Próximos passos

- [x] Back-end em Java (Spring Boot) usando o schema em `database/`
- [x] Trocar o `localStorage` por chamadas à API
- [x] Login real com JWT e senha em BCrypt
- [ ] Cadastro de usuários e perfis de acesso
- [ ] Lembrete de consulta por WhatsApp/e-mail

---

## 📄 Documentação

Toda a documentação está na pasta **[docs/](docs/)**:

| Documento | Conteúdo |
|---|---|
| [COMO_RODAR.md](docs/COMO_RODAR.md) | Passo a passo para rodar (npm, IntelliJ, Docker, Alembic) |
| [FUNCIONALIDADES.md](docs/FUNCIONALIDADES.md) | Todas as funcionalidades do front |
| [TESTES.md](docs/TESTES.md) | Roteiro de testes funcionais (87 casos) |
| [ARQUITETURA.md](docs/ARQUITETURA.md) | Camadas, fluxo de dados e API planejada |
| [DEPLOY_WEB.md](docs/DEPLOY_WEB.md) | Publicar na web: front no Vercel, API e banco no Render |
| [BACKEND.md](docs/BACKEND.md) | API: endpoints, regras de negócio, variáveis, como rodar |
| [BANCO_DE_DADOS.md](docs/BANCO_DE_DADOS.md) | Diagrama, tabelas e regras do banco |
| [DOCKER.md](docs/DOCKER.md) | Imagem, compose e variáveis |
| [DEPLOY_GCP.md](docs/DEPLOY_GCP.md) | Publicação no Google Cloud Run |
| [DESENVOLVIMENTO.md](docs/DESENVOLVIMENTO.md) | Como o projeto foi feito e a ordem dos arquivos |
