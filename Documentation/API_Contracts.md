# Contrats d'API & Spécifications des Événements – SkillQuest
## Documentation Technique pour les 6 Microservices (Séance 4 & Implémentation)

Ce document formalise l'ensemble des contrats d'interfaces (endpoints REST, corps de requêtes/réponses, codes HTTP et événements RabbitMQ) pour permettre à chaque membre de l'équipe de développer son service en totale indépendance.

---

## 📑 Sommaire

1. [Normes Globales & Headers](#normes-globales--headers)
2. [Service Commun : Auth & Identity Service](#1-service-commun--auth--identity-service-port-8081)
3. [Microservice 1 : Course Content Service](#2-microservice-1--course-content-service-port-8082)
4. [Microservice 2 : Code Execution & Sandbox Service](#3-microservice-2--code-execution--sandbox-service-port-8083)
5. [Microservice 3 : Gamification & Certification Service](#4-microservice-3--gamification--certification-service-port-8084)
6. [Microservice 4 : Job Board & Recruitment Service](#5-microservice-4--job-board--recruitment-service-port-8085)
7. [Microservice 5 : Community & Peer-Review Service](#6-microservice-5--community--peer-review-service-port-8086)
8. [Microservice 6 : Notification & Alert Service](#7-microservice-6--notification--alert-service-port-8087)
9. [Spécification des Événements Asynchrones (RabbitMQ)](#8-spécification-des-événements-asynchrones-rabbitmq)

---

## Normes Globales & Headers

Toutes les requêtes passent par l'**API Gateway** (`http://localhost:8080`).

### Headers Injectés par la Gateway vers les Microservices Internes
Une fois le token JWT validé, l'API Gateway injecte automatiquement :
```http
X-User-Id: usr_9823f4a1-7c2a-4a61-b952
X-User-Email: student@example.com
X-User-Role: ROLE_STUDENT
```

### Format Standard des Réponses d'Erreur
```json
{
  "timestamp": "2026-11-08T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Le champ 'email' est obligatoire et doit être valide.",
  "path": "/api/auth/register"
}
```

---

## 1. Service Commun : Auth & Identity Service (Port 8081)

### `POST /api/auth/register`
- **Description :** Inscription d'un nouvel utilisateur (Apprenant, Instructeur, Recruteur).
- **Request Body :**
  ```json
  {
    "email": "sarah.dev@example.com",
    "password": "SecurePassword123!",
    "firstName": "Sarah",
    "lastName": "Benali",
    "role": "ROLE_STUDENT"
  }
  ```
- **Responses :**
  - `201 Created` :
    ```json
    {
      "userId": "usr_9823f4a1",
      "email": "sarah.dev@example.com",
      "message": "Compte créé avec succès"
    }
    ```
  - `409 Conflict` : Email déjà utilisé.
  - `400 Bad Request` : Mot de passe trop faible ou champs manquants.

### `POST /api/auth/login`
- **Description :** Authentification et délivrance des tokens.
- **Request Body :**
  ```json
  {
    "email": "sarah.dev@example.com",
    "password": "SecurePassword123!"
  }
  ```
- **Responses :**
  - `200 OK` :
    ```json
    {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
      "refreshToken": "d8e27c10-8b1b-4...",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "user": {
        "id": "usr_9823f4a1",
        "email": "sarah.dev@example.com",
        "role": "ROLE_STUDENT"
      }
    }
    ```
  - `401 Unauthorized` : Identifiants invalides.

---

## 2. Microservice 1 : Course Content Service (Port 8082)

### `GET /api/courses`
- **Description :** Récupération de la liste paginée des formations.
- **Query Params :** `page=0`, `size=10`, `level=INTERMEDIATE`, `tag=JAVA`
- **Response `200 OK` :**
  ```json
  {
    "content": [
      {
        "id": "crs_java_adv_01",
        "title": "Java & Architecture Distribuée",
        "slug": "java-architecture-distribuee",
        "difficulty": "INTERMEDIATE",
        "modulesCount": 6,
        "totalDurationMinutes": 360,
        "tags": ["Java", "Spring Boot", "Microservices"]
      }
    ],
    "totalPages": 3,
    "totalElements": 28
  }
  ```

### `PUT /api/courses/{courseId}/progress`
- **Description :** Sauvegarde de l'avancement d'un élève.
- **Request Body :**
  ```json
  {
    "completedLessonId": "lsn_spring_boot_03"
  }
  ```
- **Response `200 OK` :**
  ```json
  {
    "courseId": "crs_java_adv_01",
    "completionPercentage": 45,
    "isCompleted": false
  }
  ```

---

## 3. Microservice 2 : Code Execution & Sandbox Service (Port 8083)

### `POST /api/submissions/run`
- **Description :** Exécution rapide contre les tests publics de l'énoncé.
- **Request Body :**
  ```json
  {
    "exerciseId": "ex_binary_search_01",
    "language": "JAVA",
    "sourceCode": "public class Solution { public int search(int[] nums, int target) { ... } }"
  }
  ```
- **Response `200 OK` :**
  ```json
  {
    "status": "SUCCESS",
    "allTestsPassed": true,
    "passedTests": 3,
    "totalTests": 3,
    "executionTimeMs": 42,
    "memoryUsedMb": 28.5
  }
  ```

### `POST /api/submissions/submit`
- **Description :** Évaluation globale définitive (tests publics + secrets).
- **Request Body :** Même format que `/run`.
- **Response `202 Accepted` :**
  ```json
  {
    "submissionId": "sub_847192",
    "status": "PENDING",
    "message": "Soumission mise en file d'attente d'évaluation"
  }
  ```

---

## 4. Microservice 3 : Gamification & Certification Service (Port 8084)

### `GET /api/gamification/leaderboard`
- **Description :** Classement des meilleurs développeurs par points d'XP.
- **Response `200 OK` :**
  ```json
  [
    { "rank": 1, "userId": "usr_9823f4a1", "fullName": "Sarah Benali", "totalXP": 3250, "badgesCount": 8 },
    { "rank": 2, "userId": "usr_1029ab42", "fullName": "Alexandre Martin", "totalXP": 2980, "badgesCount": 6 }
  ]
  ```

### `GET /api/certifications/:userId` *(Appel Inter-Service Synchrone utilisé par le Job Board)*
- **Description :** Renvoie les compétences certifiées d'un candidat.
- **Response `200 OK` :**
  ```json
  {
    "userId": "usr_9823f4a1",
    "totalXP": 3250,
    "level": "Senior Learner",
    "verifiedBadges": [
      { "id": "bdg_algo_master", "name": "Algorithmique Avancée", "obtainedAt": "2026-10-12" },
      { "id": "bdg_spring_pro", "name": "Expert Spring Boot", "obtainedAt": "2026-11-02" }
    ],
    "certifications": [
      {
        "certificateId": "cert_a91b2c",
        "title": "Architecte Microservices Certifié",
        "hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
      }
    ]
  }
  ```

---

## 5. Microservice 4 : Job Board & Recruitment Service (Port 8085)

### `POST /api/jobs`
- **Description :** Dépôt d'une offre d'emploi par un recruteur.
- **Request Body :**
  ```json
  {
    "title": "Développeur Backend Java / Spring Boot",
    "companyName": "TechScale Solutions",
    "location": "Paris (Hybride)",
    "requiredSkills": ["Java", "Spring Boot", "RabbitMQ"],
    "minimumXP": 1500,
    "description": "Nous recrutons un développeur backend pour participer à la refonte microservices..."
  }
  ```
- **Response `201 Created` :**
  ```json
  {
    "jobId": "job_94812",
    "status": "PUBLISHED",
    "createdAt": "2026-11-08T11:00:00Z"
  }
  ```

### `POST /api/jobs/{jobId}/apply`
- **Description :** L'apprenant postule à une offre.
- **Response `201 Created` :**
  ```json
  {
    "applicationId": "app_55419",
    "jobId": "job_94812",
    "applicantId": "usr_9823f4a1",
    "status": "UNDER_REVIEW",
    "matchingScore": 92
  }
  ```

---

## 6. Microservice 5 : Community & Peer-Review Service (Port 8086)

### `GET /api/community/threads?exerciseId={id}`
- **Description :** Récupération du fil de discussion associé à un exercice.
- **Response `200 OK` :**
  ```json
  [
    {
      "threadId": "thr_3321",
      "authorName": "Marc D.",
      "title": "Optimisation en O(log N) possible sur le test 4 ?",
      "upvotes": 12,
      "commentsCount": 4,
      "createdAt": "2026-11-07T18:22:00Z"
    }
  ]
  ```

### `POST /api/community/posts/{postId}/vote`
- **Description :** Voter pour une explication utile (+1 / -1).
- **Request Body :** `{ "value": 1 }`
- **Response `200 OK` :** `{ "newScore": 13 }`

---

## 7. Microservice 6 : Notification & Alert Service (Port 8087)

### `WS /ws/notifications`
- **Description :** Canal WebSocket persistant pour pousser les alertes en direct dans l'interface de l'étudiant.
- **Format du message reçu par le client :**
  ```json
  {
    "type": "BADGE_UNLOCKED",
    "title": "Nouveau badge débloqué !",
    "message": "Félicitations, vous avez obtenu le badge 'Clean Coder' (+50 XP).",
    "timestamp": "2026-11-08T12:00:00Z"
  }
  ```

---

## 8. Spécification des Événements Asynchrones (RabbitMQ)

- **Exchange Name :** `skillquest.events` (Type: `topic`)

### Événement 1 : `CodeSolutionPassedEvent`
- **Routing Key :** `code.solution.passed`
- **Émetteur :** `code-execution-service`
- **Consommateurs :** `gamification-service`, `course-service`, `notification-service`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_9918274a",
    "eventType": "CODE_SOLUTION_PASSED",
    "userId": "usr_9823f4a1",
    "exerciseId": "ex_binary_search_01",
    "language": "JAVA",
    "executionTimeMs": 42,
    "memoryUsedMb": 28.5,
    "timestamp": "2026-11-08T12:00:00Z"
  }
  ```

### Événement 2 : `BadgeUnlockedEvent`
- **Routing Key :** `gamification.badge.unlocked`
- **Émetteur :** `gamification-service`
- **Consommateur :** `notification-service`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_441829",
    "eventType": "BADGE_UNLOCKED",
    "userId": "usr_9823f4a1",
    "badgeId": "bdg_algo_master",
    "badgeName": "Algorithmique Avancée",
    "xpAwarded": 50,
    "timestamp": "2026-11-08T12:00:01Z"
  }
  ```
