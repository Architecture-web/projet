# SkillQuest – Plateforme d'Apprentissage & Recrutement IT

> **Projet AWD – Architecture Web Distribuée (Validation Intermédiaire – Séance 4)**  
> **Classe :** [Votre Classe, ex: 4DAS16] | **Groupe :** [Votre Groupe, ex: G3]

---

## 👥 Membres de l'Équipe & Répartition des Rôles (6 Personnes)

| Membre | Microservice Attribué | Rôle & Responsabilité principale |
| :--- | :--- | :--- |
| **Membre 1** | **Auth & Identity Service** *(Service Commun)* | Authentification multi-profils, JWT & Sécurité RBAC |
| **Membre 2** | **Course & Learning Content Service** | Catalogue des cours, chapitres & suivi de progression |
| **Membre 3** | **Code Execution & Sandbox Service** | Moteur d'exécution de code sécurisé & tests unitaires |
| **Membre 4** | **Gamification & Certification Service** | Système d'XP, badges, classements & certificats vérifiables |
| **Membre 5** | **Job Board & Recruitment Service** | Offres d'emploi tech, matching compétences & candidatures |
| **Membre 6** | **Community & Peer-Review Service** | Forum d'entraide, partage de solutions & revue de code |
| *(Tous)* | **Notification & Alert Service** | Alertes temps réel (WebSockets), push et e-mails |

---

## 📁 Organisation du Dépôt

```text
skillquest-classe-groupe/
├── Backend/                              # Tous les microservices backend
│   ├── api-gateway/                      # Point d'entrée unique & routage
│   ├── auth-service/                     # Service Commun (JWT / Utilisateurs)
│   ├── course-service/                   # Microservice 1 (Cours & Contenu)
│   ├── code-execution-service/           # Microservice 2 (Sandbox de code)
│   ├── gamification-service/             # Microservice 3 (Badges & Certificats)
│   ├── job-board-service/                # Microservice 4 (Recrutement tech)
│   ├── community-service/                # Microservice 5 (Entraide & Peer-Review)
│   └── notification-service/             # Microservice 6 (Alertes & WebSockets)
├── Frontend/                             # Application cliente SPA (React / Vue / Angular)
├── Documentation/
│   ├── Presentation/                     # Support de présentation (Slides Séance 4)
│   ├── Diagrams/                         # Diagrammes d'architecture, classes et séquence
│   └── Dossier_Technique.md              # Dossier technique complet (Word / LaTeX / MD)
├── docker-compose.yml                    # Orchestration locale des conteneurs
├── .gitignore                            # Fichiers ignorés par Git
└── README.md                             # Documentation d'accueil du dépôt
```

---

## 🎯 Problématique & Présentation du Projet

### 1. Contexte & Problématique
Le recrutement dans le secteur informatique souffre d'un manque d'évaluation pratique des compétences réelles : un CV ne garantit pas la maîtrise du code. Parallèlement, les apprenants ont besoin de plateformes interactives où s'entraîner sur des cas concrets et faire valoir leurs compétences auprès des entreprises.

### 2. Solution : SkillQuest
**SkillQuest** associe formation interactive (*style Coursera*), évaluation algorithmique en bac à sable sécurisé (*style LeetCode*) et un portail de recrutement axé sur les compétences certifiées.

### 3. Acteurs du Système
- **Apprenant / Développeur :** Se forme, soumet du code, gagne des badges d'XP et postule aux offres.
- **Instructeur / Auteur :** Rédige les cours, modules, quiz et configure les tests unitaires des exercices.
- **Recruteur Tech :** Publie des annonces d'emploi et recherche des candidats filtrés par compétences validées.
- **Administrateur :** Modère le contenu, gère les accès et supervise les quotas de ressources de la sandbox.

---

## 🏗️ Architecture Globale & Découpage des Microservices

Chaque microservice métier respecte le principe **Database-per-Service** et possède son propre contrat d'API :

| Microservice | Responsable | Base de Données | Rôle Principal |
| :--- | :--- | :--- | :--- |
| **Auth & Identity** *(Commun)* | Membre 1 (Équipe) | **PostgreSQL** | Inscription, connexion, rôles RBAC, gestion des tokens JWT |
| **Course Content** | Membre 2 | **MongoDB** | Arborescence des cours, chapitres, leçons et état d'avancement |
| **Code Execution** | Membre 3 | **Redis + PostgreSQL** | Exécution de code isolée, tests unitaires et métriques |
| **Gamification** | Membre 4 | **PostgreSQL** | Niveaux, points d'expérience (XP), badges et certificats PDF |
| **Job Board** | Membre 5 | **MongoDB** | Annonces d'emploi, matching compétences-profil et candidatures |
| **Community** | Membre 6 | **MongoDB** | Forums de discussion hiérarchiques, votes et revues de code |
| **Notification** | Support transverse | **Redis + MongoDB** | Notifications WebSockets temps réel et e-mails transactionnels |

---

## 🔄 Flux de Communication

- **Communications Synchrones (REST) :**
  - Le client communique uniquement via l'**API Gateway**.
  - Le *Job Board Service* interroge le *Gamification Service* pour récupérer en direct les certifications d'un candidat sans dupliquer les données.
- **Communications Asynchrones (Message Broker – RabbitMQ) :**
  - Lorsqu'un utilisateur valide tous les tests d'un exercice, le *Code Execution Service* émet l'événement `CodeSolutionPassedEvent`.
  - Le *Gamification Service* consomme cet événement et crédite +50 XP et un badge.
  - Le *Course Content Service* marque la leçon comme validée.
  - Le *Notification Service* pousse instantanément une alerte WebSocket sur le navigateur de l'étudiant.

---

## 🚀 Démarrage Rapide

### Prérequis
- Docker & Docker-Compose
- Node.js (v18+) ou JDK 17+ (selon la stack choisie)
- Git

### Installation
```bash
# 1. Cloner le projet
git clone <URL_DU_DEPOT_GIT>
cd skillquest

# 2. Démarrer l'infrastructure complète avec Docker
docker-compose up -d
```
