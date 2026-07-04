-- Phase 4 : cartographie des murs. Secteurs (avec photo de mur optionnelle) et voies
-- (nom, cotation libre — A-003, type bloc/voie, photo, annotations de prises).
-- Les annotations sont vectorielles (coordonnées relatives 0..1), rendues en overlay côté
-- front — le fichier image n'est jamais modifié.

CREATE TABLE sector (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL REFERENCES organization (id),
    name             VARCHAR(120) NOT NULL,
    photo_object_key VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_sector_organization ON sector (organization_id);

CREATE TABLE route (
    id               UUID         PRIMARY KEY,
    organization_id  UUID         NOT NULL REFERENCES organization (id),
    sector_id        UUID         NOT NULL REFERENCES sector (id),
    created_by       UUID         NOT NULL REFERENCES app_user (id),
    name             VARCHAR(120) NOT NULL,
    grade            VARCHAR(10)  NOT NULL,
    climb_type       VARCHAR(20)  NOT NULL, -- BOULDER | ROPE
    photo_object_key VARCHAR(255),
    holds            JSONB,                 -- [{x,y}] relatifs à l'image (0..1)
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_route_climb_type CHECK (climb_type IN ('BOULDER', 'ROPE'))
);

CREATE INDEX ix_route_organization ON route (organization_id);
CREATE INDEX ix_route_sector ON route (sector_id);
