# ADR 0002 — Monorepo unique `/backend` + `/frontend`

**Statut** : accepté — 2026-07-03

## Contexte
Un seul produit, une petite équipe (un agent), un contrat OpenAPI partagé entre back et front.

## Décision
Un seul dépôt Git contenant `/backend` (Spring Boot 3.5 / Java 21 / Maven) et `/frontend`
(Nuxt 4 / pnpm), plus `docker-compose.yml`, `openapi.json` à la racine et `/docs`. CI GitHub
Actions avec jobs séparés back/front filtrés par chemins.

## Conséquences
- Le contrat `openapi.json` est versionné au même endroit que ses producteur et consommateur :
  une PR = une tranche verticale cohérente.
- Pas de synchronisation multi-dépôts ; en contrepartie, la CI doit filtrer par chemins pour
  rester rapide.
