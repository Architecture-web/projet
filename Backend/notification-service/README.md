# Notification & Alert Service (Microservice 6 – Support Transverse)

## 📌 1. Présentation du Service
Ce microservice assure la diffusion d'alertes instantanées dans le navigateur via le protocole **WebSocket** ainsi que l'envoi d'e-mails transactionnels asynchrones.

- **Port local :** `8087`
- **Responsable :** Support Transverse (Couvert avec l'Étudiant 6 / Équipe)
- **Bases de données :** **Redis 7** (sessions WebSockets actives en mémoire) + **MongoDB 6.0** (`skillquest_notifications`)
- **Justification du choix :** Redis permet d'identifier en une fraction de milliseconde si l'utilisateur est connecté et de router le message vers la bonne instance de socket. MongoDB assure la persistance pérenne de l'historique des notifications avec horodatage et état lu / non lu.

---

## 📐 2. Conception & Diagramme de Classes

```mermaid
classDiagram
    class UserNotification {
        +UUID id
        +String recipientUserId
        +NotificationType type
        +String title
        +String message
        +Boolean isRead
        +DateTime createdAt
        +markAsRead()
    }

    class WebSocketSessionRegistry {
        +Map activeSessions
        +registerSession(userId, session)
        +removeSession(userId)
        +isUserOnline(userId) Boolean
    }

    class EmailNotificationJob {
        +UUID id
        +String toEmail
        +String subject
        +String templateName
        +EmailStatus status
        +DateTime scheduledAt
    }

    UserNotification ..> WebSocketSessionRegistry : routé en direct vers
    UserNotification ..> EmailNotificationJob : déclenche si hors-ligne
```

### Description des Entités
- **`UserNotification`** : Modèle de données persistant de l'alerte adressée à un apprenant ou recruteur.
- **`WebSocketSessionRegistry`** : Registre en mémoire Redis conservant la correspondance entre `userId` et les connexions actives de socket.
- **`EmailNotificationJob`** : Tâche d'envoi d'e-mail différé (SMTP / Mailtrap) pour les alertes prioritaires lorsque l'utilisateur est déconnecté.

---

## ⚡ 3. Exigences Techniques AWD – Interactions Inter-Services

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel reçu depuis :** `auth-service` (Port 8081)
- **Endpoint exposé :** `POST http://localhost:8087/api/notifications/direct-email`
- **Payload reçu :**
  ```json
  {
    "recipientEmail": "sarah.dev@example.com",
    "subject": "Confirmez votre compte SkillQuest",
    "template": "account-activation",
    "variables": { "token": "act_88192a" }
  }
  ```
- **Code de retour renvoyé :** `202 Accepted`
- **Justification technique :** Lors d'un processus sensible nécessitant une validation immédiate de prise en charge (par exemple l'envoi d'un mot de passe oublié ou d'un code 2FA), l'Auth Service sollicite directement ce point d'entrée REST pour obtenir un acquittement de livraison immédiat.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `NotificationDispatchedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic)
- **Routing Key :** `notification.alert.dispatched`
- **Payload JSON :**
  ```json
  {
    "eventId": "evt_notif_901",
    "eventType": "NOTIFICATION_DISPATCHED",
    "notificationId": "notif_3321",
    "recipientUserId": "usr_9823f4a1",
    "channel": "WEBSOCKET",
    "status": "DELIVERED",
    "timestamp": "2026-11-09T14:45:00Z"
  }
  ```
- **Justification :** Alimente les métriques de monitoring de la plateforme et garantit la traçabilité de la bonne réception des messages critiques.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événements consommés :**
  1. `CodeSolutionPassedEvent` (Queue: `notification.code_events.queue`) $\rightarrow$ Pousse une alerte félicitant l'élève.
  2. `BadgeUnlockedEvent` (Queue: `notification.badge_events.queue`) $\rightarrow$ Pousse une notification visuelle de déblocage d'insigne.
  3. `JobApplicationSubmittedEvent` (Queue: `notification.job_events.queue`) $\rightarrow$ Notifie le recruteur qu'une candidature certifiée a été déposée.
- **Action déclenchée :** À la réception de chaque message, le service vérifie via le `WebSocketSessionRegistry` (Redis) si l'utilisateur est connecté en ligne. Si oui, il diffuse instantanément l'alerte sur son socket. Sinon, il crée un `EmailNotificationJob` pour un envoi par courriel.

---

## 📋 4. Principaux Contrats d'API

| Méthode | Route | Description | Code Succès | Erreurs Possibles |
| :--- | :--- | :--- | :---: | :--- |
| `WS` | `/ws/notifications` | Connexion permanente WebSocket (Upgrade) | `101 Switching Protocols` | `401 Unauthorized` |
| `GET` | `/api/notifications` | Historique des alertes de l'utilisateur connecté | `200 OK` | `401 Unauthorized` |
| `PUT` | `/api/notifications/{id}/read` | Marquer une alerte comme lue | `200 OK` | `404 Not Found` |
| `POST` | `/api/notifications/direct-email` | Envoi synchrone d'un e-mail transactionnel | `202 Accepted` | `400 Bad Request` |

---

## ⚙️ 5. Configuration d'Environnement (`.env`)

```properties
PORT=8087
REDIS_URL=redis://localhost:6379
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_notifications?authSource=admin
RABBITMQ_URI=amqp://guest:guest@localhost:5672
SMTP_HOST=smtp.mailtrap.io
SMTP_PORT=2525
```
