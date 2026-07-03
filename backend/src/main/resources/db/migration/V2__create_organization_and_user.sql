-- Colonne vertébrale multi-tenant : organisations et utilisateurs.
-- Invariant : un utilisateur appartient à exactement une organisation (organization_id NOT NULL).

CREATE TABLE organization (
    id            UUID PRIMARY KEY,
    name          VARCHAR(120)  NOT NULL,
    slug          VARCHAR(140)  NOT NULL,
    climbing_type VARCHAR(20)   NOT NULL, -- BOULDER | ROPES | BOTH
    address       VARCHAR(255),
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    logo_object_key VARCHAR(255),
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_organization_slug UNIQUE (slug),
    CONSTRAINT ck_organization_climbing_type CHECK (climbing_type IN ('BOULDER', 'ROPES', 'BOTH'))
);

CREATE TABLE app_user (
    id              UUID PRIMARY KEY,
    organization_id UUID          NOT NULL REFERENCES organization (id),
    email           VARCHAR(255)  NOT NULL,
    password_hash   VARCHAR(100)  NOT NULL,
    display_name    VARCHAR(120)  NOT NULL,
    role            VARCHAR(20)   NOT NULL, -- OWNER | ADMIN | COACH | MEMBER
    status          VARCHAR(20)   NOT NULL, -- PENDING | ACTIVE | DISABLED
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_app_user_role CHECK (role IN ('OWNER', 'ADMIN', 'COACH', 'MEMBER')),
    CONSTRAINT ck_app_user_status CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED'))
);

-- Email = identifiant de connexion, unique globalement et insensible à la casse (A-001)
CREATE UNIQUE INDEX ux_app_user_email ON app_user (lower(email));
CREATE INDEX ix_app_user_organization ON app_user (organization_id);
