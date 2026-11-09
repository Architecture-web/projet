# Auth & Identity Service (Service Commun – Étudiant 1)

## 📌 1. Présentation du Service
Le service d'authentification et d'identité centralise la gestion des comptes, le contrôle d'accès basé sur les rôles (RBAC) et l'émission sécurisée des jetons JWT.

- **Port local :** `8081`
- **Responsable :** Étudiant 1 (Développé en tant que Service Commun de l'équipe)
- **Base de données :** **PostgreSQL 15** (`skillquest_auth`)
- **Justification de la base :** Intégrité relationnelle stricte (ACID), unicité des adresses e-mails, relations entre comptes, rôles et tables d'audit.

---

## 📐 2. Conception & Diagramme de Classes

```mermaid
classDiagram
    class User {
        +UUID id
        +String email
        +String passwordHash
        +String firstName
        +String lastName
        +Boolean isActive
        +DateTime createdAt
        +register()
        +validatePassword(rawPassword) Boolean
        +deactivateAccount()
    }

    class Role {
        +Long id
        +String code
        +String description
    }

    class RefreshToken {
        +UUID id
        +String token
        +DateTime expiryDate
        +Boolean isRevoked
        +isExpired() Boolean
        +revoke()
    }

    class UserRole {
        +UUID userId
        +Long roleId
        +DateTime assignedAt
    }

    User "1" --> "*" RefreshToken : gère
    User "1" --> "*" UserRole : possède
    Role "1" --> "*" UserRole : définit
```

### Description des Entités
- **`User`** : Entité racine représentant l'utilisateur avec son mot de passe haché par BCrypt et son statut d'activation.
- **`Role`** : Catalogue des rôles du système (`ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_RECRUITER`, `ROLE_ADMIN`).
- **`RefreshToken`** : Token persistant en base avec date d'expiration pour renouveler l'access token sans ressaisir les identifiants.
- **`UserRole`** : Table de jointure modélisant l'attribution de rôles multiples.

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `gamification-service` (Port 8084)
- **Endpoint appelé :** `POST http://localhost:8084/api/gamification/profiles`
- **Payload transmis :** `{ "userId": "usr_9823f4a1", "initialXP": 0 }`
- **Code de retour attendu :** `201 Created`
- **Justification technique :** Dès qu'un nouvel utilisateur s'inscrit, son compte doit immédiatement disposer d'un profil de gamification initialisé avant que l'utilisateur n'accède à son tableau de bord. Un appel synchrone garantit la cohérence immédiate du profil.
- **Service également appelé en synchrone par d'autres microservices :**
  - Expose `GET /api/auth/users/:id/summary` appelé par `course-service` et `community-service` pour obtenir le nom et l'avatar de l'auteur sans dupliquer les profils.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `UserRegisteredEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic)
- **Routing Key :** `auth.user.registered`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_auth_101",
    "eventType": "USER_REGISTERED",
    "userId": "usr_9823f4a1",
    "email": "sarah.benali@example.com",
    "role": "ROLE_STUDENT",
    "timestamp": "2026-11-09T10:00:00Z"
  }
  ```
- **Justification :** Permet à `notification-service` d'envoyer l'e-mail de bienvenue et d'activation de manière asynchrone sans ralentir la réponse HTTP d'inscription.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événement consommé :** `UserReportedAbuseEvent`
- **Queue écoutée :** `auth.abuse_reported.queue`
- **Émetteur d'origine :** `community-service`
- **Routing Key :** `community.user.reported`
- **Action déclenchée :** À la réception du message, si le nombre de signalements dépasse le seuil critique (3 abus vérifiés), le service passe automatiquement `isActive = false` pour suspendre temporairement le compte jusqu'à modération humaine.

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/auth/register` | Inscription d'un utilisateur | `201 Created` | `400 Bad Request`, `409 Conflict` |
| `POST` | `/api/auth/login` | Authentification & émission JWT | `200 OK` | `401 Unauthorized` |
| `POST` | `/api/auth/refresh` | Renouvellement de l'access token | `200 OK` | `403 Forbidden` |
| `GET` | `/api/auth/users/:id/summary` | Profil résumé pour les autres microservices | `200 OK` | `404 Not Found` |

---

## ⚙️ 5. Configuration d'Environnement (`.env`)

```properties
PORT=8081
DATABASE_URL=postgresql://postgres:password@localhost:5432/skillquest_auth
JWT_SECRET=super_secret_jwt_key_skillquest_2026_very_long_secure
JWT_EXPIRATION_MS=900000
REFRESH_TOKEN_EXPIRATION_MS=604800000
GAMIFICATION_SERVICE_URL=http://localhost:8084
RABBITMQ_URI=amqp://guest:guest@localhost:5672
```
