# Course & Learning Content Service (Microservice 1 – Étudiant 2)

## 📌 1. Présentation du Service
Ce microservice gère le catalogue pédagogique, l'arborescence des chapitres/leçons et le suivi précis de l'avancement d'apprentissage de chaque étudiant.

- **Port local :** `8082`
- **Responsable :** Étudiant 2
- **Base de données :** **MongoDB 6.0** (`skillquest_courses`)
- **Justification de la base :** La structure des cours est intrinsèquement hiérarchique et polymorphe (un cours contient une liste ordonnée de modules, qui contiennent des leçons textuelles, vidéo et des références d'exercices). MongoDB évite les jointures coûteuses et stocke l'arborescence dans un seul document JSON structuré.

---

## 📐 2. Conception & Diagramme de Classes

```mermaid
classDiagram
    class Course {
        +String id
        +String title
        +String slug
        +String description
        +DifficultyLevel difficulty
        +String instructorId
        +Boolean isPublished
        +DateTime createdAt
        +addModule(module)
        +publish()
    }

    class Module {
        +String moduleId
        +String title
        +Integer orderIndex
        +List~Lesson~ lessons
    }

    class Lesson {
        +String lessonId
        +String title
        +LessonType type
        +String contentMarkdown
        +String videoUrl
        +Integer durationMinutes
        +String exerciseRefId
    }

    class UserProgress {
        +String id
        +String userId
        +String courseId
        +Integer completionPercentage
        +Boolean isCompleted
        +DateTime lastAccessedAt
        +List~String~ completedLessonIds
        +markLessonCompleted(lessonId)
        +recalculatePercentage()
    }

    Course *-- Module : contient
    Module *-- Lesson : structure
    UserProgress ..> Course : suit l'avancement
```

### Description des Entités
- **`Course`** : Document racine du catalogue contenant les métadonnées globales du cours et la liste des modules.
- **`Module`** : Sous-ensemble thématique d'apprentissage regroupant un ensemble de leçons ordonnées.
- **`Lesson`** : Unité pédagogique minimale (cours théorique, vidéo explicative ou exercice pratique).
- **`UserProgress`** : Collection isolée persistant la liste des leçons terminées et le pourcentage de réussite de chaque apprenant.

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `auth-service` (Port 8081)
- **Endpoint appelé :** `GET http://localhost:8081/api/auth/users/:instructorId/summary`
- **Réponse reçue :** `{ "id": "usr_ins_42", "fullName": "Dr. Thomas Martin", "role": "ROLE_INSTRUCTOR", "isActive": true }`
- **Code de retour attendu :** `200 OK`
- **Justification technique :** Lors de la création d'un cours (`POST /api/courses`), le service doit obligatoirement valider en temps réel que le créateur est un instructeur actif et récupérer son nom d'affichage officiel sans dupliquer la table des utilisateurs.
- **Service également appelé en synchrone par d'autres microservices :**
  - Expose `GET /api/courses/:courseId/exercises/:exerciseId/test-cases` appelé par `code-execution-service` pour récupérer les jeux de tests unitaires avant exécution dans la sandbox.
  - Expose `GET /api/courses/:id` appelé par `gamification-service` pour obtenir le titre exact lors de l'émission du certificat.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `CourseCompletedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic)
- **Routing Key :** `course.completion.completed`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_crs_8821",
    "eventType": "COURSE_COMPLETED",
    "userId": "usr_9823f4a1",
    "courseId": "crs_java_adv_01",
    "courseTitle": "Java & Architecture Distribuée",
    "completionDate": "2026-11-09T14:30:00Z"
  }
  ```
- **Justification :** Informe de manière asynchrone le service de gamification pour déclencher automatiquement l'émission du certificat officiel de réussite.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événement consommé :** `CodeSolutionPassedEvent`
- **Queue écoutée :** `course.code_passed.queue`
- **Émetteur d'origine :** `code-execution-service`
- **Routing Key :** `code.solution.passed`
- **Action déclenchée :** À la réception de l'événement, le service extrait `userId` et `exerciseId`, recherche la leçon pratique correspondante et la marque comme validée (`markLessonCompleted`) dans le document `UserProgress`, puis recalcule le pourcentage de complétion du cours.

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/courses` | Liste paginée des cours avec filtres | `200 OK` | `400 Bad Request` |
| `GET` | `/api/courses/{id}` | Détail complet d'un cours et de son plan | `200 OK` | `404 Not Found` |
| `POST` | `/api/courses` | Création d'un cours (Instructeur) | `201 Created` | `403 Forbidden` |
| `PUT` | `/api/courses/{id}/progress` | Mise à jour de la progression | `200 OK` | `404 Not Found` |
| `GET` | `/api/courses/{id}/exercises/{eId}/test-cases` | Fournit les tests à la sandbox (interne) | `200 OK` | `404 Not Found` |

---

## ⚙️ 5. Configuration d'Environnement (`.env`)

```properties
PORT=8082
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_courses?authSource=admin
AUTH_SERVICE_URL=http://localhost:8081
RABBITMQ_URI=amqp://guest:guest@localhost:5672
```
