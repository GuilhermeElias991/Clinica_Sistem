# 🌐 Publicar na web (Vercel + Render)

O sistema tem duas partes e cada uma vai para um lugar:

| Parte | Onde | Por quê |
|---|---|---|
| **Front** (React/Vite) | **Vercel** | arquivos estáticos, deploy direto do GitHub |
| **API** (Spring Boot) + **PostgreSQL** | **Render** | o Vercel não roda servidor Java nem banco |

```
Navegador ──► Vercel (front) ──► fetch https://odonto-api.onrender.com/api ──► Render (API) ──► PostgreSQL (Render)
```

Faça **primeiro o Render** (para ter a URL da API) e depois o Vercel.

---

## 1. API + banco no Render

1. Crie conta em **render.com** (pode entrar com o GitHub).
2. **New → Blueprint** → escolha o repositório `GuilhermeElias991/Clinica_Sistem`. O Render lê o `render.yaml` e cria o banco `odonto-db` e o serviço `odonto-api`.
3. Quando pedir, preencha:
   - `ADMIN_PASSWORD` — a senha do administrador (**escolha uma forte**, não use `123456`);
   - `CORS_ORIGINS` — por enquanto `http://localhost:5173` (você troca no passo 3).
4. Aguarde o deploy (a primeira vez leva uns 5–10 min). Quando ficar **Live**, copie a URL do serviço, algo como `https://odonto-api.onrender.com`.
5. Teste: abra `https://odonto-api.onrender.com/actuator/health` — deve mostrar `{"status":"UP"}`.

> O plano gratuito **dorme após 15 min sem uso** (a primeira requisição depois disso demora ~50 s) e o banco gratuito **expira em 30 dias**. Para uso contínuo, mude `plan` para um pago.

---

## 2. Front no Vercel

### Opção A — pelo site (sem instalar nada)
1. Em **vercel.com/new**, importe o repositório `Clinica_Sistem`.
2. O Vercel detecta o Vite sozinho (o `vercel.json` já está no projeto).
3. Em **Environment Variables**, adicione:

| Nome | Valor |
|---|---|
| `VITE_API_URL` | `https://odonto-api.onrender.com/api` (a URL do passo 1, **terminando em `/api`**) |
| `VITE_SHOW_DEMO` | `false` (esconde a senha padrão da tela de login) |

4. **Deploy**. Você recebe um endereço como `https://clinica-sistem.vercel.app`.

### Opção B — pela linha de comando
```bash
npm i -g vercel
vercel login
vercel link
vercel env add VITE_API_URL production      # cole https://odonto-api.onrender.com/api
vercel env add VITE_SHOW_DEMO production    # digite false
vercel --prod
```

---

## 3. Liberar o front na API (CORS)

No painel do Render → `odonto-api` → **Environment** → edite `CORS_ORIGINS` para o endereço do Vercel
(sem barra no final), por exemplo:

```
https://clinica-sistem.vercel.app
```

Salve; o Render reinicia a API. Pronto: abra o endereço do Vercel e entre com `admin@odonto.com` e a senha que você definiu em `ADMIN_PASSWORD`.

---

## Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| "Servidor indisponível" no login | `VITE_API_URL` errada ou ausente. Confira que termina em `/api` e **refaça o deploy** do Vercel (variáveis `VITE_*` só entram no build). |
| Erro de CORS no console do navegador | `CORS_ORIGINS` no Render não é exatamente o endereço do Vercel (sem `/` no final). |
| Primeiro acesso demora ~1 min | API gratuita do Render estava dormindo. |
| Página 404 ao recarregar `/agenda` | O `vercel.json` precisa estar na raiz do projeto (ele faz o fallback para o `index.html`). |
| Login não aceita a senha | Vale a que você definiu em `ADMIN_PASSWORD`. Ela só é gravada na **primeira** subida; para trocá-la depois, apague o banco no Render ou altere o usuário direto nele. |

## Segurança
- `JWT_SECRET` é gerado pelo Render; não precisa criar.
- Nunca coloque senhas no repositório. O `render.yaml` marca `ADMIN_PASSWORD` e `CORS_ORIGINS` como `sync: false` para você digitar no painel.
- Com o site público, qualquer pessoa vê a tela de login: use senha forte.
