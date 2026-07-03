---
name: full-stack-slice
description: Recette pour ajouter une fonctionnalité de bout en bout (DB → API → types front → UI → tests) dans Belay, en respectant le contract-first, l'isolation multi-tenant et la Definition of Done. À utiliser au début de chaque tranche verticale (Phase 3+).
---

# Ajouter une tranche verticale complète

Référence vivante : la tranche « fil d'accueil » (Phase 2) — package `app.belay.post`,
`frontend/app/pages/index.vue`, `FeedIntegrationTest`. Copie ses patterns, pas des abstractions.

## 0. Préparation

1. Relis `docs/ROADMAP.md` et la section concernée de `docs/PRODUCT_SPEC.md`.
2. Branche : `feat/<slice>` depuis `main` à jour. Une PR par tranche.
3. Décision structurante → ADR (`docs/adr/`) ; hypothèse → `docs/ASSUMPTIONS.md` (numérotée A-xxx).

## 1. Backend (source de vérité du contrat)

1. **Migration Flyway** `backend/src/main/resources/db/migration/V<n>__<desc>.sql` :
   - `organization_id UUID NOT NULL REFERENCES organization (id)` sur toute table multi-tenant
     + index `(organization_id, …)` pour la requête de liste ;
   - contraintes `CHECK` pour les enums (`VARCHAR(20)` + liste de valeurs) ;
   - jamais `ddl-auto=update` ; `V<n>` = numéro suivant, jamais réutilisé.
2. **Entité JPA** (package `app.belay.<domain>`) : miroir de la table, associations
   `@ManyToOne(fetch = LAZY, optional = false)`, timestamps via `@PrePersist/@PreUpdate`.
   Jamais exposée dans un contrôleur.
3. **Repository** : chaque méthode prend `organizationId` (dérivé du principal, jamais du client).
   Lookup unitaire = `findByIdAndOrganizationId(...)` → absent = 404 (existence masquée, pas 403).
4. **Service** : logique métier + autorisation fine (rôle/audience) via
   `AccessDeniedException` (→ 403 par `RestExceptionHandler`). Erreurs métier :
   `NotFoundException`, `ConflictException`, `IllegalArgumentException` (400).
5. **Contrôleur** (`/api/...`) : DTO records dans `<domain>/dto` avec Bean Validation ;
   `@PreAuthorize("hasAnyRole(...)")` pour le filtre grossier par rôle ;
   `@AuthenticationPrincipal UserPrincipal` pour le scope ; annotations
   `@Tag`/`@Operation`/`@ApiResponse` (contrat en anglais).
6. **Upload de fichier ?** Passe par `StorageService` (validation par octets magiques, clé
   regénérée `posts/{orgId}/uuid.ext`, URL signée en lecture) — voir ADR 0006.

## 2. Contrat & types front

```bash
cd backend && ./mvnw verify          # tests + régénère ../openapi.json (OpenApiExportTest)
pnpm --dir frontend gen:api          # régénère frontend/app/types/api.ts
```
Committe `openapi.json` **et** `app/types/api.ts` — la CI échoue s'ils divergent.
Aucun type d'API écrit à la main côté front.

## 3. Frontend

1. **Schémas Zod** `app/schemas/<domain>.ts` : verrouillés sur le contrat via
   `satisfies z.ZodType<components['schemas']['X']>` ; types applicatifs par `z.infer`.
   (Les champs `null` sont omis du JSON — Jackson `non-null` — donc `.optional()`, pas `.nullable()`.)
2. **Appels API** : `apiFetch` (CSRF + cookies SSR gérés) + `feedXxxSchema.parse(...)` sur les
   payloads critiques. Chargement initial dans `useAsyncData`.
3. **Composants** : petits, typés (`defineProps<...>`/`defineEmits<...>`), dans
   `app/components/<domain>/`. Pages minces. Le front **masque** selon le rôle, le serveur **refuse**.
4. Multipart : `FormData` avec part JSON `meta` (`new Blob([JSON.stringify(...)], { type: 'application/json' })`) + part fichier.

## 4. Tests (preuves, pas cérémonie)

- **Backend — un test d'intégration par tranche** (`@SpringBootTest` + Testcontainers, via
  `ApiActor`) couvrant : le chemin critique bout en bout, la matrice de permissions (403),
  et **l'anti-IDOR inter-org** (une ressource de l'org B → 404, lecture ET écriture).
- **Frontend — vitest** (`tests/<domain>.nuxt.spec.ts`) : rendu par rôle (`useAuthStore()` + état
  direct), `mockNuxtImport('apiFetch', ...)`. Démonter le wrapper entre tests si la page
  utilise une clé `useAsyncData` fixe.

## 5. Definition of Done (avant la PR)

```bash
cd backend && ./mvnw spotless:apply && ./mvnw verify
pnpm --dir frontend lint && pnpm --dir frontend typecheck && pnpm --dir frontend test && pnpm --dir frontend build
git diff --exit-code openapi.json frontend/app/types/api.ts   # rien d'oublié
```
README/docs à jour si commandes ou env changent (`.env.example`, `docker-compose.yml`).
PR (Conventional Commits), CI verte, squash-merge, coche `docs/ROADMAP.md`, mets à jour `docs/STATUS.md`.
