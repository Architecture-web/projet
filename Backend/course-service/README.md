# Course & Learning Content Service

## 📌 Présentation du Service
Ce microservice gère le catalogue des formations, l'arborescence des chapitres/leçons et le suivi de l'avancement pédagogique de chaque apprenant.

- **Port d'écoute :** `8082`
- **Responsable :** Étudiant 2
- **Base de données :** **MongoDB** (`skillquest_courses`)
  - *Justification du choix :* Structure documentaire naturelle pour imbriquer des cours, des modules ordonnés, des leçons multimédia (vidéos, Markdown) et des quiz sans nécessiter de multiples jointures relationnelles complexes.

---

## 🎯 Responsabilités & Fonctionnalités

1. **Gestion du Catalogue :** Création, mise à jour et consultation paginée des formations selon le niveau (débutant, intermédiaire, avancé) et les tags techniques.
2. **Arborescence des Contenus :** Organisation hiérarchique : `Cours` $\rightarrow$ `Modules` $\rightarrow$ `Leçons`.
3. **Persistance de l'Avancement :** Enregistrement des leçons terminées par chaque étudiant et calcul du pourcentage de complétion.
4. **Réaction Asynchrone :** Détection automatique de la validation des défis de code pour marquer les leçons pratiques comme terminées.

---

## 🔌 Contrats d'API Principaux

| Méthode | Route | Description | Code Succès |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/courses` | Liste paginée des cours avec filtres de recherche | `200 OK` |
| `GET` | `/api/courses/{id}` | Détails d'un cours avec plan complet des leçons | `200 OK` |
| `POST` | `/api/courses` | Création d'une formation (*Réservé aux instructeurs*) | `201 Created` |
| `PUT` | `/api/courses/{id}/progress` | Mise à jour manuelle ou automatique de l'avancement | `200 OK` |

---

## 🔄 Communications Asynchrones (RabbitMQ)

- **Queue Écoutée :** `course.code_passed.queue`
- **Exchange :** `skillquest.events` (Topic)
- **Routing Key :** `code.solution.passed`
- **Action déclenchée :** Dès que le message est consommé, le service identifie la leçon pratique correspondante et la marque comme "Validée" dans le suivi de l'élève.

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8082
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_courses?authSource=admin
RABBITMQ_URI=amqp://guest:guest@localhost:5672
```
