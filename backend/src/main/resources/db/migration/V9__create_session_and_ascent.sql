-- Phase 5 : séances et ascensions. Un membre lance une séance puis y ajoute des ascensions.
-- Confidentialité par séance (CLUB | FRIENDS | PRIVATE) — FRIENDS s'activera avec le graphe
-- d'amis (Phase 5B) ; d'ici là il équivaut à privé (A-012).

CREATE TABLE climbing_session (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    user_id         UUID         NOT NULL REFERENCES app_user (id),
    started_at      TIMESTAMPTZ  NOT NULL,
    note            VARCHAR(500),
    visibility      VARCHAR(20)  NOT NULL, -- CLUB | FRIENDS | PRIVATE
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_session_visibility CHECK (visibility IN ('CLUB', 'FRIENDS', 'PRIVATE'))
);

CREATE INDEX ix_session_user ON climbing_session (user_id, started_at DESC);
CREATE INDEX ix_session_org_visibility ON climbing_session (organization_id, visibility, started_at DESC);

CREATE TABLE ascent (
    id               UUID        PRIMARY KEY,
    organization_id  UUID        NOT NULL REFERENCES organization (id),
    session_id       UUID        NOT NULL REFERENCES climbing_session (id) ON DELETE CASCADE,
    route_id         UUID        NOT NULL REFERENCES route (id),
    rating           SMALLINT,             -- appréciation 1..5 (nullable)
    top_hold         SMALLINT,             -- prise la plus haute atteinte (nullable)
    duration_seconds INT,                  -- temps (optionnel — simple métrique)
    belayer_user_id  UUID        REFERENCES app_user (id), -- assureur ami (Phase 5B)
    belayer_name     VARCHAR(120),         -- OU nom libre
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ascent_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    CONSTRAINT ck_ascent_top_hold CHECK (top_hold IS NULL OR top_hold >= 0),
    CONSTRAINT ck_ascent_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

CREATE INDEX ix_ascent_session ON ascent (session_id);
CREATE INDEX ix_ascent_organization ON ascent (organization_id);
