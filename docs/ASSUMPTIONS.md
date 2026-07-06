# Hypothèses

Hypothèses prises faute d'information, avec le défaut le plus raisonnable. À invalider/confirmer.

- **A-001** (2026-07-03) : Email unique **global** (pas seulement par org), car un utilisateur
  appartient à une seule organisation et l'email sert d'identifiant de connexion.
- **A-002** (2026-07-03) : À l'inscription, un utilisateur choisit soit de **créer une org**
  (il devient OWNER, statut ACTIVE), soit de **rejoindre une org existante** (statut PENDING
  jusqu'à validation par un admin). Pas d'utilisateur durablement sans organisation.
- **A-003** (2026-07-03) : Cotations stockées en texte libre court (`6a+`, `V5`…) — pas de
  référentiel normalisé en v1.
- **A-004** (2026-07-03) : Langue de l'UI : français uniquement en v1 (pas d'i18n).
- **A-005** (2026-07-03) : « Élèves d'un moniteur » = membres ayant une `SlotMembership` sur un
  créneau dont le moniteur est le coach. Avant la Phase 3 (pas encore de créneaux), l'audience
  COACH_STUDENTS d'un post est résolue via cette relation et renvoie un fil vide côté élève tant
  qu'aucun créneau n'existe — c'est attendu.
- **A-006** (2026-07-03) : La stack initiale (copiée d'un autre projet) mentionnait
  Thymeleaf + openhtmltopdf : aucune fonctionnalité de la spec ne requiert de génération PDF ou
  de templating serveur → dépendances retirées. À réintroduire via ADR si un besoin réel émerge.
- **A-007** (2026-07-03) : Affiches du fil : formats image JPEG/PNG/WebP uniquement (détectés
  par octets magiques), 5 Mo max. Suffisant pour des affiches de club ; à élargir si besoin réel.
- **A-008** (2026-07-03) : « Tests e2e du chemin critique » = tests d'intégration API bout en
  bout (Testcontainers : Postgres + Redis + MinIO réels, HTTP réel, CSRF réel) + tests de
  composants Vitest. Les e2e navigateur Playwright seront introduits quand l'UI aura assez de
  surface pour les rentabiliser (Phase 7 au plus tard).
- **A-009** (2026-07-04) : Récurrence des créneaux : **hebdomadaire** uniquement en v1 (jour de
  la semaine + heure + durée). Le « moniteur » d'un créneau doit être un membre actif avec un
  rôle COACH, ADMIN ou OWNER (un président de petit club peut animer ses cours) — jamais un
  simple MEMBER. Un moniteur ne crée/gère que ses propres créneaux ; un admin gère tout.
- **A-010** (2026-07-04) : Plus de catégories de posts (décision produit, retours n°3) : un post
  = titre + texte + 0..4 images (5 Mo max chacune). Les posts générés par une annulation/un
  décalage de séance portent `important` (mise en évidence rouge) et sont épinglés en tête du
  fil jusqu'à la fin du jour (UTC) de la séance concernée, puis redescendent dans le flux normal.
- **A-011** (2026-07-04) : Cartographie des murs : les **secteurs** sont gérés par les admins
  (structure du club), les **voies** par les moniteurs et admins (créateur-ou-admin pour
  modifier/supprimer). Les prises sont des points relatifs (0..1) en JSONB, rendues en overlay
  SVG — l'éditeur graphique arrive en Phase 7, l'API PUT /holds existe déjà.
- **A-012** (2026-07-05) : Séances : confidentialité par séance (CLUB | FRIENDS | PRIVATE).
  Tant que le graphe d'amis (Phase 5B) n'existe pas, FRIENDS équivaut à PRIVATE (visible du seul
  auteur). L'assureur est un **nom libre** en 5A ; la sélection d'un ami arrivera en 5B (la
  colonne `belayer_user_id` est déjà prête). L'ascension référence une voie du mur (Phase 4).
- **A-013** (2026-07-05) : Graphe d'amis (Phase 5B) : amitié **symétrique** modélisée par une
  seule ligne orientée (requester → addressee) passant de PENDING à ACCEPTED à l'acceptation par
  le destinataire. Bornée à l'organisation : on ne peut se lier qu'à un **membre actif** du même
  club (demande à un membre d'une autre org → 404, existence masquée). Une seule demande par
  paire (les deux sens sont refusés une fois un lien existant → 409). `DELETE /api/friends/{id}`
  couvre les trois cas (retirer un ami, annuler une demande envoyée, refuser une demande reçue).
  Les séances FRIENDS deviennent visibles des amis acceptés ; l'assureur d'une ascension peut
  être un membre actif (prime sur le nom libre) — pas nécessairement un ami, pour ne pas bloquer
  la saisie d'une séance avec un partenaire qu'on vient de rencontrer.
- **A-014** (2026-07-05) : Sondages (Phase 6) : **choix unique** en v1 (un membre a une seule
  voix par sondage, remplaçable tant que le sondage est ouvert) — le multi-choix pourra venir
  plus tard si besoin. Un sondage réutilise l'**audience du fil** (ORG par OWNER/ADMIN,
  COACH_STUDENTS par un moniteur, visible de ses élèves via le même prédicat que les posts) : pas
  de nouveau système de ciblage. 2 à 10 options. Échéance (`closesAt`) optionnelle : au-delà, le
  vote est refusé (409) mais les résultats restent visibles. Résultats (décomptes + total)
  visibles de tous ceux qui voient le sondage — pas de vote anonyme séparé, cohérent avec un
  petit club. Suppression par l'auteur ou un admin (comme les posts).
- **A-015** (2026-07-05) : Messagerie (Phase 6B) : **1:1 uniquement**, et strictement entre un
  **moniteur et l'un de ses élèves** — la relation est dérivée d'un rattachement à un créneau
  (même source que l'audience COACH_STUDENTS, A-005). Pas de MP entre deux simples membres ni
  entre deux moniteurs sans lien de créneau (démarrer un tel fil → 409). **Un seul fil par
  paire** (coach, élève) ; le démarrer à nouveau renvoie l'existant. Lecture par le destinataire
  suivie via `read_at` par message (compteur de non-lus par fil + global) ; ouvrir un fil marque
  ses messages reçus comme lus. Chaque envoi crée une **notification in-app** `NEW_MESSAGE` au
  destinataire (pas encore de push web — Phase 7). Pas de pièces jointes en v1 (texte, 4000 car.).
  L'éligibilité n'est pas exposée en liste : le front propose tous les membres et le serveur
  tranche (409 si pas de relation), pour rester simple sans divulguer le graphe.
- **A-016** (2026-07-05, retours terrain) : Réorganisation de la navigation. La barre débordait
  sur mobile ; on la resserre. **« Sondages » n'est plus un onglet** : la création d'un sondage
  devient une **bascule Info | Sondage dans le composeur du fil**, et les sondages s'affichent
  **dans le fil**, entrelacés avec les posts par date (le back reste inchangé : le fil merge
  posts + `/api/polls` côté front). **« Amis » et « Messages » passent en icônes** dans l'en-tête
  à côté de la cloche (Messages porte une pastille de non-lus). **« Membres » n'est visible que
  des encadrants** (président/admin/moniteurs) — l'API était déjà protégée, on masque juste
  l'onglet. La navigation texte se limite donc à : Fil, Créneaux, Voies, Séances (+ Membres pour
  les encadrants), en `flex-wrap` pour ne jamais déborder.
- **A-017** (2026-07-05, retours terrain) : Messagerie de **groupe** en plus des 1:1. Un fil est
  désormais `DIRECT` (moniteur↔élève, inchangé), `SLOT` (un groupe par créneau : ses élèves + le
  moniteur) ou `GENERAL` (un groupe par club : tous les membres actifs). Les groupes sont
  **auto-provisionnés** et leur accès est **calculé** (appartenance au créneau / au club), pas
  stocké par membre : un nouvel arrivant voit donc **tout l'historique** (choix produit du
  président). « Un groupe par créneau » suffit (pas de groupe séparé « par moniteur » : chaque
  créneau a déjà son moniteur ; un membre garde en plus un 1:1 vers chacun de ses moniteurs).
  **Non-lus par participant** via `conversation_read` (dernière lecture par fil), remplaçant le
  `read_at` par message. **Tout le monde écrit** dans les groupes ; une limite de débit du groupe
  général, configurable par les admins, arrive en tranche suivante (A-018). Les **groupes ne
  génèrent pas de notification** in-app par message (la pastille de non-lus suffit — éviter le
  spam) ; les 1:1 continuent de notifier (`NEW_MESSAGE`).
- **A-018** (2026-07-05, retours terrain) : Limite de débit du **groupe général**, réglable par
  les **admins** (président/admin). Deux réglages d'organisation : soit **illimité** (défaut),
  soit **N messages par membre par fenêtre de T secondes** (les deux ensemble, sinon 400).
  Au-delà, l'envoi dans le groupe général renvoie **429**. La limite s'applique **à tout le monde**
  uniformément (y compris les encadrants) — l'admin choisit la valeur ; elle ne concerne que le
  groupe général (les groupes de créneau et les 1:1 restent libres). Réglage exposé en lecture à
  tout membre actif (`GET /api/messaging/settings`), modifiable par les admins (`PATCH`).
- **A-019** (2026-07-06) : Politique de mot de passe (`@StrongPassword`, appliquée à l'inscription
  et au changement) : **≥ 10 caractères** et **au moins 3 des 4 classes** (majuscule, minuscule,
  chiffre, caractère spécial), plus rejet d'une petite liste de mots de passe évidents. Validée
  côté back (source de vérité) et reflétée côté front par un indicateur de règles. Le back reste
  borné à 72 caractères (limite bcrypt).
- **A-020** (2026-07-06) : Rate limiting anti brute-force (Redis, fenêtre fixe). **Connexion**
  limitée par **email** (échecs) : au-delà de `login-max` échecs dans `login-window`, la connexion
  renvoie **429** (réinitialisé au succès). **Inscription** limitée par **IP cliente** :
  au-delà de `register-max` dans `register-window`, **429**. L'IP de bouclage (127.0.0.1/::1) est
  **ignorée** (health-checks, appels locaux, suite de tests) ; en production l'app tourne derrière
  un reverse proxy et `server.forward-headers-strategy=framework` fait remonter l'IP réelle via
  `X-Forwarded-For`. Valeurs par défaut : 5 échecs / 15 min (login), 5 inscriptions / 1 h (IP),
  surchargeables par variables d'environnement (`RATELIMIT_*`).
