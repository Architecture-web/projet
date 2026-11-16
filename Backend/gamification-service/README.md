# Gamification & Certification Service (Microservice 3 – Étudiant 4)

## 📌 1. Présentation du Service
Ce microservice alimente le moteur d'engagement : comptabilité des points d'expérience (XP), calcul des paliers de progression, déblocage de badges et émission de certificats PDF cryptographiquement vérifiables.

- **Technologie :** Spring Boot 3.4 / Java 17 (Spring Web, Spring Data JPA, Spring AMQP)
- **Port local :** `8084`
- **Responsable :** Étudiant 4
- **Base de données :** **PostgreSQL 15** (`skillquest_gamification`) — base dédiée au service
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
    }

    UserProfileGamification "1" --> "*" UserBadge : possède
    Badge "1" --> "*" UserBadge : attribué via
    UserProfileGamification "1" --> "*" Certificate : certifié par
```

### Description des Entités
- **`UserProfileGamification`** : Profil joueur de l'apprenant contenant son solde d'XP, son niveau et son rang.
- **`Badge`** : Catalogue des récompenses déblocables selon des jalons d'XP (`requiredXP`) ou des réussites de parcours (`requiredXP = null`).
- **`UserBadge`** : Association horodatée attestant de l'obtention d'une compétence par un étudiant (unique par couple utilisateur/badge).
- **`Certificate`** : Certificat officiel de complétion muni d'une signature **HMAC-SHA256** infalsifiable (unique par couple utilisateur/cours).
- **`ProcessedEvent`** *(technique)* : journal d'idempotence pour ignorer un message RabbitMQ re-livré.

### Règles métier
| Règle | Détail |
| :--- | :--- |
| Niveau | `niveau = 1 + ⌊√(XP / 100)⌋` → niveau 2 à 100 XP, 3 à 400 XP, 4 à 900 XP… |
| XP solution validée | +50 XP (configurable : `gamification.xp.code-solution-passed`) |
| Badges de palier | `FIRST_STEPS` 50 XP · `CODE_APPRENTICE` 200 XP · `CODE_WARRIOR` 500 XP · `CODE_MASTER` 1000 XP |
| Badge de parcours | `COURSE_GRADUATE` : attribué à l'émission d'un certificat |
| Classement | XP décroissant, égalité = même rang (ex. 1, 1, 3) |
| Empreinte du certificat | HMAC-SHA256 (clé `CERTIFICATE_SECRET`) sur utilisateur + cours + nom + titre + date |

### Garanties de cohérence
- Chaque ajout d'XP est une **transaction ACID** avec **verrou pessimiste** (`SELECT … FOR UPDATE`) sur le profil : 40 ajouts concurrents de 10 XP donnent exactement 400 XP (testé).
- Contraintes d'unicité en base : `(user_id, badge_id)` et `(user_id, course_id)`.
- Un événement RabbitMQ re-livré (même `eventId`) ne crédite pas l'XP deux fois.
- L'événement `BadgeUnlockedEvent` est publié **après le commit** de la transaction.

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

> **En résumé :**
> - **Synchrone (REST) :** Appelle `course-service` pour obtenir le titre exact du cours avant de générer le certificat.
> - **Asynchrone (Publie) :** Émet `BadgeUnlockedEvent` quand un élève débloque un nouveau badge de compétence.
> - **Asynchrone (Consomme) :** Écoute `CodeSolutionPassedEvent` (pour ajouter +50 XP) et `CourseCompletedEvent` (pour créer le certificat).

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `course-service` (Port 8082)
- **Endpoint appelé :** `GET http://localhost:8082/api/courses/:courseId` (le champ JSON `title` est lu)
- **Explication simple :** Quand un élève termine un cours, le service appelle `course-service` pour obtenir le titre exact du cours à écrire sur son certificat officiel. L'appel a des timeouts (2 s connexion / 5 s lecture) et est fait hors transaction SQL.
- **Service également appelé en synchrone par les autres :**
  - Expose `GET /api/certifications/:userId` : appelé par `job-board-service` pour vérifier les compétences et badges réels d'un candidat.
  - Expose `POST /api/gamification/profiles` : appelé par `auth-service` pour créer le profil de points à l'inscription.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `BadgeUnlockedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (topic, routing key `gamification.badge.unlocked`)
- **Explication simple :** Quand un apprenant atteint un palier de points ou réussit un défi, le service publie cet événement pour informer le Job Board et afficher une animation de félicitations.

```json
{
  "eventId": "6f1c…", "userId": "2fa4…", "badgeId": 1, "badgeCode": "FIRST_STEPS",
  "badgeTitle": "Premiers pas", "totalXP": 50, "unlockedAt": "2026-11-16T16:31:40Z"
}
```

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événements consommés :**
  1. `CodeSolutionPassedEvent` (émis par `code-execution-service`, routing key `code.solution.passed`, queue `gamification.code-solution-passed.queue`) : ajoute automatiquement +50 XP au joueur et recalcule son niveau.
  2. `CourseCompletedEvent` (émis par `course-service`, routing key `course.completed`, queue `gamification.course-completed.queue`) : génère automatiquement le certificat officiel signé de fin de parcours.

```json
// CodeSolutionPassedEvent
{ "eventId": "uuid (optionnel, idempotence)", "userId": "uuid", "challengeId": "string", "occurredAt": "ISO-8601" }

// CourseCompletedEvent
{ "eventId": "uuid", "userId": "uuid", "courseId": "string", "studentFullName": "Ada Lovelace", "occurredAt": "ISO-8601" }
```

> ⚠️ **À aligner avec les autres étudiants** : les routing keys et les champs ci-dessus sont les conventions retenues côté gamification. Les champs inconnus sont ignorés ; les routing keys se changent dans `application.yml` (`gamification.messaging.*`).

- **Résilience :** 3 tentatives (backoff exponentiel) puis le message part dans la dead-letter queue `gamification.dead-letter.queue` (exchange `skillquest.events.dlx`).

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/gamification/leaderboard?limit=50` | Top 50 des meilleurs codeurs (`limit` entre 1 et 50) | `200 OK` | `400 Bad Request` |
| `GET` | `/api/gamification/badges/user/{userId}` | Badges débloqués par un élève | `200 OK` | `404 Not Found` |
| `GET` | `/api/certifications/{userId}` | Profil certifié : XP, niveau, badges, certificats (appelé par le Job Board) | `200 OK` | `404 Not Found` |
| `GET` | `/api/certifications/{id}/verify?hash=…` | Vérification publique d'un certificat (`hash` optionnel) → `{"valid": true/false, …}` | `200 OK` | `404 Not Found` |
| `POST` | `/api/gamification/profiles` | Initialisation du profil, body `{"userId": "<uuid>"}` (appelé par Auth) | `201 Created` | `400`, `409 Conflict` |

Endpoints complémentaires : `GET /api/gamification/profiles/{userId}`, `GET /api/gamification/badges` (catalogue), `GET /api/certifications/{id}/pdf` (télécharge le certificat PDF).
Les erreurs suivent le format RFC 7807 (`application/problem+json`). Documentation interactive : **http://localhost:8084/swagger-ui.html**. Santé : `/actuator/health`.

---

## ⚙️ 5. Configuration d'Environnement

Spring Boot ne lit pas un fichier `.env` tout seul : ces variables sont à définir dans l'environnement (ou dans la configuration de lancement d'IntelliJ). **Toutes ont une valeur par défaut adaptée au développement local**, voir `.env.example`.

```properties
PORT=8084
DB_URL=jdbc:postgresql://localhost:5432/skillquest_gamification
DB_USERNAME=postgres
DB_PASSWORD=password
COURSE_SERVICE_URL=http://localhost:8082
RABBITMQ_URI=amqp://guest:guest@localhost:5672
CERTIFICATE_SECRET=change-me-in-production
PUBLIC_BASE_URL=http://localhost:8084
```

> Différence avec la première version du README : la base se configure avec `DB_URL` (URL **JDBC**), `DB_USERNAME` et `DB_PASSWORD`, car le driver JDBC n'accepte pas le format `postgresql://user:pass@host`.

---

## 🚀 6. Lancer le service

### 6.1. Démarrer PostgreSQL et RabbitMQ
Avec Docker :
```bash
docker compose up -d
```
Sans Docker : installer PostgreSQL 15 et RabbitMQ, puis créer la base `skillquest_gamification`. Les tables et les 5 badges par défaut sont créés automatiquement au démarrage.

### 6.2. Démarrer l'application
```bash
./mvnw spring-boot:run
```
(ou ouvrir le dossier dans IntelliJ et lancer `GamificationServiceApplication`)

### 6.3. Lancer les tests
```bash
./mvnw test
```
35 tests (H2 en mode PostgreSQL ; RabbitMQ et course-service sont simulés) : calcul de niveau, empreinte HMAC, XP/badges, concurrence, idempotence, listeners, contrats REST, génération du PDF.

### 6.4. Scénario de démonstration
```bash
# 1. Auth crée le profil
curl -X POST localhost:8084/api/gamification/profiles -H "Content-Type: application/json" \
     -d '{"userId":"2fa4b1c6-d374-450c-86ea-41f6f775fab6"}'
```
2. Dans RabbitMQ (http://localhost:15672 → Exchanges → `skillquest.events` → *Publish message*, propriété `content_type = application/json`), routing key `code.solution.passed` :
   `{"userId":"2fa4b1c6-d374-450c-86ea-41f6f775fab6","challengeId":"c1"}` → +50 XP et badge `FIRST_STEPS`.
3. Même chose avec la routing key `course.completed` et `{"userId":"2fa4b1c6-d374-450c-86ea-41f6f775fab6","courseId":"1","studentFullName":"Ada Lovelace"}` → certificat (nécessite `course-service` démarré sur le port 8082).
```bash
curl localhost:8084/api/gamification/leaderboard
curl localhost:8084/api/certifications/2fa4b1c6-d374-450c-86ea-41f6f775fab6
```

### 6.5. Démo complète sans Docker (Windows)
1. Lancer `demo\start-infra.bat` : démarre PostgreSQL (5432), RabbitMQ (5672, interface http://localhost:15672, `guest`/`guest`) et un faux `course-service` (8082, `demo\fake-course-service.js`).
2. Lancer `GamificationServiceApplication` **sans** le profil `dev` (le profil `dev` désactive RabbitMQ).
3. Publier des événements depuis l'interface RabbitMQ (Exchanges → `skillquest.events` → *Publish message*, propriété `content_type` = `application/json`) :
   - routing key `code.solution.passed` : `{"eventId":"<uuid>","userId":"<uuid>","challengeId":"c1"}` → +50 XP (4 messages = 200 XP, niveau 2, badges `FIRST_STEPS` puis `CODE_APPRENTICE`) ; renvoyer le même `eventId` est ignoré ;
   - routing key `course.completed` : `{"userId":"<uuid>","courseId":"1","studentFullName":"Aziz Jemli"}` → certificat + badge `COURSE_GRADUATE`.
4. Consulter `GET /api/certifications/<userId>`, puis `/api/certifications/<certificateId>/verify?hash=…` et `/api/certifications/<certificateId>/pdf`. Un exemple de PDF est fourni : `demo\certificat-demo.pdf`.
