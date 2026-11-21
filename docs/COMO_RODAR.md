# ▶️ Como rodar o OdontoCare

> Comandos executados na **raiz do projeto** (pasta `Sistema-clinica`).

Existem 4 jeitos. O sistema agora tem **front + back-end**: para desenvolver, use o **jeito 1** (dois terminais).

---

## Jeito 1 — Desenvolvimento (back-end + front)

**Precisa ter:** JDK 21+ (`java -version`) e Node.js 18+ (`node -v`).

**Terminal 1 — back-end** (porta 8080, banco H2 em `backend/data/`, dados de exemplo automáticos):
```bash
cd backend
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

**Terminal 2 — front** (na raiz):
```bash
npm install      # só na primeira vez
npm run dev
```

Abra **http://localhost:5173**. O Vite repassa `/api` para a porta 8080. Se o back-end estiver fora, as telas mostram "Servidor indisponível".

Para trocar a porta da API: `VITE_API_PROXY=http://localhost:9090 npm run dev`. Detalhes em [BACKEND.md](BACKEND.md).

**Login:** `admin@odonto.com` / `123456` (ou clique na caixa "Acesso demo" na tela de login).

> Para apagar os dados locais, pare a API e apague a pasta `backend/data/`.

> O servidor do Vite funciona como um *live server*: ao salvar qualquer arquivo, a página atualiza sozinha.
> Para parar: `Ctrl + C` no terminal.

### Pelo IntelliJ
1. Abra o `package.json`.
2. Clique na setinha verde ▶ da linha `"dev": "vite"` → **Run 'dev'**.
3. ⌘ + clique no link `http://localhost:5173/` que aparece no painel de baixo.

> ⚠️ Não use a setinha do `"preview"`: ela só serve a versão compilada (`npm run build`) e abre em branco se o build não existir.

### Outros comandos
| Comando | O que faz |
|---|---|
| `npm run build` | gera a versão de produção em `dist/` |
| `npm run preview` | serve a pasta `dist/` em http://localhost:4173 |

---

## Jeito 2 — Docker (front + API + PostgreSQL)

**Precisa ter:** Docker Desktop aberto.

```bash
cp .env.example .env          # depois edite o .env: senha do banco e JWT_SECRET
docker compose up -d --build
```

| Serviço | Endereço | O que é |
|---|---|---|
| `web` | http://localhost:8080 | front compilado servido pelo nginx |
| `api` | http://localhost:8081/api | back-end Spring Boot (Flyway cria as tabelas) |
| `db` | localhost:5432 | PostgreSQL 16 |

Comandos úteis:
```bash
docker compose ps             # ver o que está rodando
docker compose logs api       # ver a API e a migração
docker compose down           # parar tudo
docker compose down -v        # parar e APAGAR o banco
```

---

## Jeito 3 — Só as migrações do banco (Alembic)

Para aplicar o schema num PostgreSQL que você já tem:

```bash
cd database
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # ajuste DATABASE_URL
alembic upgrade head          # cria as tabelas
```

| Comando | O que faz |
|---|---|
| `alembic current` | mostra a versão aplicada |
| `alembic history` | lista as migrações |
| `alembic downgrade -1` | desfaz a última |
| `alembic upgrade head --sql` | só mostra o SQL, sem executar |
| `alembic revision -m "descricao"` | cria uma nova migração |

### Back-end
O back-end já traz o mesmo schema em `backend/src/main/resources/db/migration/V1__schema_inicial.sql` e o **Flyway** aplica sozinho. Se o banco foi criado antes pelo Alembic, veja a nota em [BACKEND.md](BACKEND.md).

---

## Jeito 4 — Google Cloud

Veja [DEPLOY_GCP.md](DEPLOY_GCP.md).

---

## 🔒 Dados sensíveis

- Senhas e URLs de banco ficam **só** no `.env` (que está no `.gitignore`).
- Os arquivos `.env.example` são modelos sem senha real — esses vão para o git.
- Backups exportados pelo sistema (`backup-odonto-*.json`) e CSVs de agendamentos contêm dados de pacientes e também são ignorados pelo git.

---

## 🧯 Problemas comuns

| Sintoma | Solução |
|---|---|
| Página em branco em `localhost:4173` | Você rodou `preview`. Pare (■) e rode `dev`. |
| `command not found: npm` | Instale o Node.js (nodejs.org). |
| Porta 5173 ocupada | O Vite usa a próxima livre (5174…). Veja o link no terminal. |
| "Servidor indisponível" | O back-end não está rodando. Suba-o (`cd backend && ./mvnw spring-boot:run`). |
| Dados "sumiram" | Ficam no banco do back-end. Em `backend/data/` (H2) ou no PostgreSQL. Use **Configurações → Exportar backup**. |
| Quero os dados de exemplo de volta | **Configurações → Restaurar dados de exemplo**. |
| `DATABASE_URL` não definida | Crie `database/.env` a partir do `.env.example`. |
