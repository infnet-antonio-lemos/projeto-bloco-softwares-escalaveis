# Context Map

O sistema tem **dois contextos delimitados**, implantados como serviços separados e
com bancos de dados independentes. A relação é **Customer/Supplier**: `Scheduling`
consome dados de `Patient Registry`, que não conhece o consumidor.

```mermaid
flowchart TD
  subgraph PR["Patient Registry — petclinic-backend"]
    direction TB
    PR_AR["Owner\n«Aggregate Root»"]
    PR_E["Pet\n«Entity»"]
    PR_VO["Species\nenum"]

    PR_AR -- "1 para N" --> PR_E
    PR_E -- usa --> PR_VO
  end

  subgraph SC["Scheduling — appointment-service"]
    direction TB
    SC_AR["Appointment\n«Aggregate Root»"]
    SC_VO["AppointmentStatus\nenum"]

    SC_AR -- usa --> SC_VO
  end

  SC_AR -. "referencia por id (petId, ownerId)\nvia HTTP/OpenFeign — sem FK" .-> PR_E
```

O limite entre os contextos é uma chamada HTTP, não um JOIN: `Appointment` guarda
apenas identificadores de `Pet`/`Owner` e um snapshot dos nomes. Detalhes da
integração em [microservice-architecture.md](microservice-architecture.md).

---

# Modelo de Agregados

```mermaid
classDiagram
    class Owner {
        <<Aggregate Root>>
        +Long id
        +String name
        +String email
        +String phone
        +String address
        +List~Pet~ pets
    }

    class Pet {
        <<Entity>>
        +Long id
        +String name
        +Species species
        +String breed
        +LocalDate birthDate
        +Long owner_id FK
    }

    class Species {
        <<Enumeration>>
        DOG
        CAT
        BIRD
        RABBIT
        OTHER
    }

    class OwnerRequest {
        <<DTO - Input>>
        +String name
        +String email
        +String phone
        +String address
    }

    class OwnerResponse {
        <<DTO - Output>>
        +Long id
        +String name
        +String email
        +String phone
        +String address
        +int petCount
    }

    class PetRequest {
        <<DTO - Input>>
        +String name
        +Species species
        +String breed
        +LocalDate birthDate
        +Long ownerId
    }

    class PetResponse {
        <<DTO - Output>>
        +Long id
        +String name
        +Species species
        +String breed
        +LocalDate birthDate
        +Long ownerId
        +String ownerName
    }

    class Appointment {
        <<Aggregate Root>>
        +Long id
        +Long petId
        +Long ownerId
        +String petName
        +String ownerName
        +LocalDateTime scheduledAt
        +String veterinarian
        +String reason
        +String notes
        +AppointmentStatus status
    }

    class AppointmentStatus {
        <<Enumeration>>
        SCHEDULED
        COMPLETED
        CANCELLED
        NO_SHOW
    }

    class AppointmentRequest {
        <<DTO - Input>>
        +Long petId
        +LocalDateTime scheduledAt
        +String veterinarian
        +String reason
        +String notes
    }

    class AppointmentResponse {
        <<DTO - Output>>
        +Long id
        +Long petId
        +String petName
        +Long ownerId
        +String ownerName
        +LocalDateTime scheduledAt
        +String veterinarian
        +String reason
        +String notes
        +AppointmentStatus status
    }

    Owner "1" *-- "N" Pet : contém
    Pet --> Species : usa

    OwnerRequest ..> Owner : cria/atualiza
    Owner ..> OwnerResponse : projeta

    PetRequest ..> Pet : cria/atualiza
    Pet ..> PetResponse : projeta

    Appointment --> AppointmentStatus : usa
    AppointmentRequest ..> Appointment : cria/atualiza
    Appointment ..> AppointmentResponse : projeta
    Appointment ..> Pet : referencia por id (HTTP)
```

Note que `AppointmentRequest` **não** tem `ownerId`: o tutor e os nomes são
resolvidos pelo microsserviço consultando o `petclinic-backend`.

---

# Camadas da Arquitetura

Ambos os serviços seguem o mesmo empilhamento de camadas. O que muda no
`appointment-service` é a origem dos dados externos: um cliente HTTP no lugar
de um repositório.

```mermaid
flowchart LR
    HTTP(["Cliente HTTP"]) --> GW["api-gateway\n«ponto único de entrada»"]

    subgraph BC1["Bounded Context: Patient Registry"]
        direction TB
        C["OwnerController\nPetController\n«REST Layer»"]
        S["OwnerService\nPetService\n«Application Layer»"]
        R["OwnerRepository\nPetRepository\n«Repository»"]
        E["Owner / Pet\n«Domain Layer»"]

        C -->|chama| S
        S -->|usa| R
        R -->|persiste| E
        S -->|lê/escreve| E
    end

    subgraph BC2["Bounded Context: Scheduling"]
        direction TB
        C2["AppointmentController\n«REST Layer»"]
        S2["AppointmentService\n«Application Layer»"]
        R2["AppointmentRepository\n«Repository»"]
        E2["Appointment\n«Domain Layer»"]
        F2["PetClient\n«Anti-Corruption Layer»"]

        C2 -->|chama| S2
        S2 -->|usa| R2
        S2 -->|consulta| F2
        R2 -->|persiste| E2
        S2 -->|lê/escreve| E2
    end

    GW -->|"/api/owners, /api/pets"| C
    GW -->|"/api/appointments"| C2
    F2 -->|"OpenFeign\nGET /api/pets/{id}"| C

    E -->|JPA / Hibernate| DB[("petclinicdb")]
    E2 -->|JPA / Hibernate| DB2[("appointmentsdb")]
```

`PetClient` faz o papel de camada anticorrupção: traduz o `PetResponse` do outro
contexto para o `PetSummary` que o Scheduling entende, e converte as falhas remotas
em exceções do próprio domínio (404 para pet inexistente, 503 para serviço fora do ar).

O acesso ao banco é feito via JPA/Hibernate e o datasource concreto é
escolhido por profile do Spring, sem alterar o código de domínio:

- **Perfil padrão** — H2 in-memory (`ddl-auto=create-drop`), usado em dev/testes.
- **Perfil `postgres`** — PostgreSQL persistente (`ddl-auto=update`,
  `PostgreSQLDialect`), ativado com `SPRING_PROFILES_ACTIVE=postgres`.

---