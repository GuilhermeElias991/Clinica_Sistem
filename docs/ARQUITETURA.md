# 🏛️ Arquitetura

## 1. Visão geral

```
┌──────────────────────────── Navegador ────────────────────────────┐
│                                                                   │
│  main.jsx                                                         │
│   └─ BrowserRouter                                                │
│       └─ ToastProvider        (notificações)                      │
│           └─ AuthProvider     (usuário logado)                    │
│               └─ DataProvider (estado + chamadas à API)           │
│                   └─ App.jsx  (rotas)                             │
│                       ├─ /login            → Login                │
│                       └─ /* (protegido)    → Layout               │
│                            ├─ /               Dashboard           │
│                            ├─ /agenda         Agenda              │
│                            ├─ /agendamentos   Agendamentos        │
│                            ├─ /pacientes      Pacientes           │
│                            ├─ /dentistas      Dentistas           │
│                            ├─ /procedimentos  Procedimentos       │
│                            └─ /configuracoes  Configurações       │
│                                                                   │
│                     src/api.js  (fetch + JWT)                     │
└───────────────────────────────────────────────────────────────────┘
                    ▲ HTTP /api (JSON + Bearer JWT)
                    ▼
        ┌──── Back-end Java (Spring Boot) ────┐      ┌─ PostgreSQL ─┐
        │ Controllers → Services → Repos (JPA)│ ───► │ schema V1     │
        └─────────────────────────────────────┘      └───────────────┘
```

## 2. Camadas do front

| Camada | Pasta | Responsabilidade |
|---|---|---|
| Entrada | `main.jsx`, `App.jsx` | Providers, rotas, proteção de rota, transições |
| Estado/regras | `context/` | Autenticação, dados, regras de negócio, notificações |
| Componentes | `components/` | Peças reutilizáveis sem regra de negócio própria (exceto `AppointmentModal`) |
| Telas | `pages/` | Uma tela por rota; consomem os contexts |
| Utilidades | `utils.js` | Funções puras (sem React) |
| Estilo | `styles.css` | Tokens de tema, componentes visuais, responsivo |

## 3. Fluxo de dados

1. A tela chama `useData()` e recebe os dados + funções (`agendamentosApi.add`, `findConflict`…).
2. A função atualiza o estado no `DataContext`.
3. A função chama a API (`src/api.js`); com a resposta o estado é atualizado (erros 400/409 viram aviso e destaque no campo).
4. Todas as telas que usam aquele dado re-renderizam sozinhas.

## 4. API interna do `DataContext`

| Função | Descrição |
|---|---|
| `pacientes`, `dentistas`, `procedimentos`, `agendamentos`, `config` | Dados atuais |
| `xxxApi.add(obj)` / `update(id, dados)` / `remove(id)` | CRUD genérico para cada coleção |
| `byId(colecao, id)` | Busca por id |
| `findConflict({...})` | Retorna o agendamento em conflito (mesmo dentista ou paciente, horário sobreposto) |
| `removePaciente(id)` / `removeDentista(id)` | Exclusão em cascata dos agendamentos |
| `setConfig(obj)` | Atualiza as configurações |
| `exportJSON()` / `importJSON(txt)` / `resetAll()` | Backup e restauração |

## 5. Integração com o back-end (implementada)

Só `DataContext.jsx`, `AuthContext.jsx` e `api.js` falam com a API; as telas continuam iguais. A lista completa e atualizada está em [BACKEND.md](BACKEND.md). Endpoints originais planejados:

| Método | Rota | Uso |
|---|---|---|
| POST | `/api/auth/login` | Retorna JWT |
| GET/POST | `/api/pacientes` | Listar / criar |
| PUT/DELETE | `/api/pacientes/{id}` | Editar / excluir |
| GET/POST | `/api/dentistas` | Listar / criar |
| PUT/PATCH/DELETE | `/api/dentistas/{id}` | Editar / ativar-desativar / excluir |
| GET/POST | `/api/procedimentos` | Listar / criar |
| GET | `/api/agendamentos?inicio=&fim=&dentistaId=&status=` | Filtros da agenda e da lista |
| POST | `/api/agendamentos` | Criar (o back valida conflito → **409 Conflict**) |
| PATCH | `/api/agendamentos/{id}/status` | Confirmar, concluir, faltou, cancelar |
| GET | `/api/agendamentos/horarios-livres?data=&dentistaId=&procedimentoId=` | Botões de horário |
| GET/PUT | `/api/configuracoes` | Expediente |
| GET | `/api/dashboard?mes=` | Indicadores |

> A validação de conflito existe **também** no back-end (feito, com trava no banco), porque duas recepcionistas podem agendar ao mesmo tempo.

## 6. Infraestrutura

| Ambiente | Como roda |
|---|---|
| Desenvolvimento | `npm run dev` (Vite, porta 5173) |
| Local com Docker | `docker compose up` (nginx 8080 + Postgres 5432 + migração) |
| Produção | Google Cloud Run (container nginx, porta 8080) |
