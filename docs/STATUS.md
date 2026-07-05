# État d'avancement — Belay

> Mis à jour : 2026-07-05 (retours terrain n°5B : limite de débit du groupe général — retours n°5 complets).

## Fait (mergé dans `main`, CI verte)

- **Docs & décisions** (PR #1) : PRODUCT_SPEC, ROADMAP, ADR 0001-0005, ASSUMPTIONS.
- **Phase 0 — Fondations** (PR #2) : monorepo back/front, docker-compose (Postgres 16, Redis,
  MinIO, back, front), CI GitHub Actions (jobs back et front séparés), contract-first
  (`openapi.json` exporté par `./mvnw verify`, types front via `pnpm gen:api`), PWA installable,
  Flyway, Spotless, Husky/commitlint.
- **Phase 1 — Comptes / orgs / rôles / sécurité** (PR #3) : inscription (création de club ou
  demande d'adhésion par code), connexion, sessions serveur Redis (cookie `HttpOnly`/`SameSite`,
  `Secure` en prod), CSRF SPA, rôles OWNER/ADMIN/COACH/MEMBER + statut PENDING/ACTIVE/DISABLED,
  approbation des membres, autorisation par rôle côté serveur, **test anti-IDOR inter-org**.
- **Phase 2 — Fil d'accueil** (cette tranche) :
  - `Post` (INFO / CANCELLATION / POSTER) avec audience ORG ou COACH_STUDENTS (migration V3) ;
  - matrice de publication côté serveur : ORG → OWNER/ADMIN, COACH_STUDENTS → COACH ;
    suppression par l'auteur ou un admin ; agrégation du fil scopée à l'organisation ;
  - upload d'affiche : MIME vérifié par octets magiques (JPEG/PNG/WebP), 5 Mo max, clé objet
    regénérée, bucket MinIO privé, URLs signées 15 min (ADR 0006) ;
  - UI : page d'accueil = fil (cartes, images signées, « voir plus »), composeur pour
    admins/moniteurs, gestion des membres déplacée sur `/members`, nav dans l'en-tête ;
  - tests : intégration API (audiences, 403, anti-IDOR posts, upload accepté/rejeté) + vitest
    (visibilité du composeur par rôle, rendu du fil, état PENDING) ;
  - skill `.claude/skills/full-stack-slice/SKILL.md` (recette des tranches suivantes) ;
  - ménage : Thymeleaf/openhtmltopdf retirés de la stack (A-006).

## Fait aussi (suite au test terrain sur iPhone + Phase 3 partie A)

- **Déploiement auto-hébergé** documenté et testé (`docs/DEPLOY.md`) : serveur perso via
  Tailscale, ports custom committés par Willem sur `main` (front : 3003).
- **Correctifs terrain** : `app.vue` sans `NuxtLayout` (en-tête/déconnexion invisibles — corrigé,
  en-tête responsive), bannière de demandes d'adhésion sur le fil, zoom iOS au focus des champs
  (16px imposés).
- **Phase 3 partie A — créneaux & groupes** : `Slot` hebdomadaire (jour/heure/durée/moniteur) +
  `SlotMembership` (V4), API scopée avec matrice (admins partout, moniteur sur ses créneaux,
  coach éligible = ACTIVE non-MEMBER — A-009), **le fil résout enfin COACH_STUDENTS via les
  rattachements (A-005 refermée)**, page `/slots` (planning, création, gestion du groupe),
  sélecteur de rôle sur `/members`, tests intégration + vitest.

- **Phase 3 partie B — annulations & notifications in-app** : `SlotChange` (annuler/décaler UNE
  séance à une date, garde-fous : jour du créneau, unicité, heure requise si décalée — V5),
  domaine `Notification` générique (message FR, lu/non-lu), le groupe + le moniteur (sauf
  l'auteur) sont notifiés ; cloche avec pastille dans l'en-tête, page `/notifications`
  (tout marqué lu à l'ouverture), formulaire annuler/décaler + « rétablir » sur les cartes de
  créneau. L'annulation publie AUSSI un post « Cours annulé » automatique dans le fil des
  élèves (au nom du moniteur du créneau, lié par V6, retiré si la séance est rétablie) —
  décision produit de Willem après test terrain, remplace le choix initial inverse.

- **Retours de test n°2** : annulations mises en évidence dans le fil (carte rouge) et
  publiées automatiquement ; agenda « Cette semaine » sur /slots (admins/moniteurs, séances
  réelles avec annulations/décalages appliqués) ; menu avatar (initiales) qui héberge la
  déconnexion — prêt pour la future page profil ; rafraîchissement automatique (retour au
  premier plan + 60 s) sur fil/notifications/créneaux/pastille + boutons ↻ manuels, en
  attendant le push web (Phase 7).

- **Retours de test n°3 — posts simplifiés** : plus de catégories (V7) — un post = titre +
  texte + 0..4 images (`post_image`, contenus vérifiés, URLs signées) ; les annulations de
  séance sont `important` (carte rouge) et **épinglées en tête du fil** jusqu'à la fin du jour
  de la séance (`pinned_until`), puis redescendent (A-010).

- **Phase 4 — Voies & murs** : `Sector` (photo de mur optionnelle) + `Route` (nom, cotation
  libre A-003, bloc/voie, photo, **annotations de prises en JSONB**, coordonnées relatives
  0..1 — A-011) — V8 ; API scopée (secteurs : admins ; voies : moniteurs/admins,
  créateur-ou-admin en mutation ; PUT /holds) ; page `/routes` (cartographie par secteur,
  formulaires, **overlay SVG des prises en lecture** avec bouton afficher/masquer — l'éditeur
  d'annotations reste en Phase 7) ; tests intégration + vitest.

- **Phase 5A — Séances & ascensions** : `ClimbingSession` + `Ascent` (V9) ; lancer une séance,
  y ajouter des ascensions (voie du club, note /5, prise max, temps, assureur en nom libre),
  confidentialité par séance (CLUB | FRIENDS | PRIVATE — A-012), historique perso + onglet
  activité du club (séances CLUB des autres). Page `/sessions`, tests intégration + vitest.
- **Correctif SSR** : toutes les pages de liste (fil, créneaux, voies, notifications, membres,
  séances) étaient **vides au rechargement à froid** (le handler `useAsyncData` ne se rejoue pas
  côté client après SSR) — corrigé en réhydratant les refs depuis la payload.

- **Phase 5B — Amis & confidentialité** (Phase 5 complète) : `Friendship` (V10) org-scopé,
  demande → acceptation par le destinataire, un lien par paire, borné aux **membres actifs** du
  club (A-013). `FriendService`/`FriendController` : envoyer/accepter/retirer, lister amis +
  demandes entrantes. La **visibilité FRIENDS devient effective** — le fil d'activité inclut
  désormais les séances FRIENDS des amis acceptés (`findClubActivity` étendue). L'**assureur
  d'une ascension** peut être un membre actif (`belayerUserId`, prime sur le nom libre). UI :
  page `/friends` (liste, demandes reçues accepter/refuser, envoi à un membre ajoutable),
  sélecteur d'assureur membre dans la carte de séance, entrée de nav « Amis ». Tests intégration
  (flux de demande, visibilité FRIENDS, assureur membre, self/doublon 409, anti-IDOR inter-org)
  + vitest (candidats filtrés, envoi, acceptation, sélecteur d'assureur).

- **Phase 6A — Sondages** : `Poll` + `PollOption` + `PollVote` (V11) réutilisant l'audience du
  fil (ORG | COACH_STUDENTS, même prédicat de visibilité que les posts, A-014). Création selon la
  matrice (ORG : admins ; COACH_STUDENTS : moniteurs), vote à **choix unique** remplaçable avec
  décomptes en direct, échéance optionnelle (au-delà : 409). API `GET/POST /api/polls`,
  `POST /api/polls/{id}/vote`, `DELETE /api/polls/{id}` (auteur/admin). Page `/polls` (composeur
  pour admins/moniteurs, cartes avec barres de résultats et vote en ligne), entrée de nav
  « Sondages ». Tests intégration (vote+revote+décomptes, visibilité COACH_STUDENTS via créneau,
  fermeture 409, option inconnue 404, matrice, anti-IDOR) + vitest.

- **Phase 6B — Messagerie privée** (Phase 6 complète) : `Conversation` + `Message` (V12), fil 1:1
  strictement moniteur ↔ l'un de ses élèves (relation dérivée d'un créneau, A-015), un seul fil
  par paire. Non-lus par fil et global (`read_at` par message ; ouvrir un fil marque lu), chaque
  envoi crée une notification in-app `NEW_MESSAGE`. API `GET/POST /api/conversations`,
  `GET/POST /api/conversations/{id}/messages`. Page `/messages` (liste avec pastilles de non-lus,
  vue fil avec bulles alignées et zone d'envoi, démarrage d'un fil ; le serveur valide
  l'éligibilité → 409). Entrée de nav « Messages ». Tests intégration (échange + non-lus +
  notification, garde-fous d'éligibilité, anti-IDOR inter-org) + vitest.

- **Retours terrain n°4 — navigation** : la barre de nav débordait sur mobile. « Sondages »
  n'est plus un onglet : la création se fait via une bascule **Info | Sondage** dans le composeur
  du fil, et les sondages s'affichent **dans le fil** (entrelacés par date). « Amis » et
  « Messages » deviennent des **icônes d'en-tête** à côté de la cloche (Messages avec pastille de
  non-lus). « Membres » n'est visible que des **encadrants**. Nav resserrée en `flex-wrap`
  (A-016). Frontend uniquement ; tests vitest mis à jour (fil+sondages, layout).

- **Retours terrain n°5A — messagerie de groupe** : les fils deviennent typés `DIRECT | SLOT |
  GENERAL` (V13). Chaque membre voit un **groupe par créneau** (élèves + moniteur) et le **groupe
  général du club**, en plus de ses 1:1 moniteur↔élève. Groupes auto-provisionnés, accès calculé
  → **historique complet visible des nouveaux arrivants**. Non-lus par participant
  (`conversation_read`), tout le monde écrit dans les groupes, pas de notification par message de
  groupe (A-017). Page Messages adaptée (pastille Groupe/Club, nom de l'expéditeur dans les fils
  de groupe). Tests intégration (1:1 + général + créneau + anti-IDOR) + vitest.

- **Retours terrain n°5B — limite de débit du groupe général** : réglage d'organisation posé par
  un admin (V14) — soit illimité, soit **N messages par membre par fenêtre de T secondes** ;
  au-delà, l'envoi dans le groupe général renvoie **429** (A-018). `GET/PATCH
  /api/messaging/settings` (lecture pour tous, écriture admins), panneau d'admin dans la page
  Messages. Tests intégration (défaut illimité, 403 non-admin, 400 incohérent, 429 à la limite,
  retour illimité) + vitest.

## En cours

- Rien — la **Phase 6 est complète** ; **tous les retours terrain n°4 et n°5 sont traités**.

## Prochaines étapes (dans l'ordre de la ROADMAP)

1. **Phase 7 — finitions PWA** : offline de base + push web, page profil (changement de mot de
   passe), éditeur d'annotations de prises, itinéraire (deep-link maps), rate limiting (Redis)
   sur les endpoints sensibles, export / suppression RGPD.

## Points d'attention

- **Médias** : servis par l'application (`GET /api/media/**`, authentifié, scopé par org) — plus
  d'URLs signées ni de MinIO exposé (ADR 0006 amendé).
- **E2e navigateur** : pas encore de Playwright (A-008) ; le chemin critique est couvert par les
  tests d'intégration API + vitest.
- Les ports publiés sont personnalisés directement dans `docker-compose.yml` sur `main` (choix
  de Willem pour son serveur) ; l'alternative `docker-compose.override.yml` est documentée dans
  DEPLOY.md si on veut revenir aux défauts dans le repo.
