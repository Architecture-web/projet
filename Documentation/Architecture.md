# Architecture du Projet – SkillQuest

## 1. Vue d'Ensemble du Projet

**SkillQuest** est une plateforme web distribuée orientée microservices combinant :
- **L'apprentissage interactif** (cours modulaires et exercices pratiques).
- **L'évaluation de code en temps réel** (exécution sécurisée et isolée en sandbox).
- **Le recrutement technique certifié** (chasse de profils basée sur des compétences vérifiées).

Le système repose sur une architecture **microservices indépendants**, avec découplage fort selon le principe **Database-per-Service** et une combinaison de communications synchrones (REST) et asynchrones (Event-Driven).

---

## 2. Diagramme d'Architecture Globale

```mermaid
flowchart TD
    subgraph ClientLayer["Couche Client"]
        Client["Application Frontend (SPA React / Vue)"]
    end

    subgraph GatewayLayer["Point d'Entrée Unique"]
        Gateway["API Gateway (Port 8080)<br/>• Routage & Reverse-Proxy<br/>• Validation des Tokens JWT<br/>• Rate Limiting & CORS"]
    end

    subgraph CommonService["Service Commun (Équipe)"]
        AuthSvc["Auth & Identity Service<br/>• Inscription / Connexion<br/>• Rôles (RBAC) & Sessions JWT"]
        AuthDB[("PostgreSQL<br/>(Auth DB)")]
        AuthSvc --- AuthDB
    end

    subgraph MicroservicesLayer["Microservices Métier (6 Étudiants)"]
        CourseSvc["1. Course Content Service<br/>(Modules, Leçons, Progression)"]
        CourseDB[("MongoDB<br/>(Courses DB)")]
        CourseSvc --- CourseDB

        CodeSvc["2. Code Execution Service<br/>(Sandbox isolée, Tests)"]
        CodeRedis[("Redis<br/>(Queue de jobs)")]
        CodeDB[("PostgreSQL<br/>(Soumissions)")]
        CodeSvc --- CodeRedis
        CodeSvc --- CodeDB

        GamifSvc["3. Gamification Service<br/>(XP, Badges, Certificats)"]
        GamifDB[("PostgreSQL<br/>(Gamif DB)")]
        GamifSvc --- GamifDB

        JobSvc["4. Job Board Service<br/>(Offres Tech, Matching)"]
        JobDB[("MongoDB<br/>(Jobs DB)")]
        JobSvc --- JobDB

        CommSvc["5. Community Service<br/>(Forums, Entraide, Votes)"]
        CommDB[("MongoDB<br/>(Forum DB)")]
        CommSvc --- CommDB

        NotifSvc["6. Notification Service<br/>(WebSockets & Alertes)"]
        NotifRedis[("Redis<br/>(Sessions WS)")]
        NotifSvc --- NotifRedis
    end

    subgraph BrokerLayer["Bus d'Événements Asynchrones"]
        Broker{{"Message Broker (RabbitMQ)<br/>Exchange: skillquest.events"}}
    end

    %% Flux Client -> Gateway
    Client -->|HTTPS / WSS| Gateway

    %% Flux Gateway -> Microservices
    Gateway -->|HTTP REST| AuthSvc
    Gateway -->|HTTP REST| CourseSvc
    Gateway -->|HTTP REST| CodeSvc
    Gateway -->|HTTP REST| GamifSvc
    Gateway -->|HTTP REST| JobSvc
    Gateway -->|HTTP REST| CommSvc
    Gateway -->|WebSocket Upgrade| NotifSvc

    %% Interaction Synchrone inter-services
    JobSvc -.->|"Appel Synchrone REST<br/>GET /certifications/:userId"| GamifSvc

    %% Flux Asynchrones Event-Driven
    CodeSvc ==>|Publication: CodeSolutionPassedEvent| Broker
    Broker ==>|Consommation| GamifSvc
    Broker ==>|Consommation| CourseSvc
    Broker ==>|Consommation| NotifSvc
```

---

## 3. Description des Composants

### 3.1. API Gateway
- **Rôle :** Point d'entrée unique de l'ensemble du trafic externe.
- **Responsabilités :**
  - Routage dynamique des requêtes vers le bon microservice.
  - Vérification et validation de la signature des tokens JWT.
  - Injection des headers d'identité (`X-User-Id`, `X-User-Role`) vers les services internes.
  - Sécurité périmétrique (CORS, Rate Limiting, pare-feu applicatif).

### 3.2. Service Commun : Auth & Identity Service
- **Rôle :** Gestion centrale des utilisateurs et de l'authentification.
- **Base de données :** **PostgreSQL** (garantie ACID, unicité des e-mails, relations strictes).
- **Responsabilités :**
  - Inscription, connexion, révocation et rafraîchissement des tokens.
  - Contrôle d'accès basé sur les rôles (RBAC) : `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_RECRUITER`, `ROLE_ADMIN`.

### 3.3. Microservices Métier (1 par membre de l'équipe)

| # | Microservice | Responsabilité | Base de Données | Justification du choix de la BD |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **Course Content** | Gestion du catalogue de formations, chapitres, leçons et suivi d'avancement. | **MongoDB** | Données documentaires semi-structurées et imbriquées (cours $\rightarrow$ chapitres $\rightarrow$ leçons). |
| **2** | **Code Execution** | Exécution de code dans une sandbox sécurisée (Docker éphémère sans réseau), évaluation des tests unitaires. | **Redis + PostgreSQL** | Redis pour la file d'attente (FIFO) des soumissions rapides, PostgreSQL pour l'historique pérenne des résultats. |
| **3** | **Gamification** | Calcul des points d'XP, gestion des paliers de niveaux, attribution de badges et émission de certificats PDF vérifiables. | **PostgreSQL** | Intégrité transactionnelle stricte pour éviter la duplication d'XP ou la fraude aux classements. |
| **4** | **Job Board** | Publication d'offres d'emploi tech, filtrage des candidats par compétences vérifiées et gestion des candidatures. | **MongoDB** | Recherche flexible par mots-clés, tags de compétences et critères hétérogènes. |
| **5** | **Community** | Forums d'entraide dédiés aux exercices, réponses imbriquées, votes d'utilité et partage de solutions de code. | **MongoDB** | Gestion naturelle des arborescences de commentaires et threads de discussion. |
| **6** | **Notification** | Diffusion d'alertes instantanées par WebSockets et envoi d'e-mails transactionnels (convocations, badges obtenus). | **Redis + MongoDB** | Redis pour la gestion en mémoire des sessions WebSockets actives, MongoDB pour l'historique des notifications. |

---

## 4. Modes de Communication

> **En résumé :**
> - **Synchrone (HTTP REST) :** L'émetteur attend la réponse immédiatement (idéal pour la consultation de données fraîches).
> - **Asynchrone (RabbitMQ) :** L'émetteur publie un message et continue sans attendre ; les autres services le traitent en arrière-plan (idéal pour la performance et la tolérance aux pannes).

### 4.1. Communications Synchrones (HTTP REST)
Ce mode est réservé aux vérifications directes qui conditionnent immédiatement la suite d'un traitement :

1. **Point d'entrée Client :**
   - L'utilisateur envoie sa requête à l'**API Gateway** (port `8080`).
   - L'API Gateway vérifie le token JWT et route la requête vers le microservice concerné.

2. **Exemples inter-services (Service à Service) :**
   - **Vérification d'un candidat :** Lors d'une candidature, le **Job Board Service** appelle en direct `GET /api/certifications/:userId` auprès du **Gamification Service** pour valider instantanément les badges du candidat avant d'enregistrer le dossier.
   - **Validation d'un enseignant :** Lors de la création d'un cours, le **Course Service** appelle en direct `GET /api/auth/users/:id/summary` sur l'**Auth Service** pour vérifier que le créateur est bien un enseignant actif.
   - **Chargement des tests :** Avant d'exécuter un code, le **Code Execution Service** appelle en direct `GET /api/courses/.../test-cases` sur le **Course Service** pour récupérer les assertions à tester.

### 4.2. Communications Asynchrones (Événements via RabbitMQ)
Ce mode permet d'avertir plusieurs services en même temps sans ralentir l'utilisateur et sans bloquer l'application si un service est momentanément indisponible :

1. **Principe :**
   - Le service émetteur envoie un événement sur le **Topic Exchange** `skillquest.events` avec une clé de routage précise.
   - Chaque service intéressé possède sa propre **file d'attente durable (queue)** et traite le message à son rythme.

2. **Exemple concret – Flux après validation d'un exercice :**
   - **Déclencheur :** L'élève soumet son code et tous les tests unitaires réussissent.
   - **Émission :** Le **Code Execution Service** publie l'événement `CodeSolutionPassedEvent` (`code.solution.passed`).
   - **Traitements en parallèle par les consommateurs :**
     - **Gamification Service :** Consomme l'événement, crédite **+50 XP** et débloque le badge correspondant si le palier est atteint.
     - **Course Content Service :** Consomme l'événement et marque la leçon comme **Validée** (100% de complétion).
     - **Community Service :** Consomme l'événement et **débloque l'accès aux solutions** des autres étudiants (Peer-Review).
     - **Notification Service :** Consomme l'événement et pousse instantanément une **alerte visuelle (WebSocket)** sur l'écran de l'élève.

3. **Autre exemple clé – Inscription d'un nouvel utilisateur :**
   - L'**Auth Service** publie `UserRegisteredEvent` après avoir créé le compte.
   - Le **Notification Service** le consomme pour envoyer l'e-mail de confirmation en arrière-plan, ce qui permet à l'utilisateur de recevoir sa réponse HTTP d'inscription en quelques millisecondes.

---

## 5. Sécurité & Authentification

- **Format des tokens :** JSON Web Tokens (JWT) signés avec algorithme asymétrique ou HMAC-SHA256.
- **Cycle d'authentification :**
  1. L'utilisateur envoie ses identifiants à l'API Gateway $\rightarrow$ relayé à l'Auth Service.
  2. L'Auth Service génère un `access_token` (durée de vie courte : 15 min) et un `refresh_token` (durée de vie longue : 7 jours).
  3. Pour chaque requête subséquente, l'API Gateway intercepte le token dans le header `Authorization: Bearer <token>`, valide son authenticité, et enrichit la requête transmise aux microservices avec les en-têtes internes sécurisés `X-User-Id` et `X-User-Role`.
