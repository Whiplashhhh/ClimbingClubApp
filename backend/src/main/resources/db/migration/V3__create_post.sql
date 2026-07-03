-- Fil d'accueil : publications (info, cours annulé, affiche) avec audience.
-- Invariant multi-tenant : organization_id NOT NULL, exploité par toutes les requêtes.

CREATE TABLE post (
    id               UUID PRIMARY KEY,
    organization_id  UUID          NOT NULL REFERENCES organization (id),
    author_id        UUID          NOT NULL REFERENCES app_user (id),
    type             VARCHAR(20)   NOT NULL, -- INFO | CANCELLATION | POSTER
    audience         VARCHAR(20)   NOT NULL, -- ORG | COACH_STUDENTS
    title            VARCHAR(200)  NOT NULL,
    body             VARCHAR(5000),
    image_object_key VARCHAR(255),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_post_type CHECK (type IN ('INFO', 'CANCELLATION', 'POSTER')),
    CONSTRAINT ck_post_audience CHECK (audience IN ('ORG', 'COACH_STUDENTS')),
    -- Une affiche a toujours une image ; les autres types n'en ont jamais
    CONSTRAINT ck_post_poster_image CHECK ((type = 'POSTER') = (image_object_key IS NOT NULL))
);

CREATE INDEX ix_post_feed ON post (organization_id, created_at DESC);
