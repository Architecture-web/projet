# SkillQuest – Plateforme d'Apprentissage & Recrutement IT

> **Projet AWD – Architecture Web Distribuée**  
> **Classe :** [Votre Classe] | **Groupe :** [Votre Groupe]

---

## 🎯 Description du Projet

**SkillQuest** est une plateforme web distribuée orientée microservices qui combine l'apprentissage en ligne modulaire (*style Coursera*) avec un moteur d'évaluation de code en temps réel dans une sandbox sécurisée (*style LeetCode*), le tout connecté à un portail de recrutement technique basé sur des compétences certifiées.

Pour une description détaillée et les schémas techniques, consultez **[Documentation/Architecture.md](Documentation/Architecture.md)**.

---

## 👥 Répartition des Rôles (Équipe de 6 Personnes)

| Membre | Microservice | Responsabilité | Base de données |
| :--- | :--- | :--- | :--- |
| **Membre 1 (Équipe)** | **Auth & Identity Service** *(Commun)* | Authentification, JWT, rôles RBAC | **PostgreSQL** |
| **Membre 2** | **Course Content Service** | Modules de cours, leçons, progression | **MongoDB** |
| **Membre 3** | **Code Execution Service** | Sandbox d'exécution de code isolée & tests | **Redis + PostgreSQL** |
| **Membre 4** | **Gamification Service** | Système d'XP, badges et certificats PDF | **PostgreSQL** |
| **Membre 5** | **Job Board Service** | Offres tech, matching compétences/profils | **MongoDB** |
| **Membre 6** | **Community Service** | Forums d'entraide, votes et revue de code | **MongoDB** |
| *(Transverse)* | **Notification Service** | Alertes WebSockets temps réel et e-mails | **Redis + MongoDB** |

---

## 📁 Structure du Dépôt

Conforme aux directives du cours :

```text
├── Backend/
│   ├── api-gateway/                      # Point d'entrée unique & Reverse-Proxy
│   ├── auth-service/                     # Service Commun (JWT / Sécurité)
│   ├── course-service/                   # Microservice 1 (Cours & Contenu)
│   ├── code-execution-service/           # Microservice 2 (Sandbox de code)
│   ├── gamification-service/             # Microservice 3 (Badges & Certificats)
│   ├── job-board-service/                # Microservice 4 (Recrutement & Matching)
│   ├── community-service/                # Microservice 5 (Entraide & Peer-Review)
│   └── notification-service/             # Microservice 6 (Alertes & WebSockets)
├── Frontend/                             # Application cliente (SPA Web)
├── Documentation/
│   ├── Presentation/                     # Support de présentation (Séance 4)
│   ├── Diagrams/                         # Diagrammes d'architecture et de conception
│   └── Architecture.md                   # Description complète de l'architecture
├── .gitignore                            # Fichiers ignorés par Git
└── README.md                             # Présentation générale du projet
```
