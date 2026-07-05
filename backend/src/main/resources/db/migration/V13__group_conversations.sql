-- Retours terrain n°5 : la messagerie gagne des groupes en plus des 1:1 moniteur↔élève.
-- Un fil devient DIRECT (paire coach/élève), SLOT (un par créneau : ses membres + le moniteur)
-- ou GENERAL (un par club : tous les membres actifs). Les groupes sont auto-provisionnés et leur
-- accès est *calculé* (rattachement au créneau / appartenance au club) : un nouvel arrivant voit
-- donc tout l'historique. La lecture est suivie par (fil, utilisateur) pour gérer les non-lus des
-- groupes, remplaçant le read_at par message.

ALTER TABLE conversation ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'DIRECT';
ALTER TABLE conversation ALTER COLUMN type DROP DEFAULT;
ALTER TABLE conversation ALTER COLUMN coach_id DROP NOT NULL;
ALTER TABLE conversation ALTER COLUMN student_id DROP NOT NULL;
ALTER TABLE conversation ADD COLUMN slot_id UUID REFERENCES slot (id) ON DELETE CASCADE;
ALTER TABLE conversation ADD CONSTRAINT ck_conversation_type CHECK (type IN ('DIRECT', 'SLOT', 'GENERAL'));

-- Un seul fil par créneau, un seul fil général par organisation (index partiels).
CREATE UNIQUE INDEX ux_conversation_slot ON conversation (slot_id) WHERE slot_id IS NOT NULL;
CREATE UNIQUE INDEX ux_conversation_general ON conversation (organization_id) WHERE type = 'GENERAL';

-- Suivi de lecture par participant (fonctionne pour les 1:1 comme pour les groupes).
CREATE TABLE conversation_read (
    id              UUID         PRIMARY KEY,
    conversation_id UUID         NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    user_id         UUID         NOT NULL REFERENCES app_user (id),
    last_read_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_conversation_read UNIQUE (conversation_id, user_id)
);

CREATE INDEX ix_conversation_read_user ON conversation_read (user_id);

-- La lecture est désormais portée par conversation_read (par participant), plus par message.
ALTER TABLE message DROP COLUMN read_at;
