# État d'avancement — Belay

> Mis à jour : 2026-07-03 (fin de la session « Phase 2 — fil d'accueil »).

## Fait (mergé dans `main`, CI verte)

- **Docs & décisions** (PR #1) : PRODUCT_SPEC, ROADMAP, ADR 0001-0005, ASSUMPTIONS.
- **Phase 0 — Fondations** (PR #2) : monorepo back/front, docker-compose (Postgres 16, Redis,
  MinIO, back, front), CI GitHub Actions (jobs back et front séparés), contract-first
  (`openapi.json` exporté par `./mvnw verify`, types front via `pnpm gen:api`), PWA installable,
  Flyway, Spotless, Husky/commitlint.
- **Phase 1 — Comptes / orgs / rôles / sécurité** (PR #3) : inscription (création de club ou
  demande d'adhésion par code), connexion, sessions serveur Redis (cookie `HttpOnly`/`SameSite`,
  `Secure` en prod), CSRF SPA, rôles OWNER/ADMIN/COACH/MEMBER + statut PENDING/ACTIVE/DISABLED,
  approbation des membres, autorisation par rôle côté serveur, **test anti-IDOR inter-org**.
- **Phase 2 — Fil d'accueil** (cette tranche) :
  - `Post` (INFO / CANCELLATION / POSTER) avec audience ORG ou COACH_STUDENTS (migration V3) ;
  - matrice de publication côté serveur : ORG → OWNER/ADMIN, COACH_STUDENTS → COACH ;
    suppression par l'auteur ou un admin ; agrégation du fil scopée à l'organisation ;
  - upload d'affiche : MIME vérifié par octets magiques (JPEG/PNG/WebP), 5 Mo max, clé objet
    regénérée, bucket MinIO privé, URLs signées 15 min (ADR 0006) ;
  - UI : page d'accueil = fil (cartes, images signées, « voir plus »), composeur pour
    admins/moniteurs, gestion des membres déplacée sur `/members`, nav dans l'en-tête ;
  - tests : intégration API (audiences, 403, anti-IDOR posts, upload accepté/rejeté) + vitest
    (visibilité du composeur par rôle, rendu du fil, état PENDING) ;
  - skill `.claude/skills/full-stack-slice/SKILL.md` (recette des tranches suivantes) ;
  - ménage : Thymeleaf/openhtmltopdf retirés de la stack (A-006).

## En cours

- Rien — la tranche Phase 2 se termine avec cette PR.

## Prochaines étapes (dans l'ordre de la ROADMAP)

1. **Phase 3 — Créneaux & groupes** : slots récurrents, rattachement des membres, groupe dérivé,
   annulation/décalage + notification in-app. Ceci activera la vraie résolution de l'audience
   COACH_STUDENTS (A-005) : brancher `PostRepository.findFeed` sur `SlotMembership`.
2. **Phase 4 — Voies & murs** (secteurs, voies, overlay d'annotations en lecture).
3. Phases 5-7 (séances & social, sondages & messagerie, finitions PWA).

## Points d'attention

- **A-005** : tant que la Phase 3 n'existe pas, un post COACH_STUDENTS n'est visible que par son
  auteur — comportement voulu, mais à rebrancher dès que `SlotMembership` existe (le point
  d'entrée est la requête `PostRepository.findFeed`).
- **URLs signées** : la signature S3 inclut l'hôte → `S3_PUBLIC_ENDPOINT` doit être l'URL de
  MinIO vue du navigateur (compose : `http://localhost:9000` par défaut).
- **E2e navigateur** : pas encore de Playwright (A-008) ; le chemin critique est couvert par les
  tests d'intégration API + vitest.
- Lint front : 10 warnings `vue/html-self-closing` préexistants (conflit avec Prettier sur les
  éléments void) — zéro erreur ; à trancher un jour dans la config ESLint.
