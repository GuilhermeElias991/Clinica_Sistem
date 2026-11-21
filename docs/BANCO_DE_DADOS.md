# 🗄️ Banco de Dados

Schema inicial criado pela migração Alembic `0001` (`database/migrations/versions/20261114_0001_schema_inicial.py`).
O mesmo schema em SQL puro está em `database/sql/V1__schema_inicial.sql` e é aplicado pelo **Flyway** do back-end (`backend/src/main/resources/db/migration/`).

Banco alvo: **PostgreSQL 16**. A migração também roda em MySQL e SQLite.

---

## 1. Diagrama (ER)

```
┌──────────────┐        ┌────────────────────┐        ┌──────────────┐
│  pacientes   │1      N│    agendamentos    │N      1│  dentistas   │
│──────────────│────────│────────────────────│────────│──────────────│
│ id PK        │        │ id PK              │        │ id PK        │
│ nome         │        │ paciente_id FK     │        │ nome         │
│ cpf UNIQUE   │        │ dentista_id FK     │        │ cro UNIQUE   │
│ telefone     │        │ procedimento_id FK │        │ especialidade│
│ email        │        │ data               │        │ telefone     │
│ data_nasc.   │        │ hora_inicio        │        │ cor          │
│ convenio     │        │ hora_fim           │        │ ativo        │
│ observacoes  │        │ status             │        └──────────────┘
└──────────────┘        │ valor              │
                        │ observacoes        │        ┌──────────────┐
┌──────────────┐        │ criado_por FK      │N      1│procedimentos │
│   usuarios   │1      N│                    │────────│──────────────│
│──────────────│────────│                    │        │ id PK        │
│ id PK        │        └────────────────────┘        │ nome         │
│ nome         │                                      │ duracao_min  │
│ email UNIQUE │        ┌────────────────────┐        │ valor        │
│ senha_hash   │        │   configuracoes    │        │ ativo        │
│ cargo        │        │ (linha única id=1) │        └──────────────┘
│ ativo        │        └────────────────────┘
└──────────────┘
```

Todas as tabelas (menos `configuracoes`) têm `criado_em` e `atualizado_em`.

## 2. Tabelas

### usuarios
| Coluna | Tipo | Regra |
|---|---|---|
| id | BIGSERIAL | PK |
| nome | VARCHAR(120) | obrigatório |
| email | VARCHAR(160) | obrigatório, **único** |
| senha_hash | VARCHAR(255) | hash BCrypt (nunca senha pura) |
| cargo | VARCHAR(40) | padrão `RECEPCAO` |
| ativo | BOOLEAN | padrão `true` |

### pacientes
| Coluna | Tipo | Regra |
|---|---|---|
| id | BIGSERIAL | PK |
| nome | VARCHAR(150) | obrigatório, índice |
| cpf | VARCHAR(14) | obrigatório, **único** (formato 000.000.000-00) |
| telefone | VARCHAR(20) | obrigatório |
| email | VARCHAR(160) | opcional |
| data_nascimento | DATE | opcional |
| convenio | VARCHAR(60) | padrão `Particular` |
| observacoes | TEXT | alergias, cuidados |

### dentistas
| Coluna | Tipo | Regra |
|---|---|---|
| id | BIGSERIAL | PK |
| nome | VARCHAR(150) | obrigatório |
| cro | VARCHAR(30) | obrigatório, **único** |
| especialidade | VARCHAR(80) | obrigatório |
| telefone | VARCHAR(20) | opcional |
| cor | VARCHAR(7) | hex da agenda, padrão `#0ea5e9` |
| ativo | BOOLEAN | padrão `true` |

### procedimentos
| Coluna | Tipo | Regra |
|---|---|---|
| id | BIGSERIAL | PK |
| nome | VARCHAR(120) | obrigatório |
| duracao_min | INTEGER | **≥ 10** |
| valor | NUMERIC(10,2) | **≥ 0** |
| ativo | BOOLEAN | padrão `true` |

### agendamentos
| Coluna | Tipo | Regra |
|---|---|---|
| id | BIGSERIAL | PK |
| paciente_id | BIGINT | FK → pacientes (**CASCADE**) |
| dentista_id | BIGINT | FK → dentistas (**CASCADE**) |
| procedimento_id | BIGINT | FK → procedimentos (**RESTRICT**: não exclui procedimento em uso) |
| data | DATE | obrigatório |
| hora_inicio / hora_fim | TIME | `hora_fim > hora_inicio` |
| status | VARCHAR(20) | `agendado`, `confirmado`, `concluido`, `cancelado`, `faltou` |
| valor | NUMERIC(10,2) | valor cobrado (guarda o preço da época) |
| observacoes | TEXT | |
| criado_por | BIGINT | FK → usuarios (SET NULL) |

Índices: `(data, dentista_id)`, `(data, paciente_id)`, `(status)`, usados na busca de conflito e nos filtros da agenda.

### configuracoes
Linha única (`CHECK id = 1`): `nome_clinica`, `abertura`, `fechamento`, `intervalo_min` (15/30/60), `atende_sabado`.

## 3. Dados iniciais da migração
- 1 linha de configuração (08:00–18:00, intervalo de 30 min, sem sábado).
- 7 procedimentos padrão.
- **Nenhum usuário**: o back-end cria o admin na primeira subida (`ADMIN_EMAIL`/`ADMIN_PASSWORD`), com senha em BCrypt.

## 4. Mapeamento front ↔ banco

| Front (`DataContext`) | Banco |
|---|---|
| `paciente.nascimento` | `pacientes.data_nascimento` |
| `paciente.obs` | `pacientes.observacoes` |
| `procedimento.duracao` | `procedimentos.duracao_min` |
| `agendamento.hora` | `agendamentos.hora_inicio` |
| (calculado) | `agendamentos.hora_fim` |
| `config.clinica` | `configuracoes.nome_clinica` |
| `config.intervalo` | `configuracoes.intervalo_min` |
| `config.sabado` | `configuracoes.atende_sabado` |

## 5. Regra de conflito (para o back-end)

Uma consulta conflita se, na **mesma data** e com status diferente de `cancelado`, existir outra do **mesmo dentista ou do mesmo paciente** com horário sobreposto:

```sql
SELECT 1 FROM agendamentos
WHERE data = :data
  AND status <> 'cancelado'
  AND (dentista_id = :dentista OR paciente_id = :paciente)
  AND hora_inicio < :hora_fim
  AND :hora_inicio < hora_fim
  AND id <> :id_editando;
```

## 6. Como criar novas migrações

```bash
cd database
alembic revision -m "adiciona tabela de prontuarios"
# edite o arquivo gerado em migrations/versions/
alembic upgrade head
```

No back-end, crie o SQL correspondente em `backend/src/main/resources/db/migration/V2__descricao.sql` — o Flyway aplica ao subir a API.
