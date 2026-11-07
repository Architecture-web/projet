# Community & Peer-Review Service

## 📌 Présentation du Service
Ce microservice alimente l'entraide et l'apprentissage collaboratif au sein de la communauté SkillQuest. Il propose des fils de discussion par exercice et un espace de partage de solutions de code optimisées (*Peer-Review*).

- **Port d'écoute :** `8086`
- **Responsable :** Étudiant 6
- **Base de données :** **MongoDB** (`skillquest_community`)
  - *Justification du choix :* Les discussions sous forme d'arborescence (threads, commentaires imbriqués et réponses) se modélisent et s'interrogent de façon performante dans une base de données de documents.

---

## 🎯 Responsabilités & Fonctionnalités

1. **Forums Contextualisés :** Fil de discussion attaché à chaque exercice ou chapitre pour poser des questions de compréhension sans révéler directement la solution.
2. **Système de Votes d'Utilité :** Upvotes et downvotes sur les réponses pour faire émerger les explications les plus pédagogiques.
3. **Revue de Code entre Pairs (Peer-Review) :** Une fois qu'un étudiant a validé un exercice, il débloque l'accès aux solutions partagées par la communauté pour comparer les complexités algorithmiques ($O(N)$ vs $O(N^2)$) et apprendre de nouvelles pratiques.

---

## 🔌 Contrats d'API Principaux

| Méthode | Route | Description | Code Succès |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/community/threads?exerciseId={id}` | Liste des discussions pour un exercice donné | `200 OK` |
| `POST` | `/api/community/threads` | Création d'une nouvelle question / discussion | `201 Created` |
| `POST` | `/api/community/posts/{postId}/comments` | Ajout d'une réponse à un message existant | `201 Created` |
| `POST` | `/api/community/posts/{postId}/vote` | Voter (+1 / -1) pour une explication | `200 OK` |

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8086
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_community?authSource=admin
```
