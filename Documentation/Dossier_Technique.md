# Dossier Technique – Projet SkillQuest
## Validation Intermédiaire (Séance 4) – Architecture Web Distribuée (AWD)

**Projet :** SkillQuest – Plateforme d'Apprentissage & Recrutement IT  
**Équipe :** 6 membres  
**Dépôt Git :** [Lien du dépôt public]  
**Collaborateur enseignant :** [Identifiant de l'enseignant ajouté au dépôt]

---

## 1. Description du Sujet & Problématique

### 1.1 Contexte et Objectifs
Dans l'industrie informatique actuelle, le recrutement traditionnel basé sur les seuls CV ou diplômes montre ses limites. Les entreprises recherchent des développeurs capables de résoudre des problèmes algorithmiques et d'écrire un code propre, performant et testé. D'un autre côté, les étudiants et autodidactes manquent souvent d'outils certifiants pour prouver concrètement leur niveau technique.

**SkillQuest** a pour objectif de combler ce fossé en créant une plateforme distribuée qui unifie :
1. **L'apprentissage modulaire** par cours théoriques et pratiques (*style Coursera/OpenClassrooms*).
2. **L'évaluation par la pratique** via un bac à sable d'exécution de code sécurisé (*style LeetCode/CodeChef*).
3. **Le recrutement technique certifié** permettant aux entreprises de recruter directement des talents sur la base de scores de compétences vérifiés.

### 1.2 Acteurs du Système
- **Apprenant / Candidat :** Développeur souhaitant se former, résoudre des exercices de code en ligne, valider des certifications et postuler aux offres de stage ou d'emploi.
- **Instructeur / Formateur :** Professionnel concevant des cours, structurant des modules pédagogiques et configurant des jeux d'essais (tests unitaires publics et secrets).
- **Recruteur Tech :** Entreprise publiant des offres d'emploi, filtrant les candidats par critères de compétences validées (XP, badges, taux de réussite) et proposant des entretiens.
- **Administrateur :** Modérateur de contenu garantissant la qualité pédagogique des cours, surveillant la sécurité de la plateforme et supervisant les quotas de ressources machine alloués à la sandbox.

### 1.3 Cas d'Utilisation Généraux
- **Cas 1 : Inscription et suivi de formation :** L'apprenant sélectionne un cours (ex: "Algorithmes avancés en Java"), suit les modules et sauvegarde sa progression.
- **Cas 2 : Résolution d'un défi algorithmique :** L'apprenant écrit son code dans l'éditeur, lance les tests publics, puis soumet sa solution pour validation globale.
- **Cas 3 : Attribution de badge et certification :** À la réussite d'un palier, le système attribue des points d'XP, met à jour le classement et émet un certificat numérique vérifiable.
- **Cas 4 : Chasse de profils par un recruteur :** Un recruteur configure une recherche ("Développeur Backend > 1000 XP et Badge Java Master") et invite les meilleurs profils.

---

## 2. Architecture Globale Proposée

### 2.1 Schéma d'Architecture Globale

```text
                                  ┌───────────────────────────┐
                                  │    Client Web (SPA)       │
                                  │   (React.js / Tailwind)   │
                                  └─────────────┬─────────────┘
                                                │ HTTPS / WebSocket
                                                ▼
                                  ┌───────────────────────────┐
                                  │        API GATEWAY        │
                                  │ (Spring Cloud GW / Kong)  │
                                  │  - Routage & Reverse-Proxy│
                                  │  - Validation JWT (Auth)  │
                                  │  - Rate Limiting & CORS   │
                                  └─────────────┬─────────────┘
                                                │
         ┌──────────────────┬───────────────────┼───────────────────┬──────────────────┐
         │                  │                   │                   │                  │
         ▼                  ▼                   ▼                   ▼                  ▼
  ┌─────────────┐    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐    ┌─────────────┐
  │   Service   │    │ Microservice│     │ Microservice│     │ Microservice│    │ Microservice│
  │   Commun    │    │ 1 (Courses) │     │2 (Execution)│     │ 3 (Gamif)   │    │  4 (Jobs)   │
  │ (Auth / JWT)│    └──────┬──────┘     └──────┬──────┘     └──────┬──────┘    └──────┬──────┘
  └──────┬──────┘           │                   │                   │                  │
         │                  │                   │                   │                  │
         ▼                  ▼                   ▼                   ▼                  ▼
    PostgreSQL           MongoDB          Redis + Postgres      PostgreSQL          MongoDB
         │                  │                   │                   │                  │
         └──────────────────┴─────────┬─────────┴───────────────────┴──────────────────┘
                                      │
                                      ▼
                        ┌───────────────────────────┐
                        │      MESSAGE BROKER       │
                        │    (RabbitMQ / Kafka)     │
                        │  Événements asynchrones   │
                        └─────────────┬─────────────┘
                                      │
                     ┌────────────────┴────────────────┐
                     ▼                                 ▼
             ┌──────────────┐                  ┌──────────────┐
             │ Microservice │                  │ Microservice │
             │ 5 (Community)│                  │6(Notification│
             └──────┬───────┘                  └──────┬───────┘
                    │                                 │
                    ▼                                 ▼
                 MongoDB                        Redis / MongoDB
```

### 2.2 Composants d'Infrastructure
- **API Gateway :** Point d'entrée unique exposant le port `8080`, dispatche les requêtes vers les services internes sur réseau privé Docker.
- **Service Discovery (Eureka / Consul) :** Découverte dynamique des instances de services.
- **Message Broker (RabbitMQ) :** Gestion des files d'événements pour découpler les traitements lourds (notifications, gamification).
- **Sécurité :** Authentification centralisée basée sur des tokens JWT signés (HMAC SHA-256 ou RSA). L'API Gateway extrait le token, vérifie sa signature et injecte les en-têtes d'identité (`X-User-Id`, `X-User-Role`) vers les microservices.

---

## 3. Découpage en Microservices & Données Gérées

### 3.1 Service Commun : Auth & Identity Service
- **Responsables :** Toute l'équipe (présenté par un membre désigné)
- **Rôle :** Enregistrement des utilisateurs, connexion, hachage sécurisé des mots de passe (BCrypt), gestion des rôles (`STUDENT`, `INSTRUCTOR`, `RECRUITER`, `ADMIN`), émission et renouvellement des tokens JWT.
- **Base de données :** **PostgreSQL** (garantie ACID, unicité des e-mails, relations strictes).
- **Entités principales :** `User`, `Role`, `UserRole`, `RefreshToken`, `AuditLog`.

### 3.2 Microservice 1 : Course & Learning Content Service
- **Responsable :** Étudiant 1
- **Rôle :** Catalogue des formations, découpage modulaire (cours, modules, leçons en vidéo/markdown), gestion des quiz QCM et état d'avancement des apprenants.
- **Base de données :** **MongoDB** (structure arborescente flexible, manipulation naturelle des cours contenant des listes de leçons et de ressources).
- **Entités principales :** `Course`, `Module`, `Lesson`, `Quiz`, `UserEnrollment`, `ProgressState`.

### 3.3 Microservice 2 : Code Execution & Sandbox Service
- **Responsable :** Étudiant 2
- **Rôle :** Réception du code source soumis (Java, Python, JS), exécution sécurisée et isolée (Sandbox Docker sans réseau, CPU/mémoire bridés), comparaison de la sortie avec les jeux de tests (publics et cachés), mesure des performances (temps d'exécution et mémoire consommée).
- **Base de données :** **Redis** (file d'attente FIFO des soumissions à évaluer) + **PostgreSQL** (historique des soumissions, statistiques d'exécution).
- **Entités principales :** `Submission`, `ExerciseTestCase`, `ExecutionMetrics`, `LanguageRuntime`.

### 3.4 Microservice 3 : Gamification & Certification Service
- **Responsable :** Étudiant 3
- **Rôle :** Attribution d'XP selon la difficulté des exercices résolus, gestion des paliers de niveaux, déblocage de badges et génération de certificats PDF avec identifiant hashé vérifiable publiquement.
- **Base de données :** **PostgreSQL** (comptabilité précise des points, intégrité référentielle des badges attribués et unicité des certificats).
- **Entités principales :** `UserXP`, `Badge`, `UserBadge`, `Certificate`, `LeaderboardSnapshot`.

### 3.5 Microservice 4 : Job Board & Tech Recruitment Service
- **Responsable :** Étudiant 4
- **Rôle :** Publication et gestion des offres d'emploi techniques par les entreprises, algorithme de matching profil-offre basé sur les compétences validées, gestion du statut des candidatures.
- **Base de données :** **MongoDB** (recherche par mots-clés et tags de compétences, indexation géospatiale des offres).
- **Entités principales :** `CompanyProfile`, `JobPosting`, `SkillRequirement`, `JobApplication`.

### 3.6 Microservice 5 : Community & Peer-Review Service
- **Responsable :** Étudiant 5
- **Rôle :** Espaces d'entraide et forums associés à chaque exercice, système de votes d'utilité (*upvotes/downvotes*), partage de solutions après résolution du défi et revue de code entre pairs.
- **Base de données :** **MongoDB** (stockage des arborescences de commentaires et réponses imbriquées).
- **Entités principales :** `ForumThread`, `Comment`, `Vote`, `CodeReviewSolution`.

### 3.7 Microservice 6 : Notification & Alert Service
- **Responsable :** Étudiant 6
- **Rôle :** Réception des événements métier, envoi de notifications instantanées via WebSocket aux utilisateurs connectés et envoi d'e-mails transactionnels asynchrones (convocations d'entretien, récapitulatifs d'XP).
- **Base de données :** **Redis** (sessions WebSockets actives) + **MongoDB** (historique des notifications des utilisateurs).
- **Entités principales :** `Notification`, `NotificationPreference`, `DeliveryStatus`.

---

## 4. Interactions & Communications Inter-Services

### 4.1 Exemple d'interaction synchrone (REST)
- **Scénario :** Un recruteur consulte la candidature d'un étudiant pour un poste de développeur Java.
- **Flux :** Le `Job Board Service` envoie une requête GET synchrone à `Gamification & Certification Service` (`GET /api/certifications/user/102`) afin d'obtenir la liste vérifiée des badges et le rang de l'étudiant en temps réel.

### 4.2 Exemple d'interaction asynchrone (Event-Driven via RabbitMQ)
- **Scénario :** L'étudiant résout avec succès un exercice d'algorithmique complexe.
- **Flux :**
  1. `Code Execution Service` publie l'événement `CodeSolutionPassedEvent` sur le topic exchange `skillquest.code` :
     ```json
     {
       "eventType": "CODE_PASSED",
       "userId": "usr_981",
       "exerciseId": "ex_42",
       "language": "JAVA",
       "executionTimeMs": 85,
       "timestamp": "2026-11-08T10:00:00Z"
     }
     ```
  2. Le `Gamification Service` consomme le message, ajoute 50 points d'XP et débloque le badge *"Java Master"*.
  3. Le `Course Content Service` consomme le message et met à jour la progression du module à 100%.
  4. Le `Notification Service` consomme le message et diffuse une notification WebSocket en direct sur l'écran de l'étudiant : *"Félicitations ! Vous venez de débloquer le badge Java Master !"*.

---

## 5. Contrats d'API & Principaux Endpoints

### Service Commun : Auth & Identity
| Méthode | Endpoint | Description | Code Succès | Erreurs |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Inscription d'un utilisateur | 201 Created | 400, 409 |
| `POST` | `/api/auth/login` | Connexion et délivrance de token JWT | 200 OK | 401 |
| `POST` | `/api/auth/refresh` | Renouvellement du token expiré | 200 OK | 403 |

### Microservice 1 : Course Content
| Méthode | Endpoint | Description | Code Succès |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/courses` | Liste paginée des cours avec filtres | 200 OK |
| `GET` | `/api/courses/{id}` | Détails d'un cours et liste des modules | 200 OK |
| `PUT` | `/api/courses/{id}/progress` | Sauvegarde de l'avancement d'un élève | 200 OK |

### Microservice 2 : Code Execution
| Méthode | Endpoint | Description | Code Succès |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/submissions/run` | Test rapide du code avec jeux d'essais publics | 200 OK |
| `POST` | `/api/submissions/submit` | Soumission définitive et évaluation sandbox complète | 202 Accepted |
| `GET` | `/api/submissions/{id}` | Récupération du résultat et métriques du job | 200 OK |

### Microservice 3 : Gamification & Certification
| Méthode | Endpoint | Description | Code Succès |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/gamification/leaderboard` | Top 50 des développeurs par points d'XP | 200 OK |
| `GET` | `/api/certifications/{id}/verify`| Vérification publique d'un certificat | 200 OK / 404 |

### Microservice 4 : Job Board
| Méthode | Endpoint | Description | Code Succès |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/jobs` | Publication d'une offre d'emploi par un recruteur | 201 Created |
| `GET` | `/api/jobs/matched` | Liste des offres correspondant au profil du candidat | 200 OK |
| `POST` | `/api/jobs/{id}/apply` | Postuler à une offre avec profil certifié | 201 Created |

### Microservice 5 : Community
| Méthode | Endpoint | Description | Code Succès |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/community/threads?courseId={id}` | Liste des discussions d'un cours | 200 OK |
| `POST` | `/api/community/threads` | Création d'une nouvelle question | 201 Created |
| `POST` | `/api/community/posts/{id}/vote` | Voter pour une réponse utile | 200 OK |

### Microservice 6 : Notification
| Méthode | Endpoint | Description | Code Succès |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/notifications` | Historique des alertes de l'utilisateur | 200 OK |
| `PUT` | `/api/notifications/{id}/read` | Marquer une notification comme lue | 200 OK |
| `WS` | `/ws/notifications` | Connexion persistante WebSocket pour alertes live | 101 Switching |

---

## 6. Planning Prévisionnel & Gestion des Risques

### 6.1 Planning de Réalisation

| Étape | Période | Livrables / Objectifs |
| :--- | :--- | :--- |
| **Étape 1 (Séance 4)** | Semaine courante | Validation intermédiaire : Architecture globale, découpage microservices, modèles de données et contrats d'API. |
| **Étape 2** | Semaines 5 - 6 | Implémentation du service commun d'authentification et développement unitaire de chaque microservice. |
| **Étape 3** | Semaines 7 - 8 | Mise en place de l'API Gateway, configuration de RabbitMQ pour les événements asynchrones et intégration du Frontend. |
| **Étape 4** | Semaine 9 | Tests d'intégration de bout en bout, conteneurisation Docker-Compose finale et répétition de la soutenance finale. |

### 6.2 Analyse des Risques et Solutions d'Atténuation

| Risque Identifié | Impact | Probabilité | Solution Préventive / Mesure d'Atténuation |
| :--- | :---: | :---: | :--- |
| **Sécurité de la Sandbox d'exécution de code** (scripts malveillants, boucle infinie) | Élevé | Moyen | Exécution dans des conteneurs éphémères Docker sans droits root, sans accès réseau et avec limites dures de CPU/RAM (`cgroups`) et timeout de 2 secondes. |
| **Incohérence des données entre microservices** | Élevé | Faible | Définition de contrats d'API stricts (OpenAPI/Swagger) et utilisation de messages idempotents dans RabbitMQ. |
| **Goulot d'étranglement sur l'évaluation de code lors de pics de soumissions** | Moyen | Moyen | File d'attente asynchrone Redis/RabbitMQ permettant de traiter les soumissions sans bloquer les requêtes HTTP. |
| **Retard d'intégration du Frontend** | Moyen | Faible | Spécification claire des contrats d'API dès la séance 4 pour permettre aux développeurs frontend de travailler avec des mocks (Postman / MirageJS). |
