# ADR 0004 — Tenancy : une org par utilisateur, colonne `organization_id` partout

**Statut** : accepté — 2026-07-03

## Contexte
Multi-tenant par organisation ; l'isolation inter-org est un invariant de sécurité. Un utilisateur
appartient à exactement une organisation.

## Décision
- **Shared database, shared schema** : chaque table multi-tenant porte `organization_id NOT NULL`
  (FK vers `organization`), y compris `user` (nullable uniquement le temps de l'onboarding,
  cf. A-002).
- **Interdiction structurelle des requêtes non scopées** : les repositories n'exposent pas de
  finder global ; toute méthode de lecture/écriture multi-tenant prend `organizationId` en
  paramètre (dérivé de la session côté service, jamais du client). Les services chargent
  l'utilisateur courant depuis la session et scopent chaque appel.
- Contrôle d'accès : tenancy **et** rôle vérifiés côté serveur sur chaque endpoint. Une ressource
  d'une autre org répond **404** (pas 403) pour ne pas révéler son existence.
- Chaque tranche verticale inclut un **test anti-IDOR** : un membre de l'org A ne peut ni lire ni
  écrire les données de l'org B.

## Alternatives rejetées
- **Schéma par tenant / DB par tenant** : sur-ingénierie pour des clubs de taille modeste ;
  complexité migrations × N.
- **Hibernate `@Filter` global** : envisagé, mais le filtre doit être activé par session Hibernate
  et se contourne silencieusement s'il est oublié ; le scoping explicite par paramètre est
  vérifiable en revue et en test. Réévaluable si le nombre de requêtes explose.

## Conséquences
Un peu de verbosité dans les repositories (`findByIdAndOrganizationId`…), compensée par une
isolation lisible, testable et sans magie.
