# Gamification & Certification Service (Microservice 3 – Étudiant 4)

## 📌 1. Présentation du Service
Ce microservice alimente le moteur d'engagement : comptabilité des points d'expérience (XP), calcul des paliers de progression, déblocage de badges et émission de certificats PDF cryptographiquement vérifiables.

- **Port local :** `8084`
- **Responsable :** Étudiant 4
- **Base de données :** **PostgreSQL 15** (`skillquest_gamification`)
- **Justification de la base :** Les points d'expérience et les classements requièrent une intégrité transactionnelle stricte (ACID). PostgreSQL prévient les risques de duplication de points (double dépense de récompense) lors de soumissions concurrentes et garantit l'unicité des certificats émis.

---

## 📐 2. Conception & Diagramme de Classes

```mermaid
classDiagram
    class UserProfileGamification {
        +UUID userId
        +Integer totalXP
        +Integer currentLevel
        +Integer globalRank
        +DateTime updatedAt
        +addXP(amount) Boolean
        +evaluateLevelProgression()
    }

    class Badge {
        +Long id
        +String codeName
        +String title
        +String description
        +String iconUrl
        +Integer requiredXP
    }

    class UserBadge {
        +UUID id
        +UUID userId
        +Long badgeId
        +DateTime unlockedAt
    }

    class Certificate {
        +UUID id
        +UUID userId
        +String courseId
        +String studentFullName
        +String courseTitle
        +String certificateHash
        +DateTime issuedAt
        +generateHash() String
        +verifyHash(hash) Boolean
    }

    UserProfileGamification "1" --> "*" UserBadge : possède
    Badge "1" --> "*" UserBadge : attribué via
    UserProfileGamification "1" --> "*" Certificate : certifié par
```

### Description des Entités
- **`UserProfileGamification`** : Profil joueur de l'apprenant contenant son solde d'XP, son niveau et son rang.
- **`Badge`** : Catalogue des récompenses déblocables selon des jalons d'XP ou des réussites de parcours.
- **`UserBadge`** : Association horodatée attestant de l'obtention d'une compétence par un étudiant.
- **`Certificate`** : Certificat officiel de complétion muni d'une signature hashée SHA-256 infalsifiable.

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `course-service` (Port 8082)
- **Endpoint appelé :** `GET http://localhost:8082/api/courses/:courseId`
- **Réponse reçue :** `{ "id": "crs_java_adv_01", "title": "Java & Architecture Distribuée", "instructorId": "usr_ins_42" }`
- **Code de retour attendu :** `200 OK`
- **Justification technique :** Lorsqu'un cours est complété, le service doit générer le certificat officiel. Il interroge en direct le `course-service` pour obtenir le titre exact et officiel du cours ainsi que les métadonnées requises pour le tampon du PDF sans dupliquer ces données.
- **Service également appelé en synchrone par d'autres microservices :**
  - Expose `GET /api/certifications/:userId` appelé par `job-board-service` pour vérifier les compétences réelles d'un candidat avant transmission de son dossier au recruteur.
  - Expose `POST /api/gamification/profiles` appelé par `auth-service` lors de l'inscription pour initialiser le compte joueur.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `BadgeUnlockedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic)
- **Routing Key :** `gamification.badge.unlocked`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_gamif_4418",
    "eventType": "BADGE_UNLOCKED",
    "userId": "usr_9823f4a1",
    "badgeId": "bdg_algo_master",
    "badgeTitle": "Maître Algorithmique",
    "xpAwarded": 50,
    "newTotalXP": 1550,
    "timestamp": "2026-11-09T14:35:05Z"
  }
  ```
- **Justification :** Informe `job-board-service` pour recalculer les scores de matching d'emploi et `notification-service` pour diffuser une célébration live sur l'écran de l'étudiant.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événements consommés :**
  1. `CodeSolutionPassedEvent` (Queue: `gamification.code_passed.queue`) émis par `code-execution-service`.
     - *Action :* Crédite +50 XP, recalcule le niveau et vérifie si un nouveau badge doit être débloqué.
  2. `CourseCompletedEvent` (Queue: `gamification.course_completed.queue`) émis par `course-service`.
     - *Action :* Déclenche la génération du certificat numérique signé et l'émission de `CertificateIssuedEvent`.

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/gamification/leaderboard` | Top 50 des meilleurs codeurs | `200 OK` | `400 Bad Request` |
| `GET` | `/api/gamification/badges/user/{userId}` | Badges débloqués par un élève | `200 OK` | `404 Not Found` |
| `GET` | `/api/certifications/:userId` | Profil certifié (appelé par le Job Board) | `200 OK` | `404 Not Found` |
| `GET` | `/api/certifications/{id}/verify` | Vérification publique par empreinte hashée | `200 OK` | `404 Not Found` |
| `POST` | `/api/gamification/profiles` | Initialisation du profil (appelé par Auth) | `201 Created` | `409 Conflict` |

---

## ⚙️ 5. Configuration d'Environnement (`.env`)

```properties
PORT=8084
DATABASE_URL=postgresql://postgres:password@localhost:5432/skillquest_gamification
COURSE_SERVICE_URL=http://localhost:8082
RABBITMQ_URI=amqp://guest:guest@localhost:5672
```
