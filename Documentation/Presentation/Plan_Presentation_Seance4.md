# Plan de Présentation – Séance 4 (Validation Intermédiaire)
**Projet :** SkillQuest – Plateforme d'Apprentissage & Recrutement IT  
**Durée totale :** 10 à 15 minutes (+ 10 minutes d'échange avec l'enseignant)

---

## 🕒 Répartition du Temps

```text
[00:00 - 05:00]  PARTIE COMMUNE (Présentée par le groupe)
                 ├── Problématique, acteurs & fonctionnalités clés
                 ├── Architecture globale, API Gateway & sécurité JWT
                 ├── Matrice des microservices et choix de bases de données
                 └── Organisation Git, Trello/Jira & gestion des risques

[05:00 - 12:00]  PARTIE INDIVIDUELLE (1 min de speech par étudiant)
                 ├── Microservice Commun (Présenté par Membre 1)
                 ├── Microservice 1 : Course Content (Membre 2)
                 ├── Microservice 2 : Code Execution (Membre 3)
                 ├── Microservice 3 : Gamification (Membre 4)
                 ├── Microservice 4 : Job Board (Membre 5)
                 └── Microservice 5 : Community (Membre 6)

[12:00 - 14:00]  DÉMONSTRATION D'INFRASTRUCTURE / QUESTIONS
```

---

## 📑 Détail des Slides (10 à 12 Slides)

### Slide 1 : Titre & Équipe
- **Contenu :** Nom du projet (*SkillQuest*), classe, groupe, liste des 6 étudiants et leurs rôles.
- **Message clé :** Plateforme distribuée combinant formation interactive, évaluation de code en sandbox et recrutement certifié.

### Slide 2 : Problématique & Acteurs
- **Problématique :** Déconnexion entre les diplômes académiques et les compétences de code réelles exigées par les entreprises tech.
- **Acteurs :** Apprenant (codeur), Instructeur (créateur de cours), Recruteur (chasseur de talents), Administrateur.

### Slide 3 : Architecture Globale Proposée
- **Composants :** Client Web SPA $\rightarrow$ API Gateway $\rightarrow$ 6 Microservices indépendants + Message Broker (RabbitMQ).
- **Pattern :** Database-per-service (PostgreSQL, MongoDB, Redis).

### Slide 4 : Sécurité & Service Commun
- **Présentateur :** Membre 1
- **Focus :** Authentification centralisée, tokens JWT signés, gestion des rôles (RBAC).

---

### Slides 5 à 10 : Passages Individuels (1 minute / étudiant)

#### Slide 5 (Étudiant 2) : Microservice 1 – Course & Learning Content
- **Rôle :** Catalogue de cours, hiérarchie modulaire et sauvegarde de l'avancement.
- **Choix BD :** MongoDB (flexibilité des cours contenant chapitres et médias).
- **Contrats d'API :** `GET /api/courses`, `PUT /api/courses/{id}/progress`.
- **Interaction :** Consomme `CodePassedEvent` pour valider les étapes d'un cours.

#### Slide 6 (Étudiant 3) : Microservice 2 – Code Execution & Sandbox
- **Rôle :** Évaluation isolée du code sans accès réseau avec quotas CPU/RAM.
- **Choix BD :** Redis (file de jobs) + PostgreSQL (historique d'exécution).
- **Contrats d'API :** `POST /api/submissions/run`, `POST /api/submissions/submit`.
- **Interaction asynchrone :** Émet `CodeSolutionPassedEvent` sur RabbitMQ.

#### Slide 7 (Étudiant 4) : Microservice 3 – Gamification & Certification
- **Rôle :** Moteur d'XP, badges débloqués, classements et certificats PDF infalsifiables.
- **Choix BD :** PostgreSQL (intégrité transactionnelle des points et badges).
- **Contrats d'API :** `GET /api/gamification/leaderboard`, `GET /api/certifications/{id}/verify`.
- **Interaction synchrone :** Répond en REST au Job Board pour attester des compétences d'un candidat.

#### Slide 8 (Étudiant 5) : Microservice 4 – Job Board & Recruitment
- **Rôle :** Publication d'offres tech, matching automatique compétences-profils.
- **Choix BD :** MongoDB (indexation des tags techniques et compétences requises).
- **Contrats d'API :** `POST /api/jobs`, `GET /api/jobs/matched`.
- **Interaction :** Interroge le service Gamification pour vérifier les prérequis d'un candidat.

#### Slide 9 (Étudiant 6) : Microservice 5 – Community & Peer-Review
- **Rôle :** Forums d'entraide par exercice, upvotes de réponses et revue de code.
- **Choix BD :** MongoDB (gestion des threads imbriqués de discussions).
- **Contrats d'API :** `GET /api/community/threads`, `POST /api/community/posts/{id}/vote`.

#### Slide 10 : Microservice Transverse – Notifications & WebSockets
- **Rôle :** Alertes en direct (WebSocket) lors de la validation d'un test ou d'un badge.
- **Choix BD :** Redis (gestion des sessions temps réel).

---

### Slide 11 : Organisation du Projet & Planning
- **Git Flow :** Branche `main` protégée, branches `feature/...` par microservice.
- **Suivi :** Tableau Kanban (Trello / GitHub Projects).
- **Planning :** 
  - Séance 4 : Validation de la conception
  - Semaines 5-6 : Implémentation unitaire des services
  - Semaines 7-8 : Gateway & intégration RabbitMQ
  - Semaine 9 : Tests & Docker final

### Slide 12 : Analyse des Risques & Mesures
- Risque d'exécution de code malveillant $\rightarrow$ Conteneurisation isolée sans réseau.
- Risque de désynchronisation des données $\rightarrow$ Événements idempotents RabbitMQ.
