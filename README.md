# Pet Clinic

Aplicação de cadastro de tutores (owners), pets e **agendamento de consultas**,
construída como uma arquitetura de microsserviços com Spring Boot e Spring Cloud,
com frontend React.

Modelada com DDD — veja o mapa de contexto em
[docs/ddd-context-map.md](docs/ddd-context-map.md) e a arquitetura distribuída em
[docs/microservice-architecture.md](docs/microservice-architecture.md).

## Arquitetura

| Módulo | Porta | Papel |
| --- | --- | --- |
| [discovery-server](discovery-server/) | 8761 | Registro Eureka (service discovery) |
| [api-gateway](api-gateway/) | 8080 | Ponto único de entrada da API |
| [backend](backend/) | 8081 | Monólito: tutores e pets (contexto *Patient Registry*) |
| [appointment-service](appointment-service/) | 8082 | Microsserviço de consultas (contexto *Scheduling*) |
| [frontend](frontend/) | 3000 | SPA React (nginx) |
| PostgreSQL | 5432 | Databases `petclinicdb` e `appointmentsdb` |

O frontend fala **apenas com o gateway**, que roteia `/api/owners` e `/api/pets` para
o monólito e `/api/appointments` para o microsserviço. O `appointment-service`, por sua
vez, valida os pets chamando o monólito via OpenFeign — os endereços são resolvidos
pelo Eureka, nunca fixados em configuração.

## Stack

- **Backend** — Java 21, Spring Boot 4.0.7, Spring Data JPA, Hibernate Envers
  (auditoria/histórico), Lombok, Bean Validation
- **Spring Cloud 2025.1.2** — Netflix Eureka (discovery), Spring Cloud Gateway
  (roteamento), OpenFeign + LoadBalancer (comunicação entre serviços)
- **Banco** — H2 in-memory (padrão) ou PostgreSQL (profile `postgres`)
- **Frontend** — React 19, React Router 7, Vite
- **Infra** — Docker + Docker Compose

## Pré-requisitos

Para rodar localmente (sem Docker):

- JDK 21
- Node.js 20+ e npm

Para rodar via containers:

- Docker e Docker Compose

Todos os módulos usam o Maven Wrapper (`./mvnw`), então não é necessário instalar o
Maven na máquina.

---

## Docker (recomendado)

É a forma mais simples de subir os seis serviços de uma vez.

### Configuração

Copie o arquivo de exemplo e ajuste as credenciais se desejar (os defaults já
funcionam):

```bash
cp .env.example .env
```

| Variável            | Default     |
| ------------------- | ----------- |
| `POSTGRES_USER`     | `petclinic` |
| `POSTGRES_PASSWORD` | `petclinic` |

### Subir

```bash
docker compose up --build
```

> ⚠️ **Se você já rodou uma versão anterior deste projeto**, execute
> `docker compose down -v` antes. A database `appointmentsdb` é criada por um script
> em `/docker-entrypoint-initdb.d`, que o PostgreSQL só executa quando o volume de
> dados está vazio — com um `pgdata` preexistente, o `appointment-service` não sobe.

Depois que a stack estabilizar:

- **SPA**: http://localhost:3000
- **API (gateway)**: http://localhost:8080
- **Dashboard Eureka**: http://localhost:8761 — deve listar `API-GATEWAY`,
  `PETCLINIC-BACKEND` e `APPOINTMENT-SERVICE`
- **PostgreSQL**: `localhost:5432` (databases `petclinicdb` e `appointmentsdb`)

O `discovery-server` sobe primeiro (os demais têm `depends_on` no seu healthcheck).
Os serviços levam alguns segundos para aparecer no registro do Eureka; até lá o gateway
responde `503`.

### Parar

```bash
docker compose down          # para os containers
docker compose down -v       # para e apaga o volume pgdata (reseta os dados)
```

---

## Desenvolvimento local (sem Docker)

Cada backend é um projeto Maven independente. Suba nesta ordem, em terminais
separados — o Eureka precisa estar de pé para que os demais se registrem:

```bash
cd discovery-server    && ./mvnw spring-boot:run   # :8761
cd backend             && ./mvnw spring-boot:run   # :8081
cd appointment-service && ./mvnw spring-boot:run   # :8082
cd api-gateway         && ./mvnw spring-boot:run   # :8080
```

Por padrão cada serviço usa **H2 in-memory**, sem nenhuma configuração externa. O
schema é recriado a cada start (`ddl-auto=create-drop`) e populado pelos respectivos
`data.sql`.

Consoles H2 (apenas no monólito): http://localhost:8081/h2-console
(JDBC URL `jdbc:h2:mem:petclinicdb`, usuário `sa`, senha em branco).

Para rodar um serviço **isolado**, sem o Eureka:

```bash
EUREKA_ENABLED=false ./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

A SPA sobe em http://localhost:5173. O Vite faz proxy de `/api` para
`http://localhost:8080` (ver [frontend/vite.config.js](frontend/vite.config.js)) —
ou seja, para o **gateway**, que precisa estar rodando.

Outros scripts:

```bash
npm run build     # build de produção em dist/
npm run preview   # serve o build gerado
npm run lint      # ESLint
```

---

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

O agendamento envia **apenas o `petId`** — o nome do pet e os dados do tutor são
obtidos pelo microsserviço junto ao monólito:

```bash
curl -X POST http://localhost:8080/api/appointments \
  -H 'Content-Type: application/json' \
  -d '{"petId":1,"scheduledAt":"2026-12-01T10:00:00","veterinarian":"Dra. Beatriz Nunes","reason":"Check-up geral"}'
```

```json
{"id":4,"petId":1,"petName":"Rex","ownerId":1,"ownerName":"Alice Souza", ...}
```

Cenários de erro que valem demonstrar:

```bash
# Pet inexistente -> 404, com a mensagem propagada do monólito
curl -i -X POST http://localhost:8080/api/appointments -H 'Content-Type: application/json' \
  -d '{"petId":9999,"scheduledAt":"2026-12-02T10:00:00","veterinarian":"Dra. X","reason":"y"}'

# Monólito fora do ar -> 503 (não 404!), e a listagem continua funcionando
docker compose stop backend
curl -i -X POST http://localhost:8080/api/appointments -H 'Content-Type: application/json' \
  -d '{"petId":1,"scheduledAt":"2026-12-05T10:00:00","veterinarian":"Dra. X","reason":"y"}'
curl -i http://localhost:8080/api/appointments   # 200: os nomes vêm do snapshot local
docker compose start backend
```

---

## Testes

Cada módulo tem sua própria suíte, executável isoladamente. Nenhuma depende de rede:
o cliente Feign é substituído por mock e o Eureka é desligado nos testes.

```bash
cd backend             && ./mvnw test   # 20 testes
cd appointment-service && ./mvnw test   # 27 testes
cd api-gateway         && ./mvnw test   #  1 teste
cd discovery-server    && ./mvnw test   #  1 teste
```

Cobertura resumida em
[docs/microservice-architecture.md](docs/microservice-architecture.md#8-cobertura-de-testes).

---

## Perfis de banco

O datasource é escolhido por profile do Spring, sem alterar o código de domínio:

- **Perfil padrão** — H2 in-memory (`ddl-auto=create-drop`), usado em dev/testes.
- **Perfil `postgres`** — PostgreSQL persistente (`ddl-auto=update`), ativado com
  `SPRING_PROFILES_ACTIVE=postgres`. É o que o Docker Compose usa.

Cada serviço aponta para a **sua própria database** (`petclinicdb` e `appointmentsdb`):
nenhum serviço enxerga as tabelas do outro. Para rodar um backend fora do Docker contra
o Postgres do compose:

```bash
cd appointment-service
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

Os placeholders do datasource têm default para `localhost` — veja
[application-postgres.properties](appointment-service/src/main/resources/application-postgres.properties).
