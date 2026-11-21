# ⚙️ Back-end — OdontoCare API

API REST em **Java 21 + Spring Boot 3.5** que substitui o `localStorage` do front.
Fica na pasta [`backend/`](../backend).

| Item | Tecnologia |
|---|---|
| Web/REST | Spring Web (MVC) |
| Persistência | Spring Data JPA (Hibernate) |
| Migrações | **Flyway** — `V1__schema_inicial.sql` é o mesmo schema de `database/sql/` |
| Banco | **PostgreSQL 16** em produção · **H2** (arquivo) para rodar sem instalar nada |
| Segurança | Spring Security + **JWT (HS256)** · senhas em **BCrypt** |
| Validação | Bean Validation + regras nos serviços |
| Testes | JUnit 5 + MockMvc (testes de integração com H2 em memória) |

---

## 1. Rodar

**Precisa ter:** JDK 21 ou mais novo. O Maven vem junto (`mvnw`).

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

A API sobe em **http://localhost:8080**. Sem nenhuma configuração ela:

- cria um banco **H2 em arquivo** em `backend/data/` (fica fora do git);
- aplica as migrações do Flyway;
- cria o usuário **admin@odonto.com / 123456** (senha guardada em BCrypt);
- carrega os mesmos **dados de exemplo** que o front tinha (dentistas, pacientes, agendamentos da semana).

Em outro terminal, na raiz do projeto, `npm run dev` — o Vite já faz proxy de `/api` para a porta 8080.

### Usando PostgreSQL

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/odonto \
SPRING_DATASOURCE_USERNAME=odonto \
SPRING_DATASOURCE_PASSWORD=sua_senha \
JWT_SECRET=uma-chave-bem-grande-com-pelo-menos-32-caracteres \
./mvnw spring-boot:run
```

Ou suba tudo com Docker: `docker compose up -d --build` na raiz (veja [DOCKER.md](DOCKER.md)).

> Se o banco já tiver sido criado antes pelo **Alembic**, rode a API uma vez com
> `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` e `SPRING_FLYWAY_BASELINE_VERSION=1`
> para o Flyway adotar o schema existente.

### Testes

```bash
cd backend
./mvnw test
```

### Variáveis de ambiente

Modelo completo em [`backend/.env.example`](../backend/.env.example).

| Variável | Padrão | Para quê |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | H2 em `./data` | Conexão com o banco |
| `JWT_SECRET` | chave de desenvolvimento | Assinatura do token — **troque em produção** (mín. 32 caracteres) |
| `JWT_EXPIRATION_HOURS` | `8` | Validade do login |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | `admin@odonto.com` / `123456` / `Recepção` | Usuário criado na primeira subida |
| `SEED_DEMO` | `true` | Carrega dados de exemplo se o banco estiver vazio |
| `CORS_ORIGINS` | `http://localhost:5173,...` | Origens liberadas (só precisa se o front estiver em outro domínio) |
| `APP_TIMEZONE` | `America/Sao_Paulo` | Define o "hoje" (não agendar no passado) |
| `PORT` | `8080` | Porta HTTP |

---

## 2. Estrutura

```
backend/
├── pom.xml
├── Dockerfile
└── src/main/java/br/univille/odontocare/
    ├── config/       SecurityConfig (JWT, CORS), AppProperties, DataInitializer (admin + exemplo)
    ├── domain/       Entidades JPA: Paciente, Dentista, Procedimento, Agendamento, Configuracao, Usuario
    ├── repository/   Spring Data (inclui a busca de conflito de horário)
    ├── dto/          Objetos de entrada/saída no formato que o front já usava
    ├── service/      Regras de negócio
    ├── web/          Controllers REST
    └── exception/    ApiException + tratamento global de erros
```

---

## 3. Endpoints

Todos começam com `/api`. Com exceção do login, todos exigem o header
`Authorization: Bearer <token>`.

### Autenticação
| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/login` | `{email, senha}` → `{token, expiraEmSegundos, usuario}` · 401 se inválido |
| GET | `/auth/me` | Usuário logado |
| PUT | `/auth/me` | `{nome}` — altera o nome (tela Configurações) |

### Cadastros
| Método | Rota | Descrição |
|---|---|---|
| GET / POST | `/pacientes` | Listar (por nome) / criar |
| GET / PUT / DELETE | `/pacientes/{id}` | Buscar / editar / excluir (**apaga os agendamentos** do paciente) |
| GET / POST | `/dentistas` | Listar / criar |
| GET / PUT / DELETE | `/dentistas/{id}` | Buscar / editar / excluir (**apaga os agendamentos** do dentista) |
| PATCH | `/dentistas/{id}` | `{ativo: false}` — ativar/desativar |
| GET / POST | `/procedimentos` | Listar / criar |
| GET / PUT / DELETE | `/procedimentos/{id}` | Buscar / editar / excluir (**409** se tiver agendamento) |

### Agendamentos
| Método | Rota | Descrição |
|---|---|---|
| GET | `/agendamentos?inicio=&fim=&dentistaId=&pacienteId=&status=` | Lista com filtros (todos opcionais) |
| GET | `/agendamentos/horarios-livres?data=&dentistaId=&procedimentoId=&pacienteId=&ignorarId=` | Slots do expediente com `livre: true/false` |
| GET | `/agendamentos/{id}` | Buscar |
| POST | `/agendamentos` | Criar · **409 Conflict** se o horário estiver ocupado |
| PUT | `/agendamentos/{id}` | Editar (mesmas validações) |
| PATCH | `/agendamentos/{id}/status` | `{status}` — confirmar, concluir, faltou, cancelar |
| DELETE | `/agendamentos/{id}` | Excluir |

Corpo de criação/edição:
```json
{ "pacienteId": 1, "dentistaId": 3, "procedimentoId": 2, "data": "2026-11-23", "hora": "10:00", "status": "agendado", "obs": "" }
```
A resposta traz também `horaFim` (calculada pela duração do procedimento) e `valor` (preço do procedimento **no dia do agendamento**).

### Sistema
| Método | Rota | Descrição |
|---|---|---|
| GET / PUT | `/configuracoes` | `{clinica, abertura, fechamento, intervalo, sabado}` |
| GET | `/dashboard?mes=2026-11` | Consultas de hoje, pacientes, faturamento, comparecimento, semana, por dentista |
| GET | `/backup` | Exporta tudo em JSON |
| POST | `/backup` | Importa um JSON (substitui os dados; aceita também backups antigos do `localStorage`) |
| POST | `/backup/restaurar-exemplo` | Apaga tudo e recarrega os dados de exemplo |
| GET | `/actuator/health` | Saúde da aplicação (público) |

### Formato de erro
```json
{ "status": 409, "message": "Conflito: dentista ocupado às 10:00 (Marcos Pereira)", "errors": { "hora": "..." }, "timestamp": "..." }
```
`errors` traz a mensagem por campo — o front usa isso para destacar o campo no formulário.

---

## 4. Regras de negócio implementadas

| Regra | Onde | Resposta |
|---|---|---|
| Conflito: mesmo dentista **ou** mesmo paciente com horário sobreposto (ignora cancelados) | `AgendamentoService` | 409 |
| Duas recepcionistas ao mesmo tempo: paciente e dentista ficam travados (`SELECT ... FOR UPDATE`) durante a gravação | `AgendamentoService` | 409 para a segunda |
| Não abre domingo; sábado só se `atende_sabado` | `AgendamentoService` | 400 |
| Consulta inteira dentro do expediente (`abertura` ≤ início, fim ≤ `fechamento`) | `AgendamentoService` | 400 |
| Não agenda no passado (na criação ou ao mudar a data) | `AgendamentoService` | 400 |
| Dentista inativo não recebe novos agendamentos | `AgendamentoService` | 400 |
| Reativar um agendamento cancelado verifica conflito de novo | `AgendamentoService` | 409 |
| CPF com dígito verificador válido e único | `PacienteService` | 400 / 409 |
| CRO único (sem diferenciar maiúsculas) | `DentistaService` | 409 |
| Procedimento ≥ 10 min, valor ≥ 0; não exclui se estiver em uso | `ProcedimentoService` | 400 / 409 |
| Intervalo da agenda 15/30/60 e abertura antes do fechamento | `ConfiguracaoService` | 400 |
| Login com BCrypt e tempo de resposta igual para e-mail inexistente | `AuthService` | 401 |
