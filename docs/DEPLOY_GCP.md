# ☁️ Deploy no Google Cloud (Cloud Run)

O front roda num container nginx (porta 8080). O **Cloud Run** executa esse container e entrega uma URL pública `https://...run.app`. Sem tráfego, o serviço fica parado (escala a zero).

Arquivos usados: `Dockerfile`, `docker/nginx.conf`, `.dockerignore` e `cloudbuild.yaml`.

---

## 1. Pré-requisitos (uma vez só)

1. Ter um projeto no Google Cloud com **faturamento ativo** (os créditos do programa servem).
2. Instalar o **gcloud CLI**: https://cloud.google.com/sdk/docs/install
3. No terminal:

```bash
gcloud auth login
gcloud config set project SEU_PROJECT_ID

# APIs necessárias
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com

# Repositório de imagens (São Paulo)
gcloud artifacts repositories create odontocare \
  --repository-format=docker --location=southamerica-east1
```

4. Dar ao Cloud Build permissão para publicar no Cloud Run:

```bash
PROJECT_NUMBER=$(gcloud projects describe $(gcloud config get-value project) --format='value(projectNumber)')
SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

gcloud projects add-iam-policy-binding $(gcloud config get-value project) --member="serviceAccount:$SA" --role="roles/run.admin"
gcloud projects add-iam-policy-binding $(gcloud config get-value project) --member="serviceAccount:$SA" --role="roles/iam.serviceAccountUser"
gcloud projects add-iam-policy-binding $(gcloud config get-value project) --member="serviceAccount:$SA" --role="roles/artifactregistry.writer"
```

> Em projetos antigos, a conta do Cloud Build pode ser `PROJECT_NUMBER@cloudbuild.gserviceaccount.com`. Se o deploy der erro de permissão, repita os comandos com ela.

---

## 2. Publicar

Na raiz do projeto:

```bash
gcloud builds submit --config cloudbuild.yaml
```

No final aparece a URL do serviço, ex.: `https://odontocare-web-xxxxx.southamerica-east1.run.app`.

Para publicar uma nova versão, é só rodar o mesmo comando de novo.

---

## 3. Alternativa sem Cloud Build (um comando)

```bash
gcloud run deploy odontocare-web --source . --region southamerica-east1 --allow-unauthenticated --port 8080
```

O próprio Cloud Run faz o build a partir do `Dockerfile`.

---

## 4. Testar a imagem localmente antes

```bash
docker build -t odontocare-web .
docker run --rm -p 8080:8080 odontocare-web
# abra http://localhost:8080
```

---

## 5. Back-end e banco de dados no Google Cloud

O front chama a API em `/api`. Para publicar o back-end (`backend/`):

1. Criar um **Cloud SQL for PostgreSQL**.
2. Guardar a senha no **Secret Manager** (nunca no git nem no `cloudbuild.yaml`).
3. As migrações rodam sozinhas: o **Flyway** da API aplica `V1__schema_inicial.sql` ao subir.
4. Publicar o back-end também no Cloud Run (`gcloud run deploy odontocare-api --source backend`), conectado ao Cloud SQL (`--add-cloudsql-instances`), com `SPRING_DATASOURCE_*` e `JWT_SECRET` vindos do Secret Manager.
5. No serviço do front, definir `API_UPSTREAM=https://<url-da-api>` e `NGINX_RESOLVER=8.8.8.8`.

---

## 6. Custos e limpeza

- Cloud Run cobra por uso; com `min-instances=0` e pouco acesso o custo fica perto de zero.
- O Artifact Registry cobra pelo armazenamento das imagens. Apague as antigas de vez em quando.
- Para remover tudo:

```bash
gcloud run services delete odontocare-web --region southamerica-east1
gcloud artifacts repositories delete odontocare --location southamerica-east1
```
