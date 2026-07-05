-- Phase 6 : sondages. Un sondage reprend le système d'audience du fil (ORG | COACH_STUDENTS) :
-- ORG publié par OWNER/ADMIN, COACH_STUDENTS par un moniteur (visible de ses élèves). Vote à
-- choix unique (une ligne par (sondage, utilisateur), remplaçable), fermeture optionnelle par date.

CREATE TABLE poll (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    author_id       UUID         NOT NULL REFERENCES app_user (id),
    audience        VARCHAR(20)  NOT NULL, -- ORG | COACH_STUDENTS
    question        VARCHAR(300) NOT NULL,
    closes_at       TIMESTAMPTZ,           -- échéance optionnelle (au-delà : vote fermé)
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_poll_audience CHECK (audience IN ('ORG', 'COACH_STUDENTS'))
);

CREATE INDEX ix_poll_org_created ON poll (organization_id, created_at DESC);
CREATE INDEX ix_poll_author ON poll (author_id);

CREATE TABLE poll_option (
    id              UUID         PRIMARY KEY,
    poll_id         UUID         NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    label           VARCHAR(200) NOT NULL,
    position        SMALLINT     NOT NULL
);

CREATE INDEX ix_poll_option_poll ON poll_option (poll_id, position);

CREATE TABLE poll_vote (
    id              UUID         PRIMARY KEY,
    poll_id         UUID         NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
    option_id       UUID         NOT NULL REFERENCES poll_option (id) ON DELETE CASCADE,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    user_id         UUID         NOT NULL REFERENCES app_user (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_poll_vote_user UNIQUE (poll_id, user_id)
);

CREATE INDEX ix_poll_vote_option ON poll_vote (option_id);
