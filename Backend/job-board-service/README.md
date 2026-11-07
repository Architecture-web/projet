# Job Board & Tech Recruitment Service

## 📌 Présentation du Service
Ce microservice connecte les entreprises à la recherche de développeurs qualifiés avec les apprenants certifiés de la plateforme. Il gère la publication d'offres et un algorithme de matching basé sur les compétences réelles plutôt que sur des déclarations de CV.

- **Port d'écoute :** `8085`
- **Responsable :** Étudiant 5
- **Base de données :** **MongoDB** (`skillquest_jobs`)
  - *Justification du choix :* Flexibilité du modèle pour stocker des offres d'emploi contenant des listes variables de compétences requises, des descriptions riches et une indexation multi-critères.

---

## 🎯 Responsabilités & Fonctionnalités

1. **Portail Recruteurs :** Publication, modification et fermeture d'annonces de stages et emplois tech.
2. **Algorithme de Matching Automatisé :** Analyse croisée entre les exigences de l'offre (langages, XP minimum, badges demandés) et le profil certifié de l'étudiant.
3. **Gestion des Candidatures :** Suivi des statuts de postulation (`CANDIDATURE_REÇUE`, `EN_COURS_EXAMEN`, `ENTRETIEN_PROGRAMMÉ`, `REFUSÉE`, `ACCEPTÉE`).
4. **Appel Synchrone REST :** Interrogation en temps réel du service de Gamification (`GET /api/certifications/:userId`) pour récupérer les badges vérifiés d'un candidat sans dupliquer les données.

---

## 🔌 Contrats d'API Principaux

| Méthode | Route | Description | Code Succès |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/jobs` | Dépôt d'une offre d'emploi (*Réservé aux recruteurs*) | `201 Created` |
| `GET` | `/api/jobs` | Recherche et consultation paginée des offres | `200 OK` |
| `GET` | `/api/jobs/matched` | Liste des offres recommandées selon les compétences de l'élève | `200 OK` |
| `POST` | `/api/jobs/{jobId}/apply` | Postulation à une offre avec score certifié | `201 Created` |

---

## ⚙️ Configuration Requise (`.env`)

```properties
PORT=8085
MONGODB_URI=mongodb://root:rootpassword@localhost:27017/skillquest_jobs?authSource=admin
GAMIFICATION_SERVICE_URL=http://localhost:8084
```
