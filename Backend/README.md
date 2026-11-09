# Backend – Architecture des Microservices (SkillQuest)

Ce dossier regroupe l'ensemble des composants serveurs de la plateforme **SkillQuest**, découpés selon le patron d'architecture microservices (**Database-per-Service**).

---

## 📌 Vue d'Ensemble & Attribution des Ports

Pour éviter tout conflit lors du développement en local, chaque service écoute sur un port dédié :

| Dossier | Service | Rôle Principal | Responsable | Port Local | Base de Données |
| :--- | :--- | :--- | :--- | :---: | :--- |
| `api-gateway/` | **API Gateway** | Point d'entrée unique, routage, reverse-proxy & validation JWT | Toute l'équipe | `8080` | *Aucune (Stateless)* |
| `auth-service/` | **Auth & Identity** | Gestion des comptes, sécurité RBAC, émission des tokens JWT | *Service Commun* | `8081` | PostgreSQL |
| `course-service/` | **Course Content** | Catalogue pédagogique, modules, leçons et suivi d'avancement | Étudiant 2 | `8082` | MongoDB |
| `code-execution-service/` | **Code Execution** | Sandbox Docker isolée, évaluation du code et tests unitaires | Étudiant 3 | `8083` | Redis + PostgreSQL |
| `gamification-service/` | **Gamification** | Points d'expérience (XP), badges, classements et certificats | Étudiant 4 | `8084` | PostgreSQL |
| `job-board-service/` | **Job Board** | Offres d'emploi tech, matching compétences et candidatures | Étudiant 5 | `8085` | MongoDB |
| `community-service/` | **Community** | Forums d'entraide par exercice, votes et revues de code | Étudiant 6 | `8086` | MongoDB |
| `notification-service/` | **Notification** | Alertes en temps réel via WebSockets et e-mails transactionnels | Transverse | `8087` | Redis + MongoDB |

---

## ⚡ Matrice des Exigences Techniques AWD (Synchrones & Asynchrones)

Chaque microservice respecte strictement les 3 exigences techniques imposées :

| Microservice | Communication Synchrone REST (Au moins 1) | Événement Métier Publié (Au moins 1) | Événement Métier Consommé (Au moins 1) |
| :--- | :--- | :--- | :--- |
| **`auth-service`** | Appelle `gamification-service` (`POST /profiles`) & expose `GET /users/:id/summary` | `UserRegisteredEvent` | `UserReportedAbuseEvent` (émis par Community) |
| **`course-service`** | Appelle `auth-service` (`GET /users/:id/summary`) & expose `GET /test-cases` | `CourseCompletedEvent` | `CodeSolutionPassedEvent` (émis par Code Exec) |
| **`code-execution-service`** | Appelle `course-service` (`GET /test-cases`) pour charger les tests unitaires | `CodeSolutionPassedEvent` | `ExerciseCreatedEvent` (émis par Course) |
| **`gamification-service`** | Appelle `course-service` (`GET /courses/:id`) & expose `GET /certifications/:id` | `BadgeUnlockedEvent` | `CodeSolutionPassedEvent` & `CourseCompletedEvent` |
| **`job-board-service`** | Appelle `gamification-service` (`GET /certifications/:id`) pour valider les badges | `JobApplicationSubmittedEvent` | `BadgeUnlockedEvent` (émis par Gamification) |
| **`community-service`** | Appelle `auth-service` (`GET /users/:id/summary`) pour afficher l'auteur | `UserReportedAbuseEvent` | `CodeSolutionPassedEvent` (débloque Peer-Review) |
| **`notification-service`** | Expose `POST /direct-email` appelé par `auth-service` | `NotificationDispatchedEvent` | `CodeSolutionPassedEvent`, `BadgeUnlockedEvent`, `JobApplicationSubmittedEvent` |

---

## 📂 Détail de Chaque Partie (What each part is for)

### 1. `api-gateway/` – Passerelle d'Entrée & Sécurité
- **À quoi ça sert :** C'est la seule porte d'entrée exposée aux clients extérieurs (Frontend Web / Mobile).
- **Responsabilités :**
  - Réceptionner toutes les requêtes HTTP/HTTPS et les rediriger vers le microservice concerné.
  - Vérifier la validité des tokens JWT avant de transmettre la requête aux services internes.
  - Injecter les en-têtes d'identité sécurisés (`X-User-Id`, `X-User-Role`).
  - Gérer les règles de CORS et la limitation de débit (*Rate Limiting*).

---

### 2. `auth-service/` – Service Commun : Authentification & Identités
- **À quoi ça sert :** Gérer l'enregistrement et l'identification sécurisée de tous les profils de la plateforme.
- **Responsabilités :**
  - Inscription, connexion, hachage des mots de passe (BCrypt).
  - Gestion des rôles applicatifs : `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_RECRUITER`, `ROLE_ADMIN`.
  - Émission et renouvellement des jetons d'accès (`access_token` court) et de rafraîchissement (`refresh_token`).
- **Base de données :** **PostgreSQL** (intégrité relationnelle stricte, contrainte d'unicité sur les e-mails).

---

### 3. `course-service/` – Catalogue & Contenu Pédagogique (Étudiant 2)
- **À quoi ça sert :** Organiser le contenu éducatif et enregistrer la progression d'apprentissage de chaque élève.
- **Responsabilités :**
  - CRUD des cours, modules, chapitres et exercices théoriques.
  - Suivi en temps réel du pourcentage de complétion du cours par étudiant.
  - Sauvegarde de la dernière leçon consultée.
- **Base de données :** **MongoDB** (documents souples et hiérarchisés : cours $\rightarrow$ chapitres $\rightarrow$ leçons imbriquées).
- **Communication Asynchrone :** Consomme l'événement `CodeSolutionPassedEvent` émis par l'exécution de code pour marquer automatiquement un exercice comme validé.

---

### 4. `code-execution-service/` – Évaluation de Code & Sandbox (Étudiant 3)
- **À quoi ça sert :** Exécuter et tester le code soumis par les apprenants dans un environnement parfaitement isolé et sécurisé.
- **Responsabilités :**
  - Lancement éphémère d'un conteneur de sandbox (sans accès réseau, mémoire limitée à 128 Mo, timeout dur de 2 secondes).
  - Validation du code contre des jeux de tests unitaires (publics pour l'étudiant, cachés pour la notation finale).
  - Mesure des performances : temps d'exécution en millisecondes et mémoire vive utilisée.
- **Base de données :** **Redis** (file d'attente FIFO pour gérer les soumissions simultanées sans bloquer l'API) + **PostgreSQL** (historique des soumissions).
- **Communication Asynchrone :** Publie l'événement `CodeSolutionPassedEvent` sur RabbitMQ dès qu'une solution valide l'ensemble des tests.

---

### 5. `gamification-service/` – Points d'XP, Badges & Certifications (Étudiant 4)
- **À quoi ça sert :** Motiver les apprenants grâce à la gamification et certifier officiellement leur niveau technique.
- **Responsabilités :**
  - Attribution des points d'expérience (+50 XP par exercice validé) et gestion du passage de niveaux.
  - Déblocage automatique de badges thématiques (ex: *"Java Master"*, *"Clean Coder"*).
  - Génération de certificats de réussite au format PDF munis d'un hash cryptographique vérifiable en ligne.
  - Calcul et publication du classement général (*Leaderboard*).
- **Base de données :** **PostgreSQL** (garantie transactionnelle stricte pour éviter les fraudes aux points ou les duplications de badges).
- **Communication Synchrone :** Répond aux requêtes REST du Job Board (`GET /api/certifications/:userId`) pour attester du score d'un candidat.

---

### 6. `job-board-service/` – Recrutement Tech & Matching (Étudiant 5)
- **À quoi ça sert :** Connecter les entreprises en recherche de développeurs avec les apprenants ayant prouvé leurs compétences.
- **Responsabilités :**
  - Publication et gestion des offres d'emploi et de stage par les recruteurs.
  - Algorithme de matching automatique entre les exigences techniques d'une offre et les badges réels obtenus par les étudiants.
  - Suivi du cycle de vie des candidatures (reçue, présélectionnée, entretien, acceptée).
- **Base de données :** **MongoDB** (structure flexible permettant d'indexer facilement les offres selon des tags de langages et technologies hétérogènes).

---

### 7. `community-service/` – Forums d'Entraide & Peer-Review (Étudiant 6)
- **À quoi ça sert :** Favoriser l'apprentissage collaboratif et l'émulation entre développeurs.
- **Responsabilités :**
  - Forums de questions/réponses organisés par exercice et par cours.
  - Système de votes positifs/négatifs (*upvotes/downvotes*) pour faire remonter les explications les plus pertinentes.
  - Espace de revue de code (*Peer-Review*) permettant de partager et commenter des solutions optimisées une fois le problème résolu.
- **Base de données :** **MongoDB** (parfaitement adapté aux structures d'arbres de commentaires et discussions imbriquées).

---

### 8. `notification-service/` – Alertes WebSockets & E-mails (Support Transverse)
- **À quoi ça sert :** Transmettre instantanément les informations importantes aux utilisateurs connectés.
- **Responsabilités :**
  - Maintien des connexions persistantes par WebSockets (`ws://...`) pour afficher des alertes en direct dans l'interface (ex: *"Félicitations ! Vous venez de débloquer le badge Java Master"*).
  - Envoi d'e-mails transactionnels (confirmation de candidature, convocation par un recruteur, récapitulatif hebdomadaire).
- **Base de données :** **Redis** (gestion des sessions WebSockets actives) + **MongoDB** (historique des notifications).

---

## 🔄 Résumé des Flux entre Services

```text
[Client Web] ──HTTP──► [api-gateway] (8080)
                           │
       ┌───────────────────┼───────────────────┬───────────────────┐
       ▼                   ▼                   ▼                   ▼
  [auth-service]    [course-service]  [code-execution-svc]  [job-board-service]
     (8081)              (8082)              (8083)              (8085)
                                               │                   │
                                          émet │                   │ appelle en direct
                                               ▼                   ▼ (REST Synchrone)
                                          [RabbitMQ] ──────► [gamification-service]
                                               │                   (8084)
                                   consomment  ├─────────────┐
                                               ▼             ▼
                                      [course-service]  [notification-service]
                                                           (8087 - WebSockets)
```
