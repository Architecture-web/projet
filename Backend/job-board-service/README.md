# Job Board & Tech Recruitment Service (Microservice 4 – Étudiant 5)

## 📌 1. Présentation du Service
Ce microservice relie les entreprises cherchant des profils techniques aux développeurs de SkillQuest. Il met en place un algorithme de matching basé sur les compétences réelles validées sur la plateforme.

- **Port local :** `8085`
- **Responsable :** Étudiant 5
- **Base de données :** **MongoDB 6.0** (`skillquest_jobs`)
- **Justification de la base :** Les offres de recrutement comportent des structures variables (tags de compétences disparates, contrats variés, descriptions riches) qui se prêtent idéalement à un modèle de documents NoSQL avec indexation textuelle et géographique.

---

## 📐 2. Conception & Diagramme de Classes

```mermaid
classDiagram
    class JobPosting {
        +String id
        +String recruiterId
        +String companyName
        +String jobTitle
        +String location
        +ContractType contractType
        +List~String~ requiredSkills
        +Integer minimumRequiredXP
        +Boolean isOpen
        +DateTime postedAt
        +closeJob()
    }

    class JobApplication {
        +String id
        +String jobPostingId
        +String candidateId
        +ApplicationStatus status
        +Integer verifiedCandidateXP
        +List~String~ verifiedBadges
        +Integer matchingScore
        +DateTime appliedAt
        +updateStatus(newStatus)
    }

    class CandidateMatchingProfile {
        +String candidateId
        +Integer totalXP
        +List~String~ verifiedSkillTags
        +calculateMatch(jobPosting) Integer
    }

    JobPosting "1" --> "*" JobApplication : reçoit
    JobApplication ..> CandidateMatchingProfile : évalué avec
```

### Description des Entités
- **`JobPosting`** : Annonce d'emploi ou de stage publiée par une entreprise avec ses critères d'éligibilité (XP minimum, tags demandés).
- **`JobApplication`** : Dossier de postulation d'un apprenant avec horodatage, score de matching calculé et statut de suivi (UNDER_REVIEW, INTERVIEW, ACCEPTED).
- **`CandidateMatchingProfile`** : Objet valeur représentant le snapshot des compétences validées lors de la postulation.

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `gamification-service` (Port 8084)
- **Endpoint appelé :** `GET http://localhost:8084/api/certifications/:candidateId`
- **Réponse reçue :**
  ```json
  {
    "userId": "usr_9823f4a1",
    "totalXP": 3250,
    "level": "Senior Learner",
    "verifiedBadges": [
      { "id": "bdg_algo_master", "title": "Maître Algorithmique" },
      { "id": "bdg_spring_pro", "title": "Expert Spring Boot" }
    ]
  }
  ```
- **Code de retour attendu :** `200 OK`
- **Justification technique :** Lorsqu'un candidat soumet sa candidature (`POST /api/jobs/:id/apply`), le Job Board doit impérativement certifier son niveau en direct. Un appel REST synchrone garantit que le recruteur ne reçoit que des données authentifiées à la source sans stocker de doublons non synchronisés.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `JobApplicationSubmittedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic)
- **Routing Key :** `job.application.submitted`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_job_5541",
    "eventType": "JOB_APPLICATION_SUBMITTED",
    "applicationId": "app_55419",
    "jobId": "job_94812",
    "candidateId": "usr_9823f4a1",
    "recruiterId": "usr_rec_7712",
    "matchingScore": 94,
    "timestamp": "2026-11-09T14:40:00Z"
  }
  ```
- **Justification :** Informe `notification-service` pour alerter instantanément le recruteur par e-mail et notification dans son espace entreprise d'un profil correspondant à 94% à son offre.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événement consommé :** `BadgeUnlockedEvent`
- **Queue écoutée :** `job_board.badge_unlocked.queue`
- **Émetteur d'origine :** `gamification-service`
- **Routing Key :** `gamification.badge.unlocked`
- **Action déclenchée :** À la réception du nouvel insigne obtenu par l'élève, le Job Board recalcule en tâche de fond les scores de matching des offres ouvertes pour ce profil et met à jour les offres recommandées sur le tableau de bord de l'étudiant.

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/jobs` | Dépôt d'une offre (Recruteur) | `201 Created` | `403 Forbidden` |
| `GET` | `/api/jobs` | Recherche paginée des offres tech | `200 OK` | `400 Bad Request` |
| `GET` | `/api/jobs/matched` | Offres recommandées selon les badges de l'élève | `200 OK` | `401 Unauthorized` |
| `POST` | `/api/jobs/{id}/apply` | Postuler avec vérification synchrone du profil | `201 Created` | `404 Not Found`, `409 Conflict` |

---

## ⚙️ 5. Configuration d'Environnement (`.env`)

```properties
PORT=8085
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_jobs?authSource=admin
GAMIFICATION_SERVICE_URL=http://localhost:8084
RABBITMQ_URI=amqp://guest:guest@localhost:5672
```
