# Déployer Belay sur un serveur perso (via Tailscale)

Guide pour un déploiement de **test** sur un serveur Linux accessible par Tailscale, pilotable
entièrement depuis un téléphone (SSH). Pas de domaine public, pas de HTTPS à gérer : le tailnet
chiffre le transport et rien n'est exposé à Internet.

## Prérequis

- Un serveur Linux avec **Docker + le plugin compose** (`docker compose version`) et **git**.
- **Tailscale** actif sur le serveur **et** sur le téléphone (app iOS/Android, connectés au même
  tailnet). Récupère le nom du serveur : `tailscale status` (ex. `monserveur.tailnet-xyz.ts.net`),
  utilisé partout ci-dessous comme `<TS_HOST>`.
- Depuis un iPhone : une app SSH — **Termius** (gratuit, simple) ou **Blink Shell**.

## Installation (une fois)

```bash
git clone https://github.com/Whiplashhhh/ClimbingClubApp.git
cd ClimbingClubApp
cp .env.example .env
nano .env
```

Dans `.env`, change les mots de passe :

```bash
POSTGRES_PASSWORD=un-vrai-mot-de-passe
MINIO_ROOT_PASSWORD=un-autre-vrai-mot-de-passe
```

(Les images sont servies par l'application elle-même via `/api/media` : aucune URL MinIO à
configurer, quel que soit l'hôte.)

Puis :

```bash
docker compose up -d --build
docker compose ps          # tout doit finir "healthy" / "running"
```

Premier build : plusieurs minutes (Maven + pnpm). Ensuite, depuis le téléphone (Tailscale actif),
ouvre **`http://<TS_HOST>:3000`** dans le navigateur et crée un club.

### Installer la PWA sur iPhone

iOS ne propose **pas** de bouton d'installation automatique (contrairement à Android) — ça passe
par le menu partage :

- **Safari** : bouton Partager (carré + flèche) → **« Sur l'écran d'accueil »**.
- **Chrome iOS** : menu **⋯** → **Partager** → **« Sur l'écran d'accueil »**.

### Changer les ports publiés (conflit avec une autre appli)

Ne modifie pas `docker-compose.yml` (ça bloquerait les `git pull`). Crée un
`docker-compose.override.yml` à côté (non versionné, fusionné automatiquement par compose) :

```yaml
services:
  frontend:
    ports: !override
      - "3001:3000"
  minio:
    ports: !override
      - "9002:9000"
      - "127.0.0.1:9001:9001"
```

> `!override` (compose ≥ 2.24) **remplace** la liste au lieu de l'ajouter — sans lui, l'ancien
> port resterait publié en plus du nouveau. Si tu as déjà modifié `docker-compose.yml` :
> reporte tes ports dans l'override puis `git checkout docker-compose.yml`.

Les ports internes (à droite des mappings) ne changent jamais. MinIO n'a plus besoin d'être
joignable depuis le réseau : les images passent par l'application.

## Mettre à jour après un merge dans `main`

```bash
cd ClimbingClubApp
git pull
docker compose up -d --build
```

(C'est le « CD » pour l'instant : la CI GitHub Actions valide chaque PR, le déploiement est ces
deux commandes. À automatiser plus tard si le besoin se confirme.)

## Dépannage

```bash
docker compose ps                    # état des services
docker compose logs -f backend      # logs du back (démarrage, migrations Flyway)
docker compose logs -f frontend
curl http://localhost:8080/actuator/health   # doit répondre {"status":"UP"}
docker compose down                  # tout arrêter (les données persistent : volumes)
docker compose down -v               # tout arrêter ET effacer les données
```

- **Les images ne s'affichent pas** → vérifier `docker compose logs backend` (MinIO joignable ?)
  et que le volume `miniodata` n'a pas été effacé (`down -v`) alors que la base a survécu.
- **Le front répond mais l'API échoue** → `docker compose logs backend` ; le back attend
  Postgres/Redis/MinIO healthy avant de démarrer.

## Exposition réseau (sécurité)

Ports ouverts sur le serveur : **le front** et **l'API (8080)**. Postgres, Redis et MinIO
(9000/9001) sont liés à `127.0.0.1` — les images passent par l'application (`/api/media`),
authentifiées par la session. Tant que le serveur n'a pas de port forwarding public, seuls
les appareils du tailnet voient l'application.

- Le cookie de session reste `Secure=false` ici car l'accès est en HTTP sur le tailnet (déjà
  chiffré par WireGuard). **Pour une vraie mise en production publique** : HTTPS obligatoire
  (reverse proxy type Caddy/Traefik), `SESSION_COOKIE_SECURE=true`, et un vrai domaine.

## Pourquoi pas Vercel ?

Vercel n'héberge que le front (Nuxt). Le back Spring Boot, PostgreSQL, Redis et MinIO n'y
tournent pas : il faudrait éclater la stack en services managés (base, cache, stockage) + un
hébergeur de conteneurs pour le back — de la complexité et des coûts injustifiés pour un
déploiement de test. La CI est déjà assurée par GitHub Actions sur chaque PR.
