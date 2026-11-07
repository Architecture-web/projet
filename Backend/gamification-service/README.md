# Gamification & Certification Service

## 📌 Présentation du Service
Ce microservice gère le moteur d'engagement de la plateforme : calcul des points d'expérience (XP), passage de niveaux, attribution de badges et génération de certificats PDF vérifiables publiquement.

- **Port d'écoute :** `8084`
- **Responsable :** Étudiant 4
- **Base de données :** **PostgreSQL** (`skillquest_gamification`)
  - *Justification du choix :* Les points d'expérience, les badges et les certificats requièrent une intégrité transactionnelle ACID absolue pour éviter toute duplication de points ou fraude sur les classements.

---

## 🎯 Responsabilités & Fonctionnalités

1. **Calcul des XP & Niveaux :** Crédit de points (+50 XP par exercice résolu) et calcul des paliers (ex: Débutant $\rightarrow$ Intermédiaire $\rightarrow$ Expert).
2. **Attribution de Badges :** Déblocage automatique de distinctions techniques (*"Algorithmique Avancée"*, *"Clean Coder"*, *"Spring Boot Pro"*).
3. **Classements (Leaderboard) :** Classement global et mensuel des meilleurs profils.
4. **Certifications Sécurisées :** Génération de certificats PDF signés numériquement avec une empreinte hashée SHA-256 unique, vérifiable par n'importe quel recruteur via une URL publique.

---

## 🔌 Contrats d'API Principaux

| Méthode | Route | Description | Code Succès |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/gamification/leaderboard` | Top 50 des meilleurs codeurs par XP | `200 OK` |
| `GET` | `/api/gamification/badges/user/{userId}` | Liste des badges obtenus par un apprenant | `200 OK` |
| `GET` | `/api/certifications/:userId` | Profil certifié (Appel synchrone du Job Board) | `200 OK` |
| `GET` | `/api/certifications/{id}/verify` | Vérification publique de l'authenticité d'un certificat | `200 OK` |

---

## 🔄 Communications Asynchrones (RabbitMQ)

- **Queue Écoutée :** `gamification.code_passed.queue`
- **Routing Key :** `code.solution.passed`
- **Action déclenchée :** Dès qu'un exercice est réussi, le service consomme l'événement, ajoute les XP correspondants, vérifie si un badge ou un palier est franchi, et émet éventuellement un événement `BadgeUnlockedEvent`.

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8084
DATABASE_URL=postgresql://postgres:password@localhost:5432/skillquest_gamification
RABBITMQ_URI=amqp://guest:guest@localhost:5672
```
