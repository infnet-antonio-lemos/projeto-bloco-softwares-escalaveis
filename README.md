# Pet Clinic

Aplicação de cadastro de tutores (owners), pets e **agendamento de consultas**,
construída como uma arquitetura de microsserviços **orientada a eventos** com Spring
Boot, Spring Cloud e RabbitMQ, com frontend React.

Modelada com DDD — veja o mapa de contexto em
[docs/ddd-context-map.md](docs/ddd-context-map.md), a arquitetura distribuída em
[docs/microservice-architecture.md](docs/microservice-architecture.md), a comunicação
por eventos em [docs/event-driven-architecture.md](docs/event-driven-architecture.md)
e a implantação em [k8s/README.md](k8s/README.md).

Resumo por entrega, com inventário do que foi construído e revisão de escopo:
[docs/tp4.md](docs/tp4.md) (arquitetura orientada a eventos) e
[docs/tp5.md](docs/tp5.md) (implantação e produção).

## Arquitetura

| Módulo | Porta | Papel |
| --- | --- | --- |
| [api-gateway](api-gateway/) | 8080 | Ponto único de entrada da API |
| [backend](backend/) | 8081 | Monólito: tutores e pets (contexto *Patient Registry*) |
| [appointment-service](appointment-service/) | 8082 | Microsserviço de consultas (contexto *Scheduling*) |
| [frontend](frontend/) | 3000 | SPA React (nginx) |
| PostgreSQL | 5432 | Databases `petclinicdb` e `appointmentsdb` |
| RabbitMQ | 5672 / 15672 | Message broker (UI de gerenciamento na 15672) |

O frontend fala **apenas com o gateway**, que roteia cada prefixo de rota para o
serviço correspondente.

Entre os serviços, a comunicação é **assíncrona por eventos**: o monólito publica
`pet.*` e `owner.*` num topic exchange, sem saber quem escuta; o `appointment-service`
declara suas próprias filas, consome esses eventos para manter uma projeção local dos
pets — e por isso agenda consultas sem chamar o monólito — e publica `appointment.*`
no seu próprio exchange, disponível para qualquer assinante futuro.

O cliente OpenFeign continua no código, mas deixou de ser dependência do caminho
crítico: virou fallback para quando a projeção ainda não conhece um pet. Os detalhes
estão em [docs/event-driven-architecture.md](docs/event-driven-architecture.md).

## Stack

- **Backend** — Java 21, Spring Boot 4.0.7, Spring Data JPA, Hibernate Envers
  (auditoria/histórico), Lombok, Bean Validation
- **Spring Cloud 2025.1.2** — Spring Cloud Gateway
  (roteamento), OpenFeign + LoadBalancer (fallback síncrono entre serviços)
- **Mensageria** — RabbitMQ 4 + Spring AMQP (topic exchanges, roteamento por padrão de
  routing key, competing consumers, projeção alimentada por eventos)
- **Banco** — H2 in-memory (padrão) ou PostgreSQL (profile `postgres`)
- **Frontend** — React 19, React Router 7, Vite
- **Infra** — Docker, Kubernetes (Kustomize), GitHub Actions

## Pré-requisitos

Para rodar localmente (sem Docker):

- JDK 21
- Node.js 20+ e npm

Para rodar via containers, num cluster Kubernetes local:

- Docker
- minikube e kubectl

Todos os módulos usam o Maven Wrapper (`./mvnw`), então não é necessário instalar o
Maven na máquina.

---

## Kubernetes (recomendado)

### Subir

```bash
minikube start --cpus=4 --memory=6g
./k8s/deploy-local.sh dev
```

### Acessar

```bash
kubectl -n petclinic port-forward svc/frontend    3000:80
kubectl -n petclinic port-forward svc/api-gateway 8080:8080
kubectl -n petclinic port-forward svc/rabbitmq    15672:15672
```

- **SPA**: http://localhost:3000
- **API (gateway)**: http://localhost:8080
- **RabbitMQ Management**: http://localhost:15672 (`petclinic` / `petclinic`)


### Parar

```bash
kubectl delete namespace petclinic     # remove a stack
minikube stop                          # para o cluster
```

## API

Todas as rotas passam pelo gateway em `http://localhost:8080`.

| Recurso | Serviço | Endpoints |
| --- | --- | --- |
| `/api/owners` | backend | CRUD + `/{id}/history` (auditoria Envers) |
| `/api/pets` | backend | CRUD + `/{id}/history`, filtros `?ownerId=` e `?species=` |
| `/api/appointments` | appointment-service | CRUD + `PATCH /{id}/status`, filtros `?petId=`, `?ownerId=` e `?status=` |

A referência completa dos endpoints de consultas, com exemplos de payload e a tabela
de códigos de erro, está em
[docs/microservice-architecture.md](docs/microservice-architecture.md#5-endpoints-da-api-rest).

Há também uma collection do Postman em
[docs/petclinic.postman_collection.json](docs/petclinic.postman_collection.json)
cobrindo os três recursos.

### Demonstrando a comunicação entre serviços

O agendamento envia **apenas o `petId`** — o nome do pet e os dados do tutor vêm da
projeção local que os eventos mantêm atualizada:

```bash
curl -X POST http://localhost:8080/api/appointments \
  -H 'Content-Type: application/json' \
  -d '{"petId":1,"scheduledAt":"2027-03-01T10:00:00","veterinarian":"Dra. Beatriz Nunes","reason":"Check-up geral"}'
```

```json
{"id":4,"petId":1,"petName":"Rex","ownerId":1,"ownerName":"Alice Souza", ...}
```

**Resiliência: o ganho principal da refatoração.**

```bash
kubectl -n petclinic scale deployment/backend --replicas=0

# Continua funcionando: os dados do pet vêm da projeção local.
# Antes dos eventos, isto devolvia 503.
curl -i -X POST http://localhost:8080/api/appointments -H 'Content-Type: application/json' \
  -d '{"petId":3,"scheduledAt":"2027-04-01T09:00:00","veterinarian":"Dr. Marcos Vieira","reason":"Vacina"}'

# Só falha para um pet que a projeção ainda não conhece — aí o fallback
# síncrono precisa do monólito e devolve 503 (não 404)
curl -i -X POST http://localhost:8080/api/appointments -H 'Content-Type: application/json' \
  -d '{"petId":9999,"scheduledAt":"2027-04-02T09:00:00","veterinarian":"Dra. X","reason":"y"}'

kubectl -n petclinic scale deployment/backend --replicas=1
# o fallback volta a distinguir 404 de 503 assim que o pod fica Ready: o
# endpoint do Service é atualizado pelo próprio Kubernetes, sem cache de cliente
```

**Cascata entre serviços, seguida por um único identificador:**

```bash
CID=$(uuidgen)
curl -s -X DELETE http://localhost:8080/api/pets/1 -H "X-Correlation-Id: $CID"
sleep 5

# As consultas do pet foram canceladas por reação ao evento, não por chamada direta
curl -s "http://localhost:8080/api/appointments?petId=1" | jq -r '.[] | "\(.id) \(.status)"'

# O mesmo correlationId aparece no log dos dois serviços. O `sort` importa:
# cada `kubectl logs` traz um pod por vez, e a ordem causal só aparece
# reordenando pelo timestamp ISO que abre cada linha.
for d in backend appointment-service; do
  kubectl -n petclinic logs deploy/$d --tail=500 | grep "$CID" | sed "s/^/[$d] /"
done | sort -t' ' -k2
# ... petclinic-backend    ... Evento pet.deleted publicado
# ... appointment-service ... Recebido pet.deleted
# ... appointment-service ... Consulta 4 cancelada em cascata (PET_DELETED)
# ... appointment-service ... Evento appointment.cancelled publicado
```

O monólito não conhece consultas: o `DELETE` em `/api/pets/1` publicou um `pet.deleted`
num exchange, e o `appointment-service` reagiu cancelando as consultas agendadas e
anunciando `appointment.cancelled` — com o campo `trigger` dizendo *por que* foi
cancelada, para quem venha a consumir.

O roteiro completo de demonstração, incluindo queda do broker e descarte de mensagem
venenosa, está em
[docs/event-driven-architecture.md](docs/event-driven-architecture.md#10-roteiro-de-demonstração).
Para verificar o sistema à mão, o catálogo de **quais ações geram cada evento e o que cada
evento altera** — mais as quatro armadilhas que fazem um teste correto parecer falho — está
em [§6.3](docs/event-driven-architecture.md#63-catálogo-completo-ação-evento-e-consequência).

---

## Testes

Os testes automatizados são de backend — **74 no total**. O frontend não tem suíte
(ver `docs/tp5.md` §5.3); o CI o cobre com lint e build.

Cada módulo tem sua própria suíte, executável isoladamente:

```bash
cd backend              && ./mvnw test   # 27 testes
cd appointment-service  && ./mvnw test   # 46 testes
cd api-gateway          && ./mvnw test   #  1 teste
```

## Manifests e CI/CD

Um único conjunto de manifests (Kustomize), em escala de desenvolvimento, com probes de
liveness/readiness/startup e `securityContext` sem root. O pipeline
do GitHub Actions valida testes, lint, manifests e imagens a cada push, e publica no
GHCR a cada merge em `main`.

Os comandos para subir estão em [Kubernetes (recomendado)](#kubernetes-recomendado), acima.
Tudo detalhado em [k8s/README.md](k8s/README.md).

---

## Perfis de banco

O datasource é escolhido por profile do Spring, sem alterar o código de domínio:

- **Perfil padrão** — H2 in-memory (`ddl-auto=create-drop`), usado em dev/testes.
- **Perfil `postgres`** — PostgreSQL persistente (`ddl-auto=update`), ativado com
  `SPRING_PROFILES_ACTIVE=postgres`. É o que os manifests do Kubernetes usam.

Cada serviço aponta para a **sua própria database** (`petclinicdb` e `appointmentsdb`):
nenhum serviço enxerga as tabelas do outro. Para rodar um backend na máquina contra o
Postgres que está no cluster, exponha-o e ative o perfil:

```bash
kubectl -n petclinic port-forward svc/postgres 5432:5432 &
cd appointment-service
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

Os placeholders do datasource têm default para `localhost` — veja
[application-postgres.properties](appointment-service/src/main/resources/application-postgres.properties).
