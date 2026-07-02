# ADR 0005 — Stockage objet MinIO (S3), URLs présignées

**Statut** : accepté — 2026-07-03

## Contexte
Photos (logos, posters, murs, voies) : données binaires potentiellement volumineuses, exigences de
sécurité upload (CLAUDE.md §10) : hors racine web, noms regénérés, accès à durée limitée.

## Décision
- **MinIO** (S3-compatible) en docker-compose pour le dev ; API S3 standard (AWS SDK v2) donc
  portable vers n'importe quel S3 en prod.
- Upload **via le back** (multipart) : validation du type MIME **par contenu** (Apache Tika),
  taille limitée, clé objet regénérée (UUID, préfixée par `org/{organizationId}/…`).
- Lecture via **URLs présignées** à durée limitée générées par le back après contrôle
  tenancy/rôle. Aucun bucket public.

## Alternatives rejetées
- **BLOB en Postgres** : gonfle la DB et les backups, pas de streaming simple.
- **Filesystem local** : pas de présignature, path-traversal à gérer à la main, non portable.

## Conséquences
Un bucket unique `belay` avec préfixe par org ; les credentials MinIO passent par variables
d'environnement.
