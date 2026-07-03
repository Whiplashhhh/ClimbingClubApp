# Belay

> Nom de code provisoire.

PWA multi-tenant pour clubs d'escalade : chaque club (organisation) gère ses membres, créneaux,
murs et voies ; les grimpeurs enregistrent leurs séances ; présidents et moniteurs diffusent infos
et sondages ciblés.

Docs : [spec produit](docs/PRODUCT_SPEC.md) · [roadmap](docs/ROADMAP.md) · [ADRs](docs/adr/) ·
[état d'avancement](docs/STATUS.md) · [déploiement auto-hébergé](docs/DEPLOY.md)

## Stack

- **Backend** : Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, Spring Security + Spring Session
  (Redis), springdoc-openapi, Maven.
- **Frontend** : Nuxt 4 (Vue 3, SSR), TypeScript strict, Pinia, Tailwind CSS v4, PWA
  (`@vite-pwa/nuxt`), pnpm, Node 24.
- **Infra** : Docker Compose (PostgreSQL, Redis, MinIO, back, front).

## Démarrage rapide

```bash
cp .env.example .env          # adapter les mots de passe
docker compose up --build     # stack complète
# front  : http://localhost:3000
# back   : http://localhost:8080  (health : /actuator/health, Swagger UI : /swagger-ui)
# minio  : http://localhost:9001  (console)
```

## Développement

Prérequis : Java 21, Node 24 (`nvm use`), pnpm (`corepack enable`), Docker.

```bash
# Services d'infra seuls
docker compose up postgres redis minio

# Backend
cd backend
./mvnw spring-boot:run        # http://localhost:8080
./mvnw verify                 # tests (Testcontainers) + format + export openapi.json
./mvnw spotless:apply         # formatage

# Frontend
pnpm install
pnpm --dir frontend dev       # http://localhost:3000 (proxy /api → :8080)
pnpm --dir frontend lint
pnpm --dir frontend typecheck
pnpm --dir frontend test
pnpm --dir frontend build
```

## Contrat OpenAPI (contract-first)

Le back est la source de vérité. `./mvnw verify` régénère `openapi.json` à la racine ;
`pnpm --dir frontend gen:api` régénère les types TypeScript (`frontend/app/types/api.ts`).
Les deux fichiers sont committés et vérifiés en CI. Aucun type d'API écrit à la main côté front.

## Workflow

Une branche par tranche verticale, Conventional Commits (commitlint), PR avec CI verte,
squash-merge dans `main`. Voir `CLAUDE.md` pour les règles complètes.
