# Code Execution & Sandbox Service

## 📌 Présentation du Service
Cœur technique pratique de la plateforme, ce microservice permet d'exécuter et d'évaluer le code source soumis par les étudiants dans un environnement de bac à sable (*sandbox*) totalement étanche et sécurisé.

- **Port d'écoute :** `8083`
- **Responsable :** Étudiant 3
- **Stockage & Cache :** **Redis** (file d'attente FIFO) + **PostgreSQL** (`skillquest_submissions` pour l'historique pérenne)
  - *Justification du choix :* Redis absorbe les pics d'évaluation simultanée en mémoire ultra-rapide. PostgreSQL garantit l'intégrité de l'historique d'évaluation et des temps d'exécution.

---

## 🎯 Responsabilités & Fonctionnalités

1. **Isolation & Sécurité de la Sandbox :**
   - Exécution du code dans un conteneur éphémère (Docker / Isolate) sans privilèges root.
   - Désactivation stricte de tout accès réseau (pour empêcher les requêtes sortantes malveillantes).
   - Quotas machine stricts : limite mémoire à 128 Mo et interruption automatique au-delà de 2 secondes (*Timeout / Boucles infinies*).
2. **Évaluation par Jeux de Tests :**
   - *Tests Publics :* Accessibles immédiatement à l'étudiant pour vérifier ses cas d'usage de base.
   - *Tests Secrets :* Cas limites (edge cases) et tests de performance pour attribuer la note finale.
3. **Publication d'Événements Métier :** Émission sur le bus RabbitMQ dès qu'une solution valide l'intégralité des tests.

---

## 🔌 Contrats d'API Principaux

| Méthode | Route | Description | Code Succès |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/submissions/run` | Test rapide du code contre les tests publics | `200 OK` |
| `POST` | `/api/submissions/submit` | Soumission définitive et évaluation sandbox complète | `202 Accepted` |
| `GET` | `/api/submissions/{id}` | Récupération du statut (PENDING, SUCCESS, FAILED, TIMEOUT) | `200 OK` |

---

## 🔄 Émission d'Événements (RabbitMQ)

- **Exchange :** `skillquest.events` (Topic)
- **Routing Key :** `code.solution.passed`
- **Payload Exemple :**
  ```json
  {
    "eventId": "evt_9918274a",
    "eventType": "CODE_SOLUTION_PASSED",
    "userId": "usr_9823f4a1",
    "exerciseId": "ex_binary_search_01",
    "language": "JAVA",
    "executionTimeMs": 42,
    "timestamp": "2026-11-08T12:00:00Z"
  }
  ```

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8083
DATABASE_URL=postgresql://postgres:password@localhost:5432/skillquest_submissions
REDIS_URL=redis://localhost:6379
RABBITMQ_URI=amqp://guest:guest@localhost:5672
EXECUTION_TIMEOUT_SECONDS=2
MAX_MEMORY_MB=128
```
