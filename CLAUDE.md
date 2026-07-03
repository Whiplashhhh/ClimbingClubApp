# CLAUDE.md — Belay (PWA multi-clubs d'escalade)

> `belay` est un nom de code provisoire. Renomme-le si besoin, mais reste cohérent partout.
> Ce fichier est ta **source de vérité opérationnelle**. Il est lu à chaque session.
> La spec produit détaillée et le plan sont dans `docs/PRODUCT_SPEC.md` et `docs/ROADMAP.md` :
> lis-les **avant** d'écrire du code.

---

## 1. Ton rôle

Tu es l'ingénieur principal, seul aux commandes, en mode autonome. Tu conçois, codes,
testes, documentes, commits et ouvres des PR toi-même. Tu privilégies **la qualité et la
sécurité à la vitesse**. Tu livres des tranches verticales fonctionnelles plutôt que des
squelettes horizontaux à moitié faits.

## 2. Le produit en une phrase

PWA multi-tenant pour clubs d'escalade : chaque club (organisation) gère ses membres,
créneaux, murs et voies ; les grimpeurs enregistrent leurs séances ; présidents et
moniteurs diffusent infos et sondages ciblés. **L'isolation entre organisations est un
invariant de sécurité, pas une option.**

## 3. Stack (versions épinglées — ne dévie pas sans ADR)

**Back** : Java 21, Spring Boot 3.5.x, Maven (`./mvnw`), PostgreSQL 16+, Flyway, Spring Data
JPA/Hibernate, Spring Security, springdoc-openapi, Bean Validation, Actuator, Spotless +
Palantir Java Format, JUnit 5 + Testcontainers, Docker.
(Thymeleaf/openhtmltopdf retirés : aucun besoin de génération PDF dans la spec — voir A-006.)

**Front** : Nuxt 4 (Vue 3, SSR), TypeScript `strict`, Pinia, `useFetch`/`$fetch`,
`openapi-typescript` + Zod, Tailwind CSS v4 + shadcn-vue (Reka UI) + Lucide, VueUse,
`@vite-pwa/nuxt`, ESLint + Prettier, Vitest + Vue Test Utils + Playwright + `@nuxt/test-utils`,
Husky + lint-staged + commitlint, Node 24 (`.nvmrc`), **pnpm**.

**Infra** : Docker Compose (Postgres + Redis + MinIO S3 + back + front). Redis fait partie du
squelette : il porte les **sessions serveur** dès la Phase 1, et servira aussi au rate limiting.

## 4. Structure du monorepo (un seul dépôt)

```
/backend            Spring Boot
/frontend           Nuxt 4
/docs               PRODUCT_SPEC.md, ROADMAP.md, adr/, ASSUMPTIONS.md, BLOCKERS.md
/.github/workflows  CI (back + front séparés)
docker-compose.yml  Postgres, Redis, MinIO, back, front
openapi.json        contrat généré par le back, consommé par le front
README.md
```

## 5. Commandes canoniques

- Back : `./mvnw verify`, `./mvnw spring-boot:run`, `./mvnw spotless:apply`
- Front : `pnpm install`, `pnpm dev`, `pnpm build`, `pnpm test`, `pnpm test:e2e`, `pnpm lint`, `pnpm typecheck`
- Contrat : le back expose `/v3/api-docs` → export vers `openapi.json` → `pnpm gen:api` (openapi-typescript) régénère les types front.
- Full stack local : `docker compose up`

Dès que tu figes ou changes une commande, mets à jour cette section **et** le README.

## 6. Contract-first (impératif)

Le **back est la source de vérité du contrat**. À chaque ajout/modif d'endpoint :
1. annote correctement pour springdoc (schémas, statuts, exemples) ;
2. régénère `openapi.json` ;
3. régénère les types front (`pnpm gen:api`) ;
4. valide les payloads critiques côté front avec **Zod**.
   Aucun type d'API écrit à la main côté front.

## 7. Conventions de code

- **Identifiants, noms de fichiers, messages de commit, contrat OpenAPI : en anglais.**
  Commentaires et docs : français accepté.
- TS : `strict`, pas de `any` (préfère `unknown` + narrowing), pas de `// @ts-ignore` sans justification.
- Java : architecture en couches `controller → service → repository` ; entités JPA ≠ DTO
  (jamais d'entité exposée dans un contrôleur) ; validation Jakarta sur les DTO d'entrée.
- Pas de logique métier dans les contrôleurs. Pas de requêtes non scopées par organisation.
- Migrations : **toute** évolution de schéma passe par un script Flyway versionné (jamais `ddl-auto=update`).
- Composants front : petits, typés, testables ; état serveur via `useFetch`, état UI via Pinia.

## 8. Workflow Git (IMPÉRATIF)

- Une **branche par tranche verticale** : `feat/...`, `chore/...`, `fix/...`.
- **Conventional Commits** (respectés par commitlint). Commits petits et atomiques.
- Une **PR par tranche**, avec description : objectif, ce qui est fait, comment tester, captures/risques.
- **CI verte obligatoire** avant merge. Squash-merge dans `main`, puis suppression de la branche.
- `main` toujours déployable. **Jamais** de `push --force` sur `main`. **Jamais** de secret commité.
- Utilise `gh` pour créer les PR. Après merge, mets à jour `docs/ROADMAP.md` (case cochée).

## 9. Definition of Done (par tranche)

Typecheck ✅ · lint ✅ · format ✅ · tests unitaires + un test d'intégration/e2e sur le chemin
critique ✅ · build back + front ✅ · `openapi.json` et types front régénérés ✅ · README/docs à
jour ✅ · aucun secret, aucun TODO de sécurité laissé ouvert.

## 10. Sécurité — NON négociable

- **Isolation multi-tenant** : *chaque* requête de données est scopée à l'organisation du membre
  authentifié. Vérifie l'appartenance **et** le rôle sur *chaque* endpoint. Écris un test qui
  prouve qu'un membre de l'org A ne peut pas lire/écrire les données de l'org B (anti-IDOR).
- **AuthN/Z** : mots de passe hashés (bcrypt/argon2), **sessions serveur adossées à Redis**
  (Spring Session), cookie de session `HttpOnly`/`Secure`/`SameSite`, expiration + invalidation à
  la déconnexion ; **aucun** identifiant de session en `localStorage` ; CSRF géré. Autorisation par
  rôle **côté serveur** (le front ne fait que masquer).
- **Validation** : tout input validé (Bean Validation côté back, Zod côté front). Rejet par défaut.
- **Upload de photos** : type MIME vérifié par contenu (pas seulement l'extension), taille limitée,
  nom regénéré (anti path-traversal), stockage objet (MinIO/S3) hors racine web, URLs signées/à durée limitée.
- **Secrets** : uniquement via variables d'environnement (`.env` gitignore + `.env.example`).
  Identifiants DB, Redis et MinIO jamais en dur, jamais loggués.
- **RGPD** (app française, données perso : identités, messages, graphe d'amis, séances, photos) :
  minimisation, export et suppression de compte, pas de PII dans les logs, base légale documentée.
- Réfléchis OWASP Top 10 à chaque endpoint. Headers de sécurité (CSP, HSTS, etc.) côté Nuxt.
  Redis étant présent, active le rate limiting sur les endpoints sensibles (login, inscription) dès utile.

## 11. Garde-fous & anti-patterns

- **Pas de sur-ingénierie.** Pas de microservices, pas d'abstraction spéculative, pas de
  framework maison. La solution la plus simple qui satisfait la spec et la DoD gagne.
- **Tranches verticales**, pas de gros PR fourre-tout. Si une PR dépasse ~20 fichiers non générés,
  découpe.
- **Décisions d'archi** notables → un ADR court dans `docs/adr/NNNN-titre.md`.
- **Hypothèses** prises faute d'info → `docs/ASSUMPTIONS.md`, puis tu continues avec le défaut le
  plus raisonnable. Tu ne bloques pas et tu n'inventes pas de fait.
- **Blocage réel** (après 2-3 tentatives) → `docs/BLOCKERS.md` : contexte, ce que tu as essayé,
  options proposées. Tu prends l'option la moins risquée pour avancer, tu la signales dans la PR.

## 12. Rythme de session autonome

Boucle : (1) relis ROADMAP → (2) choisis la prochaine tranche → (3) branche → (4) code + tests →
(5) DoD → (6) PR → (7) CI verte → (8) merge → (9) coche la ROADMAP → (10) recommence.
En fin de session (ou si le budget se réduit), termine proprement la tranche en cours, ne laisse
pas `main` cassé, et écris un **rapport d'état** dans `docs/STATUS.md` (fait / en cours / prochaines
étapes / points d'attention).