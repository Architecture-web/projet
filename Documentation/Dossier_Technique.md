# Dossier Technique – Projet AWD (Validation Intermédiaire)

**Projet :** [Nom du Projet]  
**Classe / Groupe :** [ex: 4DAS16 - Groupe 3]  
**Date :** Séance 4  
**Enseignant :** [Nom de l'enseignant]

---

## 1. Description du Sujet & Problématique

### 1.1 Contexte et Objectifs
[Expliquer le contexte du projet, le besoin auquel il répond et la valeur ajoutée de la solution.]

### 1.2 Acteurs du Système
- **Acteur 1 :** [Description des prérogatives et interactions]
- **Acteur 2 :** [Description des prérogatives et interactions]
- **Administrateur :** [Supervision, modération et monitoring]

### 1.3 Cas d'Utilisation Généraux
[Lister et synthétiser les cas d'utilisation majeurs.]

---

## 2. Architecture Globale Proposée

### 2.1 Schéma d'Architecture Globale
*(Insérer le schéma exporté dans `Documentation/Diagrams/architecture_globale.png`)*

### 2.2 Composants d'Infrastructure
- **API Gateway :** Rôle de reverse proxy, terminaison SSL/TLS, centralisation de l'authentification et dispatching vers les services.
- **Service Discovery / Registre (optionnel) :** Découverte dynamique des instances.
- **Message Broker (Event-Driven) :** Communication asynchrone (ex: RabbitMQ / Kafka) pour le découplage temporel.
- **Sécurité :** Tokens JWT, RBAC (Role-Based Access Control) validés au niveau de la passerelle ou des filtres de microservices.

---

## 3. Découpage en Microservices & Données Gérées

### 3.1 Microservice Commun : [Nom du service commun]
- **Responsables :** Toute l'équipe
- **Rôle :** [Ex: Service d'authentification et gestion des identités / Service de notification partagé]
- **Base de données :** [Ex: PostgreSQL / Redis pour sessions]
- **Données gérées :** [Entités gérées, ex: Utilisateurs, Rôles, Permissions, Refresh Tokens]

### 3.2 Microservice 1 : [Nom du Service]
- **Responsable :** [Nom de l'étudiant]
- **Rôle métier :** [Description]
- **Base de données :** [Choix et justification, ex: MongoDB / PostgreSQL]
- **Données gérées :** [Entités et modèles]

### 3.3 Microservice 2 : [Nom du Service]
- **Responsable :** [Nom de l'étudiant]
- **Rôle métier :** [Description]
- **Base de données :** [Choix et justification]
- **Données gérées :** [Entités et modèles]

### 3.4 Microservice 3 : [Nom du Service]
- **Responsable :** [Nom de l'étudiant]
- **Rôle métier :** [Description]
- **Base de données :** [Choix et justification]
- **Données gérées :** [Entités et modèles]

---

## 4. Interactions & Communications Inter-Services

### 4.1 Communications Synchrones (REST / gRPC)
- *Exemple :* L'API Gateway relaie la requête client vers le Microservice A en HTTP REST (JSON).
- *Exemple :* Le Microservice A interroge le Service Commun pour valider les droits d'un token.

### 4.2 Communications Asynchrones (Événements / Messaging)
- *Exemple d'événement :* `UserRegisteredEvent` émis par le service d'authentification sur le topic `users.events`, consommé par le service de notification pour l'envoi d'e-mails.

---

## 5. Contrats d'API & Spécifications

### 5.1 Service Commun
- `POST /api/auth/register` (201 Created, 400 Bad Request)
- `POST /api/auth/login` (200 OK avec JWT, 401 Unauthorized)

### 5.2 Microservice 1 ([Nom de l'étudiant])
- `GET /api/v1/...`
- `POST /api/v1/...`

### 5.3 Microservice 2 ([Nom de l'étudiant])
- `GET /api/v1/...`
- `POST /api/v1/...`

---

## 6. Organisation du Projet & Gestion des Risques

### 6.1 Organisation Git & Outils
- **Dépôt Git :** Branche principale protégée (`main`), branches par fonctionnalité (`feature/...`).
- **Outil de suivi :** Trello / Jira / GitHub Projects.

### 6.2 Planning Prévisionnel
- **Phase 1 (Séance 4) :** Validation intermédiaire de l'architecture, modèles de données et contrats d'API.
- **Phase 2 :** Implémentation des microservices et tests unitaires/intégration.
- **Phase 3 :** Intégration de l'API Gateway et du Frontend.
- **Phase 4 :** Déploiement conteneurisé (Docker / Docker-Compose).

### 6.3 Analyse des Risques & Mesures d'Atténuation
| Risque identifié | Impact | Probabilité | Solution / Contournement |
| :--- | :--- | :--- | :--- |
| Complexité des communications inter-services | Élevé | Moyen | Définir des contrats d'API stricts (OpenAPI/Swagger) dès le début |
| Désynchronisation des données entre bases | Élevé | Faible | Utiliser le pattern Saga / Eventual consistency avec compensation |
| Retard sur l'intégration Frontend | Moyen | Moyen | Mocker les endpoints via Swagger/Postman pendant le dev frontend |
