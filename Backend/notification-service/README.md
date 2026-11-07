# Notification & Alert Service

## 📌 Présentation du Service
Ce microservice transverse assure la diffusion d'alertes instantanées dans l'interface utilisateur grâce au protocole **WebSocket** et l'envoi d'e-mails transactionnels asynchrones.

- **Port d'écoute :** `8087`
- **Responsable :** Support transverse (Tous les membres)
- **Base de données / Cache :** **Redis** (gestion en mémoire des sessions WebSockets actives) + **MongoDB** (`skillquest_notifications` pour l'historique pérenne)
  - *Justification du choix :* Redis offre la réactivité nécessaire pour router les messages en temps réel vers les bons canaux d'utilisateurs connectés.

---

## 🎯 Responsabilités & Fonctionnalités

1. **Diffusion Temps Réel par WebSockets :**
   - Canal persistant (`ws://localhost:8080/ws/notifications`).
   - Notification immédiate lorsqu'un résultat d'évaluation de code est prêt, qu'un badge est débloqué ou qu'une réponse est apportée sur le forum.
2. **E-mails Transactionnels :**
   - Notifications différées (ex: confirmation d'inscription, invitation d'entretien par un recruteur, rapport hebdomadaire d'avancement).
3. **Historique des Notifications :**
   - Stockage et consultation de l'historique des alertes avec statut lu / non lu.

---

## 🔌 Contrats d'API Principaux

| Méthode | Route | Description | Code Succès |
| :--- | :--- | :--- | :---: |
| `WS` | `/ws/notifications` | Connexion persistante WebSocket pour alertes live | `101 Switching Protocols` |
| `GET` | `/api/notifications` | Historique des alertes de l'utilisateur connecté | `200 OK` |
| `PUT` | `/api/notifications/{id}/read` | Marquer une notification comme lue | `200 OK` |

---

## 🔄 Communications Asynchrones (RabbitMQ)

- **Queues Écoutées :**
  - `notification.code_events.queue` (consomme `CodeSolutionPassedEvent`)
  - `notification.gamification_events.queue` (consomme `BadgeUnlockedEvent`)
- **Action déclenchée :** À la réception d'un événement, le service formate un message JSON convivial et le pousse instantanément sur la session WebSocket de l'utilisateur ciblé.

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8087
REDIS_URL=redis://localhost:6379
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_notifications?authSource=admin
RABBITMQ_URI=amqp://guest:guest@localhost:5672
SMTP_HOST=smtp.mailtrap.io
SMTP_PORT=2525
```
