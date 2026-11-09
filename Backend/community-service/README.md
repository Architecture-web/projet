# Community & Peer-Review Service (Microservice 5 – Étudiant 6)

## 📌 1. Présentation du Service
Ce microservice encourage l'entraide collective et l'apprentissage par les pairs à travers des forums contextuels par exercice et un module exclusif de revue de code (*Peer-Review*).

- **Port local :** `8086`
- **Responsable :** Étudiant 6
- **Base de données :** **MongoDB 6.0** (`skillquest_community`)
- **Justification de la base :** Les fils de discussion hiérarchiques, commentaires imbriqués (réponses à des réponses) et votes d'utilité s'organisent naturellement sous forme de documents JSON arborescents.

---

## 📐 2. Conception & Diagramme de Classes

```mermaid
classDiagram
    class ForumThread {
        +String id
        +String exerciseId
        +String authorId
        +String authorName
        +String title
        +String contentMarkdown
        +Integer upvotesCount
        +Integer repliesCount
        +DateTime createdAt
        +upvote()
    }

    class ThreadComment {
        +String id
        +String threadId
        +String parentCommentId
        +String authorId
        +String contentMarkdown
        +Integer score
        +DateTime createdAt
        +addScore(delta)
    }

    class PeerReviewSolution {
        +String id
        +String exerciseId
        +String authorId
        +String codeSnippet
        +String timeComplexity
        +String spaceComplexity
        +String explanation
        +Integer likesCount
        +DateTime sharedAt
    }

    class AbuseReport {
        +String id
        +String targetUserId
        +String reporterUserId
        +String reason
        +ReportStatus status
        +DateTime reportedAt
    }

    ForumThread "1" *-- "*" ThreadComment : regroupe
    ThreadComment "1" *-- "*" ThreadComment : réponses imbriquées
    ForumThread ..> AbuseReport : peut faire l'objet de
```

### Description des Entités
- **`ForumThread`** : Fil principal de discussion attaché à un exercice particulier.
- **`ThreadComment`** : Commentaire ou réponse technique rédigée par un autre apprenant.
- **`PeerReviewSolution`** : Solution optimisée partagée après réussite du défi pour analyse collective de la complexité algorithmique.
- **`AbuseReport`** : Signalement d'un comportement déplacé ou d'un partage prématuré de solution (spoil).

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `auth-service` (Port 8081)
- **Endpoint appelé :** `GET http://localhost:8081/api/auth/users/:authorId/summary`
- **Réponse reçue :** `{ "id": "usr_9823f4a1", "fullName": "Sarah Benali", "avatarUrl": "https://..." }`
- **Code de retour attendu :** `200 OK`
- **Justification technique :** Lors de l'affichage d'un fil de discussion ou d'une revue de code, le service doit afficher le nom et l'avatar de l'auteur en temps réel sans jamais dupliquer de table de profils dans sa base MongoDB.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événements publiés :**
  1. `UserReportedAbuseEvent`
     - **Exchange :** `skillquest.events` (Topic)
     - **Routing Key :** `community.user.reported`
     - **Payload :** `{ "targetUserId": "usr_bad_actor_9", "reason": "Spam & insultes", "reporterId": "usr_102" }`
     - **Justification :** Permet à `auth-service` de suspendre automatiquement un utilisateur malveillant.
  2. `HelpfulCommentVotedEvent` (Routing Key: `community.comment.upvoted`)
     - Informe `notification-service` pour avertir l'auteur que sa réponse a reçu un upvote (+1).

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événement consommé :** `CodeSolutionPassedEvent`
- **Queue écoutée :** `community.code_passed.queue`
- **Émetteur d'origine :** `code-execution-service`
- **Routing Key :** `code.solution.passed`
- **Action déclenchée :** *Règle métier majeure !* Tant qu'un élève n'a pas validé un exercice de code, il lui est strictement interdit d'accéder aux solutions de la communauté (*Peer-Review*). À la réception de cet événement pour un couple `(userId, exerciseId)`, le service débloque les droits de lecture et de partage de solutions pour cet élève sur cet exercice précis.

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `GET` | `/api/community/threads?exerciseId={id}` | Liste des questions d'un exercice | `200 OK` | `400 Bad Request` |
| `POST` | `/api/community/threads` | Création d'une nouvelle question | `201 Created` | `401 Unauthorized` |
| `POST` | `/api/community/posts/{id}/vote` | Voter (+1 / -1) pour une explication | `200 OK` | `404 Not Found` |
| `GET` | `/api/community/exercises/{id}/peer-reviews` | Accès aux solutions d'autres élèves (si validé) | `200 OK` | `403 Forbidden` |
| `POST` | `/api/community/reports` | Signalement d'un abus sur le forum | `201 Created` | `400 Bad Request` |

---

## ⚙️ 5. Configuration d'Environnement (`.env`)

```properties
PORT=8086
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_community?authSource=admin
AUTH_SERVICE_URL=http://localhost:8081
RABBITMQ_URI=amqp://guest:guest@localhost:5672
```
