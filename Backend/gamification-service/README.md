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

> **En résumé :**
> - **Synchrone (REST) :** Appelle `course-service` pour obtenir le titre exact du cours avant de générer le certificat.
> - **Asynchrone (Publie) :** Émet `BadgeUnlockedEvent` quand un élève débloque un nouveau badge de compétence.
> - **Asynchrone (Consomme) :** Écoute `CodeSolutionPassedEvent` (pour ajouter +50 XP) et `CourseCompletedEvent` (pour créer le certificat).

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `course-service` (Port 8082)
- **Endpoint appelé :** `GET http://localhost:8082/api/courses/:courseId`
- **Explication simple :** Quand un élève termine un cours, le service appelle `course-service` pour obtenir le titre exact du cours à écrire sur son certificat officiel.
- **Service également appelé en synchrone par les autres :**
  - Expose `GET /api/certifications/:userId` : appelé par `job-board-service` pour vérifier les compétences et badges réels d'un candidat.
  - Expose `POST /api/gamification/profiles` : appelé par `auth-service` pour créer le profil de points à l'inscription.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `BadgeUnlockedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic `gamification.badge.unlocked`)
- **Explication simple :** Quand un apprenant atteint un palier de points ou réussit un défi, le service publie cet événement pour informer le Job Board et afficher une animation de félicitations.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événements consommés :**
  1. `CodeSolutionPassedEvent` (émis par `code-execution-service`) : Ajoute automatiquement +50 XP au joueur et recalcule son niveau.
  2. `CourseCompletedEvent` (émis par `course-service`) : Génère automatiquement le certificat officiel signé de fin de parcours.

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
