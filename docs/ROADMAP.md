# Belay — Roadmap

> Une case cochée = tranche mergée dans `main` avec CI verte.

## Phase 0 — Fondations
- [ ] Monorepo `/backend` + `/frontend`, `.nvmrc` (Node 24)
- [ ] docker-compose : Postgres 16, Redis, MinIO, back, front
- [ ] CI GitHub Actions (build/test/lint back + front, jobs séparés)
- [ ] springdoc + export `openapi.json` ; `openapi-typescript` (`pnpm gen:api`)
- [ ] Coquille Nuxt 4 + PWA installable (`@vite-pwa/nuxt`)
- [ ] Health check `/actuator/health`
- [ ] Flyway baseline
- [ ] README, hooks Git (Husky / lint-staged / commitlint), Spotless

**DoD : `docker compose up` lève la stack, CI verte, PWA installable.**

## Phase 1 — Comptes / orgs / rôles / sécurité
- [ ] Inscription / connexion, mots de passe bcrypt
- [ ] Sessions serveur Redis (Spring Session), cookie `HttpOnly`/`Secure`/`SameSite`, CSRF
- [ ] Création d'organisation ; rattachement utilisateur ↔ org (une seule org) + rôles
- [ ] Invitations / validation des membres `PENDING`
- [ ] Autorisation serveur par rôle sur chaque endpoint
- [ ] **Test anti-IDOR inter-org**
- [ ] En-têtes de sécurité, `.env.example`

**DoD : un membre de A ne peut rien voir de B (prouvé par test).**

## Phase 2 — Fil d'accueil (infos)
- [ ] Post type INFO / CANCELLATION / POSTER, audience ORG vs COACH_STUDENTS
- [ ] Agrégation du fil par utilisateur
- [ ] Upload de poster (MinIO, validation MIME par contenu, URLs signées)
- [ ] UI accueil (fil)
- [ ] Tests e2e du chemin critique
- [ ] Skill `.claude/skills/full-stack-slice/SKILL.md` (recette tranche verticale)

## Phase 3 — Créneaux & groupes
- [ ] Slots récurrents, rattachement des membres, groupe dérivé
- [ ] Annulation / décalage + notification in-app

## Phase 4 — Voies & murs
- [ ] Secteurs, voies (nom/cotation/type/photo), rattachement, affichage
- [ ] Overlay d'annotations en lecture

## Phase 5 — Séances & social
- [ ] Séances + ascensions (note, prise max, temps, assureur)
- [ ] Amis, fil d'amis, confidentialité

## Phase 6 — Sondages & messagerie
- [ ] Polls (création / réponse / résultats, mêmes audiences)
- [ ] MP élève ↔ moniteur

## Phase 7 — Finitions PWA
- [ ] Offline de base, notifications push web
- [ ] Éditeur d'annotations de prises
- [ ] Itinéraire (deep-link maps)
- [ ] Rate limiting (Redis) sur les endpoints sensibles
- [ ] Export / suppression RGPD
