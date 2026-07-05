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
- [x] Slots récurrents, rattachement des membres, groupe dérivé (+ résolution de l'audience
      COACH_STUDENTS du fil via les rattachements — A-005 refermée)
- [x] Annulation / décalage + notification in-app

## Phase 4 — Voies & murs
- [x] Secteurs, voies (nom/cotation/type/photo), rattachement, affichage
- [x] Overlay d'annotations en lecture (l'éditeur reste en Phase 7)

## Phase 5 — Séances & social
- [x] Séances + ascensions (note, prise max, temps, assureur — assureur en nom libre, ami en 5B)
- [x] Amis, fil d'amis, confidentialité (5B) — graphe d'amis org-scopé, séances FRIENDS visibles
      des amis acceptés, assureur choisi parmi les membres actifs

## Phase 6 — Sondages & messagerie
- [x] Polls (création / réponse / résultats, mêmes audiences que le fil — choix unique, clôture optionnelle)
- [x] MP élève ↔ moniteur (fils privés, non-lus, notification in-app — relation dérivée d'un créneau)

## Phase 7 — Finitions PWA
- [ ] Offline de base, notifications push web
- [ ] Page profil (infos du compte, changement de mot de passe)
- [ ] Éditeur d'annotations de prises
- [ ] Itinéraire (deep-link maps)
- [ ] Rate limiting (Redis) sur les endpoints sensibles
- [ ] Export / suppression RGPD
