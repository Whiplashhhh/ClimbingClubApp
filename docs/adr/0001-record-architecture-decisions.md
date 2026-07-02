# ADR 0001 — Enregistrer les décisions d'architecture

**Statut** : accepté — 2026-07-03

## Contexte
Le projet est développé en mode autonome ; les décisions structurantes doivent être traçables sans
dépendre d'une conversation.

## Décision
Toute décision d'architecture notable est consignée dans `docs/adr/NNNN-titre.md`, format court :
Statut / Contexte / Décision / Conséquences. Un ADR n'est jamais réécrit : il est remplacé
(« superseded by NNNN »).

## Conséquences
Les choix (auth, tenancy, stockage…) sont auditables ; les remises en cause passent par un nouvel ADR.
