# 0006 — Uploads médiés par le backend, lecture par URLs signées

Date : 2026-07-03 · Statut : accepté — **amendé le 2026-07-04** : la lecture par URLs
signées est remplacée par un endpoint applicatif `GET /api/media/**` (authentifié, scopé par
organisation via le préfixe de clé). Raisons : les URLs signées embarquent l'hôte MinIO
(`S3_PUBLIC_ENDPOINT`), fragile selon le réseau du client (images cassées en déploiement
Tailscale), et elles fuitent hors session. MinIO n'est plus exposé au réseau du tout.

## Contexte

La Phase 2 introduit l'upload d'images (affiches du fil). CLAUDE.md §10 impose : MIME vérifié
par contenu, taille limitée, nom regénéré, stockage objet hors racine web, URLs signées.
Deux options : (a) le client upload directement vers MinIO via une URL signée PUT, ou
(b) le client envoie le fichier au backend, qui valide puis écrit dans MinIO.

## Décision

**Upload médié par le backend** (option b) :

- `POST /api/posts/poster` (multipart) → le backend lit les octets, détecte le format par
  **octets magiques** (JPEG/PNG/WebP uniquement), rejette le reste (400), limite la taille via
  `spring.servlet.multipart.max-file-size` (5 MB → 413) ;
- la clé objet est **regénérée** (`posts/{organizationId}/{uuid}.{ext}`) — le nom de fichier
  client n'est jamais utilisé (anti path-traversal) ;
- le bucket est privé ; la lecture passe par des **URLs signées GET à durée limitée**
  (`belay.storage.presign-ttl`, 15 min par défaut), régénérées à chaque lecture du fil ;
- deux endpoints configurables : `S3_ENDPOINT` (réseau interne, ex. `http://minio:9000`) et
  `S3_PUBLIC_ENDPOINT` (vu du navigateur, ex. `http://localhost:9000`) car la signature S3
  inclut l'hôte.

## Conséquences

- La validation de contenu est garantie côté serveur quel que soit le client ; aucune écriture
  non validée ne peut atteindre le bucket (contrairement au PUT signé direct).
- Le fichier transite par le backend : acceptable pour des images ≤ 5 MB ; à reconsidérer
  (PUT signé + validation asynchrone) si des fichiers lourds arrivent (vidéo…).
- Client S3 : AWS SDK v2 (standard, MinIO-compatible), path-style activé.
