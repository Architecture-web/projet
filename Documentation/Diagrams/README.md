# Diagrammes d'Architecture du Projet SkillQuest

## 1. Diagramme d'Architecture Globale (Microservices & Communications)

```mermaid
flowchart TD
    subgraph Clients["Clients"]
        Browser["Navigateur Web (SPA React)"]
    end

    subgraph GatewayLayer["Couche Entrée & Sécurité"]
        Gateway["API Gateway (Port 8080)<br/>- Routage & Reverse-Proxy<br/>- Validation JWT<br/>- Rate Limiting"]
    end

    subgraph CommonService["Service Partagé (Équipe)"]
        AuthSvc["Auth & Identity Service<br/>(JWT, Profils, Rôles RBAC)"]
        AuthDB[("PostgreSQL<br/>(Auth DB)")]
        AuthSvc --- AuthDB
    end

    subgraph CoreServices["Microservices Métier"]
        direction TB
        CourseSvc["1. Course Content Service<br/>(Catalogue, Modules, Progrès)"]
        CourseDB[("MongoDB<br/>(Courses DB)")]
        CourseSvc --- CourseDB

        CodeSvc["2. Code Execution Service<br/>(Sandbox Docker, Tests)"]
        CodeRedis[("Redis<br/>(Queue)")]
        CodeDB[("PostgreSQL<br/>(Submissions)")]
        CodeSvc --- CodeRedis
        CodeSvc --- CodeDB

        GamifSvc["3. Gamification Service<br/>(XP, Badges, Certificats)"]
        GamifDB[("PostgreSQL<br/>(Gamif DB)")]
        GamifSvc --- GamifDB

        JobSvc["4. Job Board Service<br/>(Offres Tech, Matching)"]
        JobDB[("MongoDB<br/>(Jobs DB)")]
        JobSvc --- JobDB

        CommSvc["5. Community Service<br/>(Forums, Votes, Reviews)"]
        CommDB[("MongoDB<br/>(Forum DB)")]
        CommSvc --- CommDB

        NotifSvc["6. Notification Service<br/>(WebSockets, Alertes)"]
        NotifRedis[("Redis<br/>(Sessions WS)")]
        NotifSvc --- NotifRedis
    end

    subgraph BrokerLayer["Bus d'Événements Asynchrones"]
        RabbitMQ{{"RabbitMQ Message Broker<br/>Topic Exchange: 'skillquest.events'"}}
    end

    %% Flux Clients vers Gateway
    Browser -->|HTTPS / WSS| Gateway

    %% Gateway vers Services
    Gateway -->|HTTP REST| AuthSvc
    Gateway -->|HTTP REST| CourseSvc
    Gateway -->|HTTP REST| CodeSvc
    Gateway -->|HTTP REST| GamifSvc
    Gateway -->|HTTP REST| JobSvc
    Gateway -->|HTTP REST| CommSvc
    Gateway -->|WebSocket Upgrade| NotifSvc

    %% Interaction synchrone
    JobSvc -.->|1. Requête GET REST : Profil Candidat| GamifSvc

    %% Flux asynchrones (Event-Driven)
    CodeSvc ==>|2. Émet 'CodePassedEvent'| RabbitMQ
    RabbitMQ ==>|Consomme| GamifSvc
    RabbitMQ ==>|Consomme| CourseSvc
    RabbitMQ ==>|Consomme| NotifSvc

    classDef db fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    classDef svc fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px;
    classDef gw fill:#fff3e0,stroke:#f57c00,stroke-width:2px;
    classDef broker fill:#e8f5e9,stroke:#388e3c,stroke-width:2px;

    class AuthDB,CourseDB,CodeDB,GamifDB,JobDB,CommDB,CodeRedis,NotifRedis db;
    class AuthSvc,CourseSvc,CodeSvc,GamifSvc,JobSvc,CommSvc,NotifSvc svc;
    class Gateway gw;
    class RabbitMQ broker;
```

---

## 2. Diagramme de Séquence : Traitement d'une Soumission de Code (Synchrone & Asynchrone)

```mermaid
sequenceDiagram
    autonumber
    actor Etudiant as Apprenant
    participant GW as API Gateway
    participant Code as Code Execution Service
    participant Rabbit as RabbitMQ
    participant Gamif as Gamification Service
    participant Notif as Notification Service

    Etudiant->>GW: POST /api/submissions/submit (Code Java)
    GW->>Code: Relaye la requête HTTP (avec X-User-Id)
    Code-->>GW: 202 Accepted (Job en cours d'évaluation)
    GW-->>Etudiant: 202 Accepted { submissionId: "sub_102" }

    Note over Code: Exécution isolée dans sandbox Docker<br/>Comparaison avec les tests cachés...

    alt Tests Validés avec succès
        Code->>Rabbit: Publie 'CodeSolutionPassedEvent'
        par Notification de Gamification
            Rabbit->>Gamif: Consomme l'événement
            Gamif->>Gamif: +50 XP & Déblocage Badge "Java Master"
        and Notification Temps Réel
            Rabbit->>Notif: Consomme l'événement
            Notif->>Etudiant: WebSocket: "Badge Java Master débloqué ! (+50 XP)"
        end
    else Échec des tests
        Code->>Rabbit: Publie 'CodeFailedEvent'
        Rabbit->>Notif: Consomme l'événement
        Notif->>Etudiant: WebSocket: "3/5 tests ont échoué."
    end
```
