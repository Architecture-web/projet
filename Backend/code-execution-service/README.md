# Code Execution & Sandbox Service (Microservice 2 – Étudiant 3)

## 📌 1. Présentation du Service
Cœur technique pratique de SkillQuest, ce service prend en charge l'exécution isolée et l'évaluation automatisée du code source soumis par les étudiants au sein d'une sandbox sécurisée.

- **Port local :** `8083`
- **Responsable :** Étudiant 3
- **Bases de données :** **Redis 7** (file d'attente FIFO) + **PostgreSQL 15** (`skillquest_submissions`)
- **Justification du choix :** Redis offre un traitement en mémoire ultra-rapide des jobs de compilation sans bloquer le serveur web lors des pics de soumission. PostgreSQL assure la persistance ACID de l'historique complet des évaluations, des temps d'exécution et de la mémoire consommée.

---

## 📐 2. Conception & Diagramme de Classes

```mermaid
classDiagram
    class CodeSubmission {
        +UUID id
        +String userId
        +String exerciseId
        +ProgrammingLanguage language
        +String sourceCode
        +SubmissionStatus status
        +Integer totalTests
        +Integer passedTests
        +Integer executionTimeMs
        +Double memoryUsedMb
        +DateTime submittedAt
        +markPending()
        +completeEvaluation(results)
    }

    class TestCase {
        +UUID id
        +String exerciseId
        +String inputData
        +String expectedOutput
        +Boolean isHidden
        +Integer timeoutSeconds
    }

    class ExecutionResult {
        +UUID id
        +UUID submissionId
        +UUID testCaseId
        +Boolean passed
        +String actualOutput
        +String errorLog
        +Integer durationMs
    }

    class ExecutionSandboxPolicy {
        +Integer maxMemoryMb
        +Integer maxTimeoutSeconds
        +Boolean networkAccessAllowed
        +Boolean allowRootExecution
    }

    CodeSubmission "1" *-- "*" ExecutionResult : produit
    TestCase "1" --> "*" ExecutionResult : évalue
    CodeSubmission ..> ExecutionSandboxPolicy : contraint par
```

### Description des Entités
- **`CodeSubmission`** : Entité principale enregistrant le code soumis, le langage (Java, Python, JS) et le verdict final (PENDING, SUCCESS, FAILED, TIMEOUT).
- **`TestCase`** : Représentation d'un cas de test avec ses entrées et sorties attendues (publics ou secrets).
- **`ExecutionResult`** : Résultat individuel d'exécution pour chaque test case (sortie obtenue, trace d'erreur éventuelle).
- **`ExecutionSandboxPolicy`** : Règles d'isolation appliquées au conteneur Docker éphémère (128 Mo RAM, 2s timeout, pas de réseau).

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `course-service` (Port 8082)
- **Endpoint appelé :** `GET http://localhost:8082/api/courses/:courseId/exercises/:exerciseId/test-cases`
- **Réponse reçue :**
  ```json
  [
    { "id": "tc_1", "input": "[2, 7, 11, 15], target = 9", "expectedOutput": "[0, 1]", "isHidden": false },
    { "id": "tc_2", "input": "[3, 2, 4], target = 6", "expectedOutput": "[1, 2]", "isHidden": false },
    { "id": "tc_3", "input": "[3, 3], target = 6", "expectedOutput": "[0, 1]", "isHidden": true }
  ]
  ```
- **Code de retour attendu :** `200 OK`
- **Justification technique :** Le service d'exécution de code est strictement découplé du contenu pédagogique : il ne stocke aucun énoncé ni jeu de test en dur. Avant de lancer le conteneur sandbox, il interroge en direct le `course-service` pour obtenir la liste à jour des assertions à tester.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `CodeSolutionPassedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic)
- **Routing Key :** `code.solution.passed`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_code_9918",
    "eventType": "CODE_SOLUTION_PASSED",
    "userId": "usr_9823f4a1",
    "exerciseId": "ex_binary_search_01",
    "language": "JAVA",
    "executionTimeMs": 42,
    "memoryUsedMb": 28.5,
    "timestamp": "2026-11-09T14:35:00Z"
  }
  ```
- **Justification :** Événement pivot de l'architecture ! Sa publication déclenche en parallèle la progression du cours (`course-service`), le crédit d'XP (+50 XP dans `gamification-service`), l'ouverture de l'espace de discussion (`community-service`) et l'alerte temps réel (`notification-service`).

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événement consommé :** `ExerciseCreatedEvent`
- **Queue écoutée :** `code_exec.exercise_created.queue`
- **Émetteur d'origine :** `course-service`
- **Routing Key :** `course.exercise.created`
- **Action déclenchée :** À la création d'un nouvel exercice par un instructeur, le service pré-chauffe (*warm-up*) les images Docker et les compilateurs associés sur le serveur d'exécution pour que la première soumission de l'élève s'exécute sans latence d'initialisation d'image.

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/submissions/run` | Test rapide contre les assertions publiques | `200 OK` | `400 Bad Request` |
| `POST` | `/api/submissions/submit` | Soumission définitive évaluée en tâche de fond | `202 Accepted` | `400 Bad Request` |
| `GET` | `/api/submissions/{id}` | Statut de l'évaluation et métriques CPU/RAM | `200 OK` | `404 Not Found` |

---

## ⚙️ 5. Configuration d'Environnement (`.env`)

```properties
PORT=8083
DATABASE_URL=postgresql://postgres:password@localhost:5432/skillquest_submissions
REDIS_URL=redis://localhost:6379
COURSE_SERVICE_URL=http://localhost:8082
RABBITMQ_URI=amqp://guest:guest@localhost:5672
EXECUTION_TIMEOUT_SECONDS=2
MAX_MEMORY_MB=128
```
