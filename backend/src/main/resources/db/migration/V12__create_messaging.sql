-- Phase 6 : messagerie privée. Un fil de discussion relie un moniteur et l'un de ses élèves
-- (relation dérivée d'un rattachement à un créneau, A-005). Un seul fil par paire (coach, élève).
-- Les messages portent leur lecture par le destinataire (read_at) pour le compteur de non-lus.

CREATE TABLE conversation (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    coach_id        UUID         NOT NULL REFERENCES app_user (id),
    student_id      UUID         NOT NULL REFERENCES app_user (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    last_message_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_conversation_distinct CHECK (coach_id <> student_id),
    CONSTRAINT ux_conversation_pair UNIQUE (coach_id, student_id)
);

CREATE INDEX ix_conversation_coach ON conversation (coach_id, last_message_at DESC);
CREATE INDEX ix_conversation_student ON conversation (student_id, last_message_at DESC);

CREATE TABLE message (
    id              UUID          PRIMARY KEY,
    conversation_id UUID          NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    organization_id UUID          NOT NULL REFERENCES organization (id),
    sender_id       UUID          NOT NULL REFERENCES app_user (id),
    body            VARCHAR(4000) NOT NULL,
    read_at         TIMESTAMPTZ,            -- lu par le destinataire (NULL = non lu)
    created_at      TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_message_conversation ON message (conversation_id, created_at);

-- Un nouveau message notifie le destinataire : on étend le type de notification (in-app).
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification
    ADD CONSTRAINT ck_notification_type CHECK (type IN ('SLOT_CANCELLED', 'SLOT_MOVED', 'NEW_MESSAGE'));
