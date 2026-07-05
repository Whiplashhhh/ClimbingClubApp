-- Retours terrain n°5B : les admins peuvent limiter le débit du groupe général du club. Deux
-- colonnes sur l'organisation : NULL = écriture illimitée ; sinon N messages par membre et par
-- fenêtre de temps (secondes). Les deux vont de pair (contrainte).

ALTER TABLE organization ADD COLUMN general_chat_rate_limit INT;
ALTER TABLE organization ADD COLUMN general_chat_window_seconds INT;

ALTER TABLE organization ADD CONSTRAINT ck_org_general_chat_limit CHECK (
    (general_chat_rate_limit IS NULL AND general_chat_window_seconds IS NULL)
    OR (general_chat_rate_limit > 0 AND general_chat_window_seconds > 0));
