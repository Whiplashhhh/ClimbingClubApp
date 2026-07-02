# Belay — Spécification produit

> `belay` est un nom de code provisoire.
> Ce document est la spec de référence. Les décisions d'architecture sont dans `docs/adr/`,
> le plan d'exécution dans `docs/ROADMAP.md`.

## Concept

Une application (PWA d'abord) qui permet à n'importe quel club d'escalade de gérer sa vie interne.
N'importe qui peut **créer une organisation** (club) : logo, nom, adresse (avec lancement d'un
itinéraire vers le lieu via un lien maps), type (bloc / voies / les deux), et cartographie des murs
(photos des murs, et optionnellement une photo par voie). Un utilisateur appartient à **exactement
une organisation** (relation directe `User → Organization`), avec un **rôle** et un **statut** au
sein de celle-ci.

**L'isolation entre organisations est un invariant de sécurité, pas une option.**

## Acteurs & rôles (scopés par organisation)

- **Président / Owner** : administre l'org, gère membres/rôles, créneaux, murs, voies ; publie
  infos et sondages visibles par **toute** l'org.
- **Admin** : comme président sauf transfert de propriété.
- **Moniteur (coach)** : gère ses créneaux et les groupes associés ; publie infos/sondages visibles
  **uniquement par ses élèves** ; peut annuler/décaler un créneau (notifie le groupe) ; crée des
  voies ; échange en message privé avec ses élèves.
- **Élève / Grimpeur (membre)** : voit le fil d'accueil qui le concerne, son/ses créneau(x) et
  groupe(s), les voies ; enregistre ses séances ; gère ses amis ; répond aux sondages ; contacte
  son moniteur en privé.
- **Pending** : membre en attente de validation par un admin (état intermédiaire).

Représentation technique : `User.role ∈ {OWNER, ADMIN, COACH, MEMBER}` et
`User.status ∈ {PENDING, ACTIVE, DISABLED}`. Un utilisateur `PENDING` est authentifiable mais
n'accède à aucune donnée de l'organisation tant qu'un admin ne l'a pas validé.

## Fonctionnalités (regroupées par domaine)

### Organisation & murs
- Création d'org (logo, nom, adresse géocodée + deep-link itinéraire, type d'escalade).
- Gestion des membres et attribution des rôles ; invitations / demandes d'adhésion.
- Cartographie des murs : secteurs, photos de mur, rattachement des voies aux secteurs.

### Créneaux & groupes
- Créneaux (récurrents) auxquels des membres sont rattachés.
- Un **groupe** = l'ensemble des membres d'un créneau (dérivé), support de communication du
  moniteur.
- Annulation / décalage d'un créneau → notification au groupe.

### Fil d'accueil (infos, posters, sondages)
- Publications avec une **audience** : org-entière (président/admin) ou élèves-d'un-moniteur
  (moniteur).
- Types : info/événement, cours annulé, poster (image), sondage.
- **Sondages** : création, réponses, résultats, mêmes règles d'audience.
- Le fil d'un utilisateur agrège les publications dont il fait partie de l'audience.

### Voies
- Créées par moniteurs/admins : nom, cotation, secteur/mur, type (bloc/voie), photo.
- **Mise en évidence des prises** sur la photo : stockée comme **annotations vectorielles**
  (points/polygones en coordonnées relatives à l'image) rendues en overlay SVG/canvas — pas de
  modification du fichier image. L'éditeur d'annotations est une fonctionnalité de phase
  ultérieure.

### Séances & performances
- Un membre **lance une séance**, puis y ajoute des **ascensions** ; par ascension : la voie, une
  note/appréciation, la **prise la plus haute atteinte**, le **temps** (optionnel/nullable —
  simple métrique, ne rien bloquer dessus), et **l'assureur** (un ami sélectionné OU un nom libre).
- Historique de séances par utilisateur, avec réglage de **confidentialité** (qui peut voir).

### Social
- Graphe d'amis (demande / acceptation). Voir les séances des amis (selon leur confidentialité).
- Sélection d'un ami comme assureur lors d'une ascension.

### Messagerie
- Messages privés **élève ↔ moniteur** (au minimum ; conversation 1-1).

### Notifications
- In-app d'abord (annulation de créneau, nouveau sondage, demande d'ami, nouveau message). Push
  web via la PWA en phase ultérieure.

## Modèle de domaine

Entités principales (le schéma détaillé vit dans les migrations Flyway) :

- `Organization` — id, name, logo, address (+ lat/lng pour le deep-link itinéraire),
  climbing type (`BOULDER | ROPES | BOTH`).
- `User` — rattaché à **une seule** `Organization` (`organization_id NOT NULL` sauf le temps de
  l'onboarding), porte `role` et `status`. Email unique global, mot de passe hashé (bcrypt).
- `Wall` / `Sector` — cartographie : un mur porte des photos ; un secteur appartient à un mur.
- `Route` (voie) — name, grade, sector, type (bloc/voie), photo. + `RouteHold` / `Annotation` :
  annotations vectorielles (coordonnées relatives) rendues en overlay.
- `Slot` (créneau récurrent) + `SlotMembership` — le **groupe** d'un créneau est dérivé de ses
  memberships.
- `Post` — audience `ORG | COACH_STUDENTS`, type `INFO | CANCELLATION | POSTER | POLL`,
  author, optional image (poster).
- `Poll` + `PollOption` + `PollVote`.
- `Session` (séance) + `Ascent` (ascension : `route`, `rating`, `top_hold`, `duration?`,
  `belayer_user_id?` / `belayer_name?`) ; `Session.visibility ∈ {PRIVATE, FRIENDS, ORG}`.
- `Friendship` (`requester`, `addressee`, `status ∈ {PENDING, ACCEPTED, DECLINED}`).
- `Conversation` + `Message` (1-1 élève ↔ moniteur).
- `Notification` (in-app).

### Invariants

1. Un utilisateur appartient à **une seule** organisation.
2. **Toute** table multi-tenant (y compris `User`) porte, directement ou par relation, une
   `organization_id` exploitée pour l'isolation. Aucune requête non scopée.
3. Le graphe d'amis et la messagerie restent **bornés à l'organisation**.
4. L'autorisation (rôle + tenancy) est vérifiée **côté serveur** sur chaque endpoint ; le front ne
   fait que masquer.

## Matrice de permissions

Chaque ligne est testée côté back : un rôle ou une organisation non autorisés reçoivent **403/404**
(pas seulement un masquage UI).

| Action | OWNER | ADMIN | COACH | MEMBER | PENDING |
|---|---|---|---|---|---|
| Administrer l'org (nom, logo, adresse) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Transférer la propriété de l'org | ✅ | ❌ | ❌ | ❌ | ❌ |
| Gérer membres & rôles, valider les PENDING | ✅ | ✅ | ❌ | ❌ | ❌ |
| Créer/modifier murs & secteurs | ✅ | ✅ | ❌ | ❌ | ❌ |
| Créer/modifier des voies | ✅ | ✅ | ✅ | ❌ | ❌ |
| Créer/modifier des créneaux | ✅ | ✅ | ✅ (les siens) | ❌ | ❌ |
| Annuler/décaler un créneau (notifie le groupe) | ✅ | ✅ | ✅ (les siens) | ❌ | ❌ |
| Publier info/poster/sondage **audience ORG** | ✅ | ✅ | ❌ | ❌ | ❌ |
| Publier info/poster/sondage **audience COACH_STUDENTS** | ❌ | ❌ | ✅ (ses élèves) | ❌ | ❌ |
| Lire le fil d'accueil | ✅ | ✅ | ✅ | ✅ (posts dont il est l'audience) | ❌ |
| Répondre à un sondage | ✅ | ✅ | ✅ | ✅ (si dans l'audience) | ❌ |
| Enregistrer une séance / des ascensions | ✅ | ✅ | ✅ | ✅ (les siennes) | ❌ |
| Voir la séance d'autrui | selon `visibility` du propriétaire (amis / org) — jamais inter-org | | | | ❌ |
| Demander/accepter une amitié (même org) | ✅ | ✅ | ✅ | ✅ | ❌ |
| Envoyer un MP à son moniteur | — | — | ✅ (à ses élèves) | ✅ (à son moniteur) | ❌ |
| Toute action sur les données d'une **autre org** | ❌ | ❌ | ❌ | ❌ | ❌ |

## Contraintes non fonctionnelles

- **Sécurité** : cf. `CLAUDE.md` §10 (sessions Redis, CSRF, validation, uploads, secrets, OWASP).
- **RGPD** : app française, données perso (identités, messages, graphe d'amis, séances, photos) —
  minimisation, export et suppression de compte, pas de PII dans les logs, base légale documentée.
- **PWA** : installable, offline de base en phase ultérieure.
- **Contract-first** : le back définit `openapi.json`, le front consomme des types générés.
