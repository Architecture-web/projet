# SkillQuest – Soutenance Séance 4
## Architecture Web Distribuée (AWD) – Validation Intermédiaire

> **Classe :** [Votre Classe, ex: 4DAS16] | **Groupe :** [Votre Groupe, ex: G3]  
> **Dépôt Git :** https://github.com/Architecture-web/projet  
> **Durée estimée :** 12 à 14 minutes

---

## 👥 Sommaire & Équipe

1. **Partie Collective (5 min)**
   - Problématique & Idée générale
   - Acteurs & Cas d'utilisation
   - Architecture globale & API Gateway
   - Choix technologiques & Bases de données
   - Organisation Git & Planning prévisionnel
2. **Partie Individuelle (6 x 1 min)**
   - Service Commun : Auth & Identity
   - Microservice 1 : Course Content
   - Microservice 2 : Code Execution & Sandbox
   - Microservice 3 : Gamification & Certification
   - Microservice 4 : Job Board & Matching
   - Microservice 5 : Community & Peer-Review
3. **Conclusion & Échanges (10 min)**

---

## 🎯 Slide 1 : Problématique & Idée Générale

### Constat
- Les CV traditionnels ne reflètent pas les compétences réelles de code.
- Les étudiants ont besoin d'exercices pratiques pour valider leurs acquis avec des preuves concrètes.
- Les recruteurs tech recherchent des profils certifiés par la pratique.

### Solution : SkillQuest
Une plateforme web distribuée alliant :
1. **L'apprentissage modulaire** (*style Coursera*).
2. **L'évaluation de code en temps réel** (*style LeetCode*) dans une sandbox isolée.
3. **Le recrutement certifié** par les compétences validées.

---

## 👤 Slide 2 : Acteurs & Cas d'Utilisation

| Acteur | Besoins & Actions principales |
| :--- | :--- |
| **Apprenant** | Suivre des cours, soumettre du code, débloquer des badges d'XP, postuler. |
| **Instructeur** | Créer des modules, rédiger des exercices et configurer les tests unitaires. |
| **Recruteur** | Publier des offres tech, filtrer les candidats par compétences certifiées. |
| **Administrateur** | Modérer le contenu, surveiller les quotas de ressources de la sandbox. |

---

## 🏛️ Slide 3 : Architecture Globale

```text
Client Web (SPA)
       │
       ▼ (HTTPS / WSS)
┌────────────────────────────────────────────────────────┐
│               API GATEWAY (Port 8080)                  │
│       Routage • Validation JWT • Rate Limiting         │
└────────────────────────────────────────────────────────┘
       │
       ├──────────────┬──────────────┬──────────────┬──────────────┐
       ▼              ▼              ▼              ▼              ▼
┌─────────────┐┌─────────────┐┌─────────────┐┌─────────────┐┌─────────────┐
│ Auth (Comm) ││ 1. Courses  ││ 2. CodeExec ││ 3. Gamif    ││ 4. Jobs     │
│ PostgreSQL  ││ MongoDB     ││ Redis+PgSQL ││ PostgreSQL  ││ MongoDB     │
└─────────────┘└─────────────┘└──────┬──────┘└─────────────┘└─────────────┘
                                     │
                             Émet CodePassedEvent
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │    Message Broker (RabbitMQ)    │
                    └────────┬───────────────┬────────┘
                             │               │
                             ▼               ▼
                      ┌─────────────┐ ┌─────────────┐
                      │ 5. Community│ │ 6. Notif WS │
                      │ MongoDB     │ │ Redis + WS  │
                      └─────────────┘ └─────────────┘
```

---

## 💾 Slide 4 : Stratégie des Données & Pattern Database-per-Service

- **Principe fondamental :** Aucun microservice n'accède directement à la base d'un autre.
- **Approche Polyglotte :**
  - **PostgreSQL** : Données transactionnelles relationnelles (Identités, Droits RBAC, Points d'XP, Certificats).
  - **MongoDB** : Données documentaires flexibles (Arborescence des cours, Offres d'emploi, Threads de discussion).
  - **Redis** : Données volatiles et temps réel (Files d'attente d'évaluation de code, sessions WebSockets).

---

## 🔄 Slide 5 : Communications Synchrones vs Asynchrones

### 1. Communication Synchrone (HTTP REST)
- Utilisée pour les lectures directes sans persistance intermédiaire.
- *Exemple :* Le `Job Board Service` interroge en direct le `Gamification Service` (`GET /api/certifications/:userId`) pour vérifier les badges d'un candidat avant transmission de son dossier.

### 2. Communication Asynchrone (Event-Driven avec RabbitMQ)
- Utilisée pour découpler les traitements lourds et les effets de bord.
- *Exemple :* Le `Code Execution Service` publie `CodeSolutionPassedEvent`.
  - Le `Gamification Service` incrémente les XP et attribue les badges.
  - Le `Course Service` met à jour la progression du cours.
  - Le `Notification Service` envoie une alerte WebSocket au navigateur.

---

## 🛡️ Slide 6 : Sécurité & Gestion des Accès

- **Authentification centralisée :** Tokens JWT signés émis par le service d'authentification.
- **Rôle de l'API Gateway :**
  - Intercepte l'en-tête `Authorization: Bearer <token>`.
  - Vérifie la signature et la validité temporelle du token.
  - Propage les informations d'identité via les headers internes `X-User-Id` et `X-User-Role`.
- **Rôles applicatifs (RBAC) :** `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_RECRUITER`, `ROLE_ADMIN`.

---

## 📅 Slide 7 : Organisation Git, Outils & Planning

- **Gestion du code Git :**
  - Branche `main` protégée (validations stables).
  - Branches par microservice : `feature/auth`, `feature/courses`, etc.
  - Dépôt public avec enseignant ajouté comme collaborateur.
- **Outils de gestion :** GitHub Projects / Trello (méthode Kanban).
- **Planning prévisionnel :**
  - *Séance 4 :* Validation intermédiaire (architecture, modèles et contrats d'API).
  - *Semaines 5-6 :* Développement unitaire des microservices.
  - *Semaines 7-8 :* Intégration Gateway, RabbitMQ et application cliente.
  - *Semaine 9 :* Tests de charge et conteneurisation finale.

---

## ⚠️ Slide 8 : Gestion des Risques

| Risque | Impact | Solution mise en place |
| :--- | :---: | :--- |
| **Exécution de code dangereux** | Élevé | Sandbox Docker éphémère sans accès réseau, CPU bridé et timeout de 2s. |
| **Désynchronisation des données** | Élevé | Messages RabbitMQ persistants et traitement idempotent. |
| **Pic de charge sur les soumissions** | Moyen | File d'attente Redis pour lisser le flux sans bloquer l'API. |

---

# 🎤 Partie Individuelle (1 min / étudiant)

---

## 👤 Étudiant 1 : Service Commun – Auth & Identity

- **Responsabilité :** Authentification, cycle de vie des comptes, hachage BCrypt, tokens JWT et contrôle d'accès RBAC.
- **Base de données :** **PostgreSQL** (contraintes d'intégrité strictes, unicité des e-mails).
- **Contrats d'API principaux :**
  - `POST /api/auth/register` $\rightarrow$ `201 Created`
  - `POST /api/auth/login` $\rightarrow$ `200 OK` (JWT + Refresh Token)
  - `POST /api/auth/refresh` $\rightarrow$ `200 OK`
- **Exemple d'interaction :** Valide les signatures JWT relayées par l'API Gateway.

---

## 👤 Étudiant 2 : Microservice 1 – Course & Content

- **Responsabilité :** Catalogue de formation, chapitres, modules, leçons et suivi d'avancement.
- **Base de données :** **MongoDB** (structure arborescente naturelle des cours et chapitres).
- **Contrats d'API principaux :**
  - `GET /api/courses` $\rightarrow$ `200 OK` (liste paginée avec filtres)
  - `GET /api/courses/{id}` $\rightarrow$ `200 OK` (détail du cours)
  - `PUT /api/courses/{id}/progress` $\rightarrow$ `200 OK` (mise à jour de la progression)
- **Interaction asynchrone :** Consomme l'événement `CodeSolutionPassedEvent` pour marquer la leçon pratique comme terminée.

---

## 👤 Étudiant 3 : Microservice 2 – Code Execution & Sandbox

- **Responsabilité :** Exécution sécurisée du code étudiant dans un conteneur éphémère Docker et validation par tests unitaires.
- **Base de données :** **Redis** (file FIFO de soumissions) + **PostgreSQL** (historique des résultats).
- **Contrats d'API principaux :**
  - `POST /api/submissions/run` $\rightarrow$ `200 OK` (évaluation contre les tests publics)
  - `POST /api/submissions/submit` $\rightarrow$ `202 Accepted` (évaluation complète en tâche de fond)
  - `GET /api/submissions/{id}` $\rightarrow$ `200 OK` (résultat et métriques CPU/RAM)
- **Interaction asynchrone :** Émet `CodeSolutionPassedEvent` sur RabbitMQ si tous les tests réussissent.

---

## 👤 Étudiant 4 : Microservice 3 – Gamification & Certification

- **Responsabilité :** Calcul des points d'XP, gestion des paliers de niveau, badges et génération de certificats PDF avec empreinte vérifiable.
- **Base de données :** **PostgreSQL** (intégrité transactionnelle stricte pour prévenir toute fraude aux scores).
- **Contrats d'API principaux :**
  - `GET /api/gamification/leaderboard` $\rightarrow$ `200 OK` (classement des meilleurs étudiants)
  - `GET /api/certifications/:userId` $\rightarrow$ `200 OK` (profil certifié)
  - `GET /api/certifications/{id}/verify` $\rightarrow$ `200 OK` (vérification publique)
- **Interactions :**
  - *Asynchrone :* Consomme `CodeSolutionPassedEvent` pour créditer les XP (+50 XP).
  - *Synchrone :* Répond au Job Board pour attester des compétences du candidat.

---

## 👤 Étudiant 5 : Microservice 4 – Job Board & Recruitment

- **Responsabilité :** Publication d'offres d'emploi tech, matching automatisé compétences-profils et gestion des candidatures.
- **Base de données :** **MongoDB** (recherche par mots-clés, tags de langages et frameworks).
- **Contrats d'API principaux :**
  - `POST /api/jobs` $\rightarrow$ `201 Created` (dépôt d'offre par recruteur)
  - `GET /api/jobs/matched` $\rightarrow$ `200 OK` (offres recommandées selon l'XP de l'élève)
  - `POST /api/jobs/{id}/apply` $\rightarrow$ `201 Created` (soumission de candidature)
- **Interaction synchrone :** Interroge `GET /api/certifications/:userId` auprès du service Gamification pour valider l'éligibilité du candidat.

---

## 👤 Étudiant 6 : Microservice 5 – Community & Notifications

- **Responsabilité :** Espace d'entraide par exercice, upvotes de solutions, et canal temps réel de notifications par WebSockets.
- **Base de données :** **MongoDB** (commentaires imbriqués) + **Redis** (sessions WebSockets).
- **Contrats d'API principaux :**
  - `GET /api/community/threads?courseId={id}` $\rightarrow$ `200 OK`
  - `POST /api/community/threads` $\rightarrow$ `201 Created`
  - `WS /ws/notifications` $\rightarrow$ `101 Switching Protocols` (connexion persistante)
- **Interaction asynchrone :** Consomme les événements de réussite de code pour diffuser instantanément les félicitations à l'étudiant via WebSocket.

---

## 🏁 Conclusion

- Une architecture microservices cohérente, modulaire et hautement disponible.
- Une séparation claire des responsabilités avec le principe **Database-per-Service**.
- Des cas d'usage réels combinant flux **REST synchrones** et **événements asynchrones RabbitMQ**.
- Une équipe de 6 étudiants organisée avec des rôles définis et un planning maîtrisé.
