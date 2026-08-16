# Arquitetura do Microsserviço de Agendamento

Documento de referência do `appointment-service` e da infraestrutura distribuída
(Spring Cloud) introduzida para integrá-lo ao sistema existente.

---

## 1. Visão geral

O sistema deixou de ser um monólito com frontend acoplado e passou a ter **quatro
processos backend** coordenados por service discovery, com um ponto único de entrada:

```mermaid
flowchart TB
    Browser(["Navegador"])

    subgraph Edge["Borda"]
        FE["frontend<br/>React SPA + nginx<br/>:3000"]
        GW["api-gateway<br/>Spring Cloud Gateway<br/>:8080"]
    end

    subgraph Services["Serviços de aplicação"]
        BE["petclinic-backend<br/>Patient Registry<br/>:8081"]
        AS["appointment-service<br/>Scheduling<br/>:8082"]
    end

    EU["discovery-server<br/>Eureka<br/>:8761"]

    DB1[("petclinicdb")]
    DB2[("appointmentsdb")]

    Browser --> FE
    FE -->|"proxy /api/"| GW
    GW -->|"lb://petclinic-backend<br/>/api/owners, /api/pets"| BE
    GW -->|"lb://appointment-service<br/>/api/appointments"| AS
    AS -->|"OpenFeign<br/>GET /api/pets/{id}"| BE

    BE --- DB1
    AS --- DB2

    BE -.->|registra| EU
    AS -.->|registra| EU
    GW -.->|consulta| EU
```

| Serviço | Porta | Papel |
| --- | --- | --- |
| `discovery-server` | 8761 | Registro Eureka; resolve nomes lógicos em endereços |
| `api-gateway` | 8080 | Ponto único de entrada; roteia por caminho |
| `backend` (`petclinic-backend`) | 8081 | Monólito existente: tutores e pets |
| `appointment-service` | 8082 | **Novo microsserviço**: consultas |
| `db` | 5432 | PostgreSQL com duas databases independentes |
| `frontend` | 3000 | SPA React servida por nginx |

---