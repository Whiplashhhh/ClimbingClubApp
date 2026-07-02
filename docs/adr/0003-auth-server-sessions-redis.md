# ADR 0003 — Authentification par sessions serveur (Spring Session + Redis)

**Statut** : accepté — 2026-07-03

## Contexte
PWA first-party (front et back même domaine en production), besoin d'invalidation immédiate
(déconnexion, désactivation d'un membre), exigences CLAUDE.md §10 : aucun identifiant de session
en `localStorage`.

## Décision
- Sessions **serveur** stockées dans **Redis** via Spring Session (`spring-session-data-redis`).
- Cookie de session `HttpOnly`, `Secure` (en prod), `SameSite=Lax`.
- Login/logout via endpoints JSON (`/api/auth/login`, `/api/auth/logout`) ; logout invalide la
  session côté Redis.
- **CSRF activé** (Spring Security, `CookieCsrfTokenRepository` non-HttpOnly pour que le front
  lise le token et l'envoie en header `X-XSRF-TOKEN`).
- Mots de passe hashés **bcrypt** (encodeur par défaut Spring Security, `{bcrypt}`).

## Alternatives rejetées
- **JWT stateless** : pas d'invalidation immédiate, tentation du `localStorage`, complexité de
  rotation/refresh inutile pour une app first-party.

## Conséquences
Redis est un composant obligatoire dès la Phase 1 (déjà dans le compose). Il servira aussi au rate
limiting (Phase 7).
