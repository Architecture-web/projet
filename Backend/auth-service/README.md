# Auth & Identity Service (Service Commun)

## 📌 Présentation du Service
Le service d'authentification centralise l'enregistrement, la sécurisation des accès et la gestion du cycle de vie des comptes utilisateurs. Il est développé collectivement par l'équipe en tant que **service commun** du projet.

- **Port d'écoute :** `8081`
- **Responsables :** Toute l'équipe (Présenté par un membre désigné)
- **Base de données :** **PostgreSQL** (`skillquest_auth`)

---

## 🎯 Responsabilités & Fonctionnalités

1. **Cycle de vie des utilisateurs :** Inscription, validation d'e-mail, modification de profil et réinitialisation de mot de passe.
2. **Sécurité & Chiffrement :** Hachage des mots de passe avec sel cryptographique via **BCrypt**.
3. **Gestion des Rôles (RBAC) :**
   - `ROLE_STUDENT` : Accès aux cours, exécution de code, forum et candidatures.
   - `ROLE_INSTRUCTOR` : Création et édition de modules pédagogiques et de quiz.
   - `ROLE_RECRUITER` : Publication d'annonces de recrutement et recherche de talents certifiés.
   - `ROLE_ADMIN` : Modération globale et supervision des ressources.
4. **Distribution des Tokens :**
   - `access_token` : Token JWT à durée de vie courte (15 minutes).
   - `refresh_token` : Token stocké en base de données permettant de renouveler la session en toute sécurité (durée : 7 jours).

---

## 🔌 Contrats d'API Principaux

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `POST` | `/api/auth/register` | Inscription d'un nouvel utilisateur | `201 Created` | `400 Bad Request`, `409 Conflict` |
| `POST` | `/api/auth/login` | Connexion et délivrance des tokens JWT | `200 OK` | `401 Unauthorized` |
| `POST` | `/api/auth/refresh` | Renouvellement de l'access token | `200 OK` | `403 Forbidden` |
| `GET` | `/api/auth/profile` | Récupération des infos de l'utilisateur connecté | `200 OK` | `401 Unauthorized` |

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8081
DATABASE_URL=postgresql://postgres:password@localhost:5432/skillquest_auth
JWT_SECRET=super_secret_jwt_key_skillquest_2026_very_long_secure
JWT_EXPIRATION_MS=900000
REFRESH_TOKEN_EXPIRATION_MS=604800000
```
