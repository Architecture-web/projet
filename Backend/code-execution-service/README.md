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

> **En résumé :**
> - **Synchrone (REST) :** Appelle `course-service` pour récupérer les cas de tests avant de lancer l'exécution.
> - **Asynchrone (Publie) :** Émet `CodeSolutionPassedEvent` dès que le code de l'élève est validé (donne des points XP et valide la leçon).
> - **Asynchrone (Consomme) :** Écoute `ExerciseCreatedEvent` pour préparer à l'avance les conteneurs Docker de compilation.

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `course-service` (Port 8082)
- **Endpoint appelé :** `GET http://localhost:8082/api/courses/:courseId/exercises/:exerciseId/test-cases`
- **Explication simple :** Le service ne stocke pas les tests. Avant de compiler le code, il demande en direct à `course-service` la liste des tests à exécuter.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `CodeSolutionPassedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic `code.solution.passed`)
- **Explication simple :** Dès que tous les tests passent avec succès, le service publie cet événement. Cela permet aux autres services de donner des points d'XP à l'élève, de valider sa leçon et d'afficher un message de succès en direct.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événement consommé :** `ExerciseCreatedEvent` (émis par `course-service`)
- **Queue écoutée :** `code_exec.exercise_created.queue`
- **Explication simple :** Quand un enseignant ajoute un nouvel exercice dans un cours, le service télécharge et prépare à l'avance l'environnement Docker pour exécuter le code sans temps d'attente.

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
