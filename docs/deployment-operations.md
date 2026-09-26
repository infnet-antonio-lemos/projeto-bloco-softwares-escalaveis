# Implantação e Operação

## 1. Conteinerização

Cada um dos quatro serviços tem seu próprio `Dockerfile`, com build multi-estágio.

## 2. Pipeline

### CI — `.github/workflows/ci.yml`

Dispara em todo push e em PRs para `main`.

| Job | O que faz |
| --- | --- |
| `java` (matriz de 3) | `./mvnw verify` por módulo |
| `frontend` | `npm ci`, lint, build |
| `manifests` | `kubectl kustomize` + `kubeconform` |
| `images` (matriz de 4) | `docker build` sem publicar |

### CD — `.github/workflows/cd.yml`

Dispara em `main` e por `workflow_dispatch`. Publica as quatro imagens no GHCR com duas
tags de propósitos distintos:

| Tag | Uso |
| --- | --- |
| `sha-<commit>` | **Imutável.** É a que o manifesto de produção referencia. |
| `latest` | Conveniência para `docker run`. **Nunca** em produção. |

## 3. Observabilidade

**Existe hoje:**

- `/actuator/health` com grupos `liveness` e `readiness`, usados pelas probes do
  Kubernetes;
- **correlation id** propagado de ponta a ponta — entra pelo header `X-Correlation-Id`
  (ou é gerado), vai para o MDC, aparece em todo log da requisição por causa de
  `logging.pattern.level`, viaja no envelope do evento e é reposto no MDC pelo consumidor,
  de modo que os eventos emitidos em cascata herdam o mesmo identificador;
- **envelope de evento com metadados de rastreio** — `eventId`, `occurredAt` e
  `correlationId` viajam em toda mensagem, então a cadeia disparada por uma requisição é
  reconstituível a partir dos logs dos serviços envolvidos.