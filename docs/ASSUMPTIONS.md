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
