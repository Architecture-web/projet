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

> **En résumé :**
> - **Synchrone (REST) :** Appelle `gamification-service` pour vérifier les vrais badges d'un élève qui postule.
> - **Asynchrone (Publie) :** Émet `JobApplicationSubmittedEvent` pour avertir le recruteur qu'un candidat a postulé.
> - **Asynchrone (Consomme) :** Écoute `BadgeUnlockedEvent` pour recalculer les offres d'emploi compatibles avec l'élève.

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel émis vers :** `gamification-service` (Port 8084)
- **Endpoint appelé :** `GET http://localhost:8084/api/certifications/:candidateId`
- **Explication simple :** Quand un candidat postule à une offre, le service demande en direct à `gamification-service` son total de points et ses badges certifiés pour prouver son niveau au recruteur.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `JobApplicationSubmittedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic `job.application.submitted`)
- **Explication simple :** Dès qu'un candidat dépose son dossier, le service publie cet événement pour que le recruteur reçoive immédiatement un e-mail et une notification avec le score de compatibilité.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événement consommé :** `BadgeUnlockedEvent` (émis par `gamification-service`)
- **Queue écoutée :** `job_board.badge_unlocked.queue`
- **Explication simple :** Dès qu'un élève débloque un nouveau badge, le service recalcule automatiquement les offres d'emploi qui correspondent à son nouveau profil.

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
