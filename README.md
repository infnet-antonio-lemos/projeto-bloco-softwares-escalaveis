# Pet Clinic

Aplicação de cadastro de tutores (owners) e pets, construída como um monólito
Spring Boot com frontend React. Modelada com DDD — veja o mapa de contexto e os
diagramas em [docs/ddd-context-map.md](docs/ddd-context-map.md).

## Stack

- **Backend** — Java 21, Spring Boot 4, Spring Data JPA, Hibernate Envers
  (auditoria/histórico), Lombok, Bean Validation
- **Banco** — H2 in-memory (padrão) ou PostgreSQL (profile `postgres`)
- **Frontend** — React 19, React Router 7, Vite
- **Infra** — Docker + Docker Compose

## Pré-requisitos

Para rodar localmente (sem Docker):

- JDK 21
- Node.js 20+ e npm

Para rodar via containers:

- Docker e Docker Compose

O backend usa o Maven Wrapper (`./mvnw`), então não é necessário instalar o
Maven na máquina.

---

## Desenvolvimento

Rode o backend e o frontend em terminais separados. Por padrão o backend sobe
com o banco **H2 in-memory**, que não exige nenhuma configuração externa.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

A API fica disponível em `http://localhost:8080`.

- Base da API: `/api/owners` e `/api/pets` (CRUD + `/{id}/history` para o
  histórico de auditoria)
- Console H2: `http://localhost:8080/h2-console`
  (JDBC URL `jdbc:h2:mem:petclinicdb`, usuário `sa`, senha em branco)

O schema é recriado a cada start (`ddl-auto=create-drop`) e populado a partir de
[backend/src/main/resources/data.sql](backend/src/main/resources/data.sql).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

A SPA sobe em `http://localhost:5173`. O Vite faz proxy de `/api` para
`http://localhost:8080` (ver [frontend/vite.config.js](frontend/vite.config.js)),
então basta ter o backend rodando em paralelo.

Outros scripts do frontend:

```bash
npm run build     # build de produção em dist/
npm run preview   # serve o build gerado
npm run lint      # ESLint
```

---

## Testes

Os testes do backend usam Spring Boot Test (JPA, MVC e validação), incluindo
testes de repositório e do histórico de auditoria (Envers):

```bash
cd backend
./mvnw test
```

---

## Docker

O [docker-compose.yml](docker-compose.yml) sobe dois serviços: `db`
(PostgreSQL 16) e `backend` (Spring Boot com o profile `postgres`). Os dados do
Postgres são persistidos no volume `pgdata`, e o backend só inicia após o
`healthcheck` do banco passar.

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

- API: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (banco `petclinicdb`)

Diferente do perfil H2, o schema é preservado entre reinícios
(`ddl-auto=update`, `PostgreSQLDialect`).

### Parar

```bash
docker compose down          # para os containers
docker compose down -v       # para e apaga o volume pgdata (reseta os dados)
```

### Apontar o backend local para o Postgres

Também é possível rodar o backend fora do Docker usando o profile `postgres`
(por exemplo, com o `db` do compose já no ar). O datasource usa placeholders com
default para `localhost` — veja
[application-postgres.properties](backend/src/main/resources/application-postgres.properties):

```bash
cd backend
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```
