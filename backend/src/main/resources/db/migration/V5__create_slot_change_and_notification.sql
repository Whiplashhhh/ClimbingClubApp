-- Annulation / décalage d'une séance de créneau (à une date donnée) et notifications in-app.

CREATE TABLE slot_change (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    slot_id         UUID         NOT NULL REFERENCES slot (id) ON DELETE CASCADE,
    date            DATE         NOT NULL,
    action          VARCHAR(20)  NOT NULL, -- CANCELLED | MOVED
    new_start_time  TIME,                  -- requis si MOVED, absent sinon
    note            VARCHAR(500),
    created_by      UUID         NOT NULL REFERENCES app_user (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_slot_change_action CHECK (action IN ('CANCELLED', 'MOVED')),
    CONSTRAINT ck_slot_change_moved_time CHECK ((action = 'MOVED') = (new_start_time IS NOT NULL)),
    -- Une seule modification par séance : annulée OU décalée
    CONSTRAINT ux_slot_change UNIQUE (slot_id, date)
);

CREATE INDEX ix_slot_change_organization ON slot_change (organization_id);

CREATE TABLE notification (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    recipient_id    UUID         NOT NULL REFERENCES app_user (id),
    type            VARCHAR(30)  NOT NULL, -- SLOT_CANCELLED | SLOT_MOVED
    message         VARCHAR(500) NOT NULL,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_notification_type CHECK (type IN ('SLOT_CANCELLED', 'SLOT_MOVED'))
);

CREATE INDEX ix_notification_recipient ON notification (recipient_id, created_at DESC);
