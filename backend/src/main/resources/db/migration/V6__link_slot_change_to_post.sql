-- Une annulation/décalage publie automatiquement un post dans le fil des élèves du moniteur ;
-- le lien permet de retirer le post si la séance est rétablie. SET NULL : le post peut être
-- supprimé indépendamment (par son auteur ou un admin) sans casser l'historique du créneau.

ALTER TABLE slot_change
    ADD COLUMN post_id UUID REFERENCES post (id) ON DELETE SET NULL;
