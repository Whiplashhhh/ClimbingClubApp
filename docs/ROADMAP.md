# Belay — Roadmap

> Une case cochée = tranche mergée dans `main` avec CI verte.

## Phase 0 — Fondations
- [x] Monorepo `/backend` + `/frontend`, `.nvmrc` (Node 24)
- [x] docker-compose : Postgres 16, Redis, MinIO, back, front
- [x] CI GitHub Actions (build/test/lint back + front, jobs séparés)
- [x] springdoc + export `openapi.json` ; `openapi-typescript` (`pnpm gen:api`)
- [x] Coquille Nuxt 4 + PWA installable (`@vite-pwa/nuxt`)
- [x] Health check `/actuator/health`
- [x] Flyway baseline
- [x] README, hooks Git (Husky / lint-staged / commitlint), Spotless

**DoD : `docker compose up` lève la stack, CI verte, PWA installable.**

## Phase 1 — Comptes / orgs / rôles / sécurité
- [x] Inscription / connexion, mots de passe bcrypt
- [x] Sessions serveur Redis (Spring Session), cookie `HttpOnly`/`Secure`/`SameSite`, CSRF
- [x] Création d'organisation ; rattachement utilisateur ↔ org (une seule org) + rôles
- [x] Invitations / validation des membres `PENDING`
- [x] Autorisation serveur par rôle sur chaque endpoint
- [x] **Test anti-IDOR inter-org**
- [x] En-têtes de sécurité, `.env.example`

**DoD : un membre de A ne peut rien voir de B (prouvé par test).**

## Phase 2 — Fil d'accueil (infos)
- [x] Post type INFO / CANCELLATION / POSTER, audience ORG vs COACH_STUDENTS
- [x] Agrégation du fil par utilisateur
- [x] Upload de poster (MinIO, validation MIME par contenu, URLs signées)
- [x] UI accueil (fil)
- [x] Tests e2e du chemin critique (intégration API — navigateur : voir A-008)
- [x] Skill `.claude/skills/full-stack-slice/SKILL.md` (recette tranche verticale)

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
