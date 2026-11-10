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

> **En résumé :**
> - **Synchrone (REST) :** Reçoit des requêtes de `auth-service` pour expédier immédiatement des e-mails sensibles (activation, mot de passe).
> - **Asynchrone (Publie) :** Émet `NotificationDispatchedEvent` pour confirmer qu'une alerte a bien été délivrée.
> - **Asynchrone (Consomme) :** Écoute les événements des autres services (`CodeSolutionPassedEvent`, `BadgeUnlockedEvent`, `JobApplicationSubmittedEvent`) pour envoyer des alertes WebSockets en direct ou par e-mail.

### 3.1. Communication Synchrone REST (Au moins 1)
- **Appel reçu depuis :** `auth-service` (Port 8081)
- **Endpoint exposé :** `POST http://localhost:8087/api/notifications/direct-email`
- **Explication simple :** Quand un utilisateur s'inscrit ou demande à réinitialiser son mot de passe, `auth-service` contacte directement ce service en REST pour lancer l'envoi immédiat de l'e-mail.

### 3.2. Publication d'un Événement Métier (Au moins 1)
- **Événement publié :** `NotificationDispatchedEvent`
- **Exchange RabbitMQ :** `skillquest.events` (Topic `notification.alert.dispatched`)
- **Explication simple :** Le service publie cet événement chaque fois qu'un message est remis pour garder une trace et vérifier que le système d'alertes fonctionne.

### 3.3. Consommation d'un Événement Métier (Au moins 1)
- **Événements consommés :**
  1. `CodeSolutionPassedEvent` : Envoie une notification félicitant l'élève dès qu'un exercice est réussi.
  2. `BadgeUnlockedEvent` : Affiche une alerte animée sur l'écran quand un badge est débloqué.
  3. `JobApplicationSubmittedEvent` : Préviens immédiatement le recruteur qu'un élève a postulé à son offre.
- **Explication simple :** Si l'utilisateur est connecté sur le site, le message apparaît en temps réel à l'écran grâce aux WebSockets. S'il est déconnecté, le service lui envoie un e-mail.

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
