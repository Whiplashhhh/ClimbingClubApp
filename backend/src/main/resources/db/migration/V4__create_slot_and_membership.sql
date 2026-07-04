-- Créneaux hebdomadaires récurrents et rattachement des membres (le « groupe » d'un moniteur).
-- Invariant multi-tenant : organization_id NOT NULL sur les deux tables (dénormalisé sur la
-- membership pour des requêtes d'isolation directes).

CREATE TABLE slot (
    id               UUID PRIMARY KEY,
    organization_id  UUID          NOT NULL REFERENCES organization (id),
    coach_id         UUID          NOT NULL REFERENCES app_user (id),
    name             VARCHAR(120)  NOT NULL,
    day_of_week      VARCHAR(10)   NOT NULL,
    start_time       TIME          NOT NULL,
    duration_minutes INT           NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_slot_day_of_week CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    ),
    CONSTRAINT ck_slot_duration CHECK (duration_minutes BETWEEN 15 AND 600)
);

CREATE INDEX ix_slot_organization ON slot (organization_id);

CREATE TABLE slot_membership (
    id              UUID PRIMARY KEY,
    organization_id UUID        NOT NULL REFERENCES organization (id),
    slot_id         UUID        NOT NULL REFERENCES slot (id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES app_user (id),
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_slot_membership UNIQUE (slot_id, user_id)
);

CREATE INDEX ix_slot_membership_user ON slot_membership (user_id);
CREATE INDEX ix_slot_membership_organization ON slot_membership (organization_id);
