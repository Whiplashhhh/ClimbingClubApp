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

Dans `.env`, change les mots de passe et surtout renseigne l'endpoint public MinIO —
**les images d'affiches ne s'afficheront pas sans ça** (les URLs signées incluent l'hôte) :

```bash
POSTGRES_PASSWORD=un-vrai-mot-de-passe
MINIO_ROOT_PASSWORD=un-autre-vrai-mot-de-passe
S3_PUBLIC_ENDPOINT=http://<TS_HOST>:9000
```

Puis :

```bash
docker compose up -d --build
docker compose ps          # tout doit finir "healthy" / "running"
```

Premier build : plusieurs minutes (Maven + pnpm). Ensuite, depuis le téléphone (Tailscale actif),
ouvre **`http://<TS_HOST>:3000`** dans Safari/Chrome → crée un club, et
« Ajouter à l'écran d'accueil » pour l'installer en PWA.

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

- **Les affiches ne s'affichent pas** → `S3_PUBLIC_ENDPOINT` absent ou faux dans `.env`
  (doit être l'URL de MinIO vue du téléphone), puis `docker compose up -d` pour recharger.
- **Le front répond mais l'API échoue** → `docker compose logs backend` ; le back attend
  Postgres/Redis/MinIO healthy avant de démarrer.

## Exposition réseau (sécurité)

Ports ouverts sur le serveur : **3000** (front), **8080** (API + Swagger), **9000** (MinIO,
URLs signées). Postgres, Redis et la console MinIO (9001) sont liés à `127.0.0.1` — accessibles
uniquement en SSH sur la machine. Tant que le serveur n'a pas de port forwarding public, seuls
les appareils du tailnet voient l'application.

- Le cookie de session reste `Secure=false` ici car l'accès est en HTTP sur le tailnet (déjà
  chiffré par WireGuard). **Pour une vraie mise en production publique** : HTTPS obligatoire
  (reverse proxy type Caddy/Traefik), `SESSION_COOKIE_SECURE=true`, et un vrai domaine.

## Pourquoi pas Vercel ?

Vercel n'héberge que le front (Nuxt). Le back Spring Boot, PostgreSQL, Redis et MinIO n'y
tournent pas : il faudrait éclater la stack en services managés (base, cache, stockage) + un
hébergeur de conteneurs pour le back — de la complexité et des coûts injustifiés pour un
déploiement de test. La CI est déjà assurée par GitHub Actions sur chaque PR.
