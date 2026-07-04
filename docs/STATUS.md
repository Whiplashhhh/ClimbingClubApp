# État d'avancement — Belay

> Mis à jour : 2026-07-04 (Phase 3 partie A : créneaux & groupes).

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

## Fait aussi (suite au test terrain sur iPhone + Phase 3 partie A)

- **Déploiement auto-hébergé** documenté et testé (`docs/DEPLOY.md`) : serveur perso via
  Tailscale, ports custom committés par Willem sur `main` (front : 3003).
- **Correctifs terrain** : `app.vue` sans `NuxtLayout` (en-tête/déconnexion invisibles — corrigé,
  en-tête responsive), bannière de demandes d'adhésion sur le fil, zoom iOS au focus des champs
  (16px imposés).
- **Phase 3 partie A — créneaux & groupes** : `Slot` hebdomadaire (jour/heure/durée/moniteur) +
  `SlotMembership` (V4), API scopée avec matrice (admins partout, moniteur sur ses créneaux,
  coach éligible = ACTIVE non-MEMBER — A-009), **le fil résout enfin COACH_STUDENTS via les
  rattachements (A-005 refermée)**, page `/slots` (planning, création, gestion du groupe),
  sélecteur de rôle sur `/members`, tests intégration + vitest.

## En cours

- Rien — la partie A de la Phase 3 se termine avec cette PR.

## Prochaines étapes (dans l'ordre de la ROADMAP)

1. **Phase 3 partie B** : annulation / décalage d'une séance de créneau (à une date donnée) →
   notifications in-app pour le groupe (cloche + pastille) + post CANCELLATION automatique.
2. **Phase 4 — Voies & murs** (secteurs, voies, overlay d'annotations en lecture).
3. Phases 5-7 (séances & social, sondages & messagerie, finitions PWA).

## Points d'attention

- **URLs signées** : la signature S3 inclut l'hôte → `S3_PUBLIC_ENDPOINT` doit être l'URL de
  MinIO vue du navigateur (compose : `http://localhost:9000` par défaut).
- **E2e navigateur** : pas encore de Playwright (A-008) ; le chemin critique est couvert par les
  tests d'intégration API + vitest.
- Les ports publiés sont personnalisés directement dans `docker-compose.yml` sur `main` (choix
  de Willem pour son serveur) ; l'alternative `docker-compose.override.yml` est documentée dans
  DEPLOY.md si on veut revenir aux défauts dans le repo.
