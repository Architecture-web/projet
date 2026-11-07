# API Gateway (Point d'Entrée Unique)

## 📌 Présentation du Service
L'**API Gateway** constitue le point de contact unique entre les clients externes (Frontend Web / Mobile) et l'écosystème de microservices de SkillQuest. Elle agit en tant que Reverse-Proxy intelligent, pare-feu applicatif et premier rempart de sécurité.

- **Port d'écoute :** `8080`
- **Responsable :** Toute l'équipe (Microservice d'infrastructure commun)
- **Base de données :** *Aucune (Service sans état / Stateless)*

---

## 🎯 Responsabilités & Fonctionnalités

1. **Routage Centralisé :** Réceptionne l'ensemble du trafic externe et l'achemine vers le microservice interne approprié.
2. **Validation des Tokens JWT :** Vérifie l'authenticité de la signature cryptographique du token avant de relayer la requête.
3. **Injection des Headers d'Identité :** Enrichit les requêtes transmises aux services avec les en-têtes internes :
   - `X-User-Id` : Identifiant unique de l'utilisateur connecté.
   - `X-User-Email` : Adresse e-mail du compte.
   - `X-User-Role` : Rôle applicatif (`ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_RECRUITER`, `ROLE_ADMIN`).
4. **Sécurité Périmétrique :** Gestion globale des en-têtes CORS et limitation du débit de requêtes (*Rate Limiting*) pour contrer les attaques par déni de service (DDoS).

---

## 🗺️ Table de Routage Interne

| Préfixe URL Public | Microservice Cible | Port Interne | Authentification requise ? |
| :--- | :--- | :---: | :---: |
| `/api/auth/**` | `auth-service` | `8081` | Non (Publique) |
| `/api/courses/**` | `course-service` | `8082` | Oui (Lecture publique / Écriture protégée) |
| `/api/submissions/**` | `code-execution-service` | `8083` | Oui (`ROLE_STUDENT`) |
| `/api/gamification/**` | `gamification-service` | `8084` | Oui |
| `/api/jobs/**` | `job-board-service` | `8085` | Oui (Lecture apprenant / Dépôt recruteur) |
| `/api/community/**` | `community-service` | `8086` | Oui |
| `/ws/**` | `notification-service` | `8087` | Oui (Protocole WebSocket) |

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8080
JWT_SECRET=super_secret_jwt_key_skillquest_2026_very_long_secure
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
RATE_LIMIT_PER_MINUTE=100
```
