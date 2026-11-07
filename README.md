# Nom du Projet

> **Projet AWD – Architecture Web Distribuée (Validation Intermédiaire)**  
> **Classe :** [Ex: 4DAS16] | **Groupe :** [Ex: G3]

---

## 👥 Membres de l'équipe & Répartition des Tâches

| Nom & Prénom | Microservice / Responsabilité | Rôle spécifique |
| :--- | :--- | :--- |
| **Membre 1** | Service Commun (ex: Auth / Notifications) | Conception & Lead API |
| **Membre 2** | Microservice A | Dév & Schéma BD |
| **Membre 3** | Microservice B | Dév & Schéma BD |
| **Membre 4** | Microservice C | Dév & Schéma BD |

---

## 📁 Organisation du Dépôt

```text
nom-du-projet-classe-groupe/
├── Backend/                     # Code source de l'ensemble des microservices
├── Frontend/                    # Application cliente (Web / Mobile)
├── Documentation/
│   ├── Presentation/            # Support de présentation (Slides séance 4)
│   ├── Diagrams/                # Diagrammes d'architecture, classes, séquence
│   └── Dossier_Technique.md     # Dossier technique détaillé
├── .gitignore                   # Fichiers et dossiers ignorés par Git
└── README.md                    # Présentation générale du projet
```

---

## 🎯 Problématique & Cas d'Utilisation

### 1. Idée générale & Problématique
*Description du contexte métier, des besoins utilisateurs et des défis adressés par cette solution.*

### 2. Acteurs identifiés
- **Acteur 1 (ex: Client / Candidat) :** Rôle et actions principales.
- **Acteur 2 (ex: Recruteur / Vendeur) :** Rôle et actions principales.
- **Acteur 3 (ex: Administrateur) :** Gestion de la plateforme et modération.

### 3. Principales fonctionnalités
- **Fonctionnalité 1 :** ...
- **Fonctionnalité 2 :** ...
- **Fonctionnalité 3 :** ...

---

## 🏛️ Architecture Globale & Choix Technologiques

### 1. Composants majeurs
- **API Gateway :** Point d'entrée unique, gestion du routage, de la sécurité et du rate limiting.
- **Microservices Métier :** Découpage indépendant avec base de données dédiée (Database-per-service).
- **Service Commun :** Géré collectivement par l'équipe (ex: authentification JWT / service de logging).
- **Frontend :** Application cliente communiquant exclusivement via l'API Gateway.

### 2. Microservices & Bases de données

| Microservice | Responsabilité | Base de données | Type de communication |
| :--- | :--- | :--- | :--- |
| *Service Commun* | Authentification / Token JWT | PostgreSQL / Redis | Synchrone (REST) |
| *Service 1* | Gestion métier 1 | PostgreSQL / MongoDB | Synchrone & Asynchrone |
| *Service 2* | Gestion métier 2 | MongoDB / MySQL | Asynchrone (Event-driven) |
| *Service 3* | Traitement / Notifications | PostgreSQL | Asynchrone (Message Broker) |

---

## 📋 Prérequis & Démarrage

```bash
# 1. Cloner le dépôt
git clone <URL_DU_DEPOT>

# 2. Se positionner dans le projet
cd <NOM_DU_DOSSIER>
```
