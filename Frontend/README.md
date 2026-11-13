# Frontend SkillQuest – Landing Page & Web Client

Ce dossier contient l'interface Web client de la plateforme **SkillQuest**, intégrée au microservice d'authentification (`auth-service`, port 8081).

## 🚀 Architecture des Pages & Tableaux de Bord
- **`index.html` (Landing Page & Portails d'Authentification) :**
  - Catalogue de cours et aperçu sandbox d'exécution de code Docker.
  - Modales d'inscription (`POST /api/auth/register`) et connexion classique (`POST /api/auth/login`).
  - **Popup de Confirmation OAuth Interactif (`#oauthConsentModal`) :**
    - S'ouvre lors du clic sur Google ou GitHub.
    - Affiche la demande de consentement réaliste avec permissions et compte recommandé.
    - Permet de confirmer en 1 clic ou de saisir n'importe quel **e-mail/nom réel** et de choisir son rôle (**Étudiant** ou **Entreprise/Recruteur**).
    - Envoie les données vers `POST /api/auth/social`, enregistre les jetons JWT et déclenche la redirection automatique.
  - **Mot de passe oublié & réinitialisation :** Formulaires avec token sécurisé d'1 heure.

- **`dashboard-student.html` (Espace Étudiant) :**
  - Tableau de bord complet : progression sur les cours d'algorithmique et microservices.
  - Métriques gamifiées (XP, niveau, badges Docker & Java, classement).
  - Vues des entreprises ayant consulté les soumissions de code validées.
  - Accès direct à la sandbox et protection de session par token JWT.

- **`dashboard-recruiter.html` (Portail Entreprises & Recruteurs) :**
  - Vivier de développeurs qualifiés par leurs résultats de code en temps réel sur Docker.
  - Fiches candidats avec score sandbox, pile technique (Java, Go, Python, Spring, PostgreSQL) et bouton de contact/invitation.
  - Gestion des épreuves techniques et statistiques de recrutement.

## 💻 Comment Lancer la Landing Page
Vous pouvez ouvrir directement le fichier `index.html` dans votre navigateur ou démarrer un serveur web local léger :

```bash
# Option 1 : Avec Python
cd Frontend
python3 -m http.server 3000

# Option 2 : Avec npx serve (si Node.js disponible)
npx serve Frontend -p 3000
```

Accédez ensuite à : `http://localhost:3000` (ou double-cliquez sur `Frontend/index.html`).
