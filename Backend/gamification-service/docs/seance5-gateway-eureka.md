# Séance 5 — Gateway, Eureka et Swagger pour le gamification-service

## 1. Découverte de services (Eureka client)
Le service s'enregistre auprès d'Eureka sous le nom **`GAMIFICATION-SERVICE`** (`spring.application.name`).

| Variable | Défaut | Rôle |
| :--- | :--- | :--- |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Adresse du serveur Eureka |
| `EUREKA_ENABLED` | `true` | `false` pour démarrer sans Eureka (le profil `dev` le désactive) |

L'instance est publiée sous la forme `gamification-service:<port>` : la gateway n'a donc **pas besoin de connaître le port**.
Si Eureka est arrêté, le service démarre quand même (il réessaie en arrière-plan).

## 2. Routes à ajouter dans l'API Gateway (Spring Cloud Gateway)
À fusionner dans `spring.cloud.gateway.routes` du `application.yml` de la gateway :

```yaml
        - id: gamification-service
          uri: lb://gamification-service      # nom Eureka ; équilibrage de charge automatique
          predicates:
            - Path=/api/gamification/**,/api/certifications/**,/api/quizzes/**,/api/admin/**
```

La gateway doit aussi être cliente Eureka (`spring-cloud-starter-netflix-eureka-client` + `spring-cloud-starter-loadbalancer`)
et pointer vers le même serveur (`eureka.client.service-url.defaultZone`).

Notes pour la table de routage du README de la gateway :
- `/api/certifications/**`, `/api/quizzes/**` et `/api/admin/**` sont **en plus** de `/api/gamification/**` (absents de la table actuelle).
- Le service **valide lui-même le JWT** (même secret que `auth-service`) : la gateway doit simplement transmettre l'en-tête `Authorization`.
- Autoriser le CORS du frontend sur la gateway (origines `http://localhost:8081`, etc.) pour les appels du navigateur.
- Le **secret JWT de la gateway doit être identique à celui de `auth-service`** (`app.jwt.secret`), sinon les jetons seront refusés.

## 3. Tester à travers le point d'entrée unique
Remplacer `G` par l'adresse de la gateway (`http://localhost:8080`, ou son port si 8080 est pris — voir ci-dessous).

```bash
G=http://localhost:8080
curl $G/api/gamification/leaderboard                                   # public -> 200
curl -X POST $G/api/gamification/profiles -H "Content-Type: application/json" -d '{"userId":"<uuid>"}'   # 201 puis 409
curl $G/api/quizzes                                                    # sans jeton -> 401
TOKEN=$(curl -s -X POST $G/api/auth/login -H "Content-Type: application/json" -d '{"email":"...","password":"..."}' | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" $G/api/quizzes                  # étudiant -> 200
curl -H "Authorization: Bearer $TOKEN" $G/api/admin/quizzes            # étudiant -> 403 (ROLE_ADMIN requis)
```

> ⚠️ Sur un PC avec **Oracle Database** installé, le port **8080** est souvent occupé par `TNSLSNR.EXE`. Démarrer alors la gateway sur un autre port (`PORT=8090`).

## 4. Swagger / OpenAPI
- Interface : `http://localhost:8084/swagger-ui.html` — spécification JSON : `http://localhost:8084/v3/api-docs`.
- Groupes affiches : **Gamification**, **Certifications** et **Quiz (etudiant)** (`springdoc.paths-to-match`). Le groupe **Administration**
  (`/api/admin/**`) reste actif et reserve a `ROLE_ADMIN` mais n'est pas liste ; retirer la propriete pour le documenter aussi.
- Chaque route documente ses codes de réponse (200/201/204, 400, 401, 403, 404, 409).
- Bouton **Authorize** : coller le `accessToken` obtenu avec `POST /api/auth/login` (sans le mot « Bearer »)
  pour tester `/api/quizzes/**` (utilisateur connecté) et `/api/admin/**` (`ROLE_ADMIN`).

## 5. Démo locale (hors dépôt)
Un Eureka et une gateway de démonstration sont fournis dans `Desktop\eureka-gateway-demo` (`start-demo.bat`) pour valider ces routes
si la gateway de l'équipe n'est pas encore disponible.
