-- Phase 5B : graphe d'amis, borné à l'organisation. Une demande (PENDING) devient une amitié
-- (ACCEPTED) à l'acceptation par le destinataire. Une seule ligne par paire (ordre demandeur →
-- destinataire) ; l'unicité de la paire non ordonnée est garantie côté service.

CREATE TABLE friendship (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    requester_id    UUID         NOT NULL REFERENCES app_user (id),
    addressee_id    UUID         NOT NULL REFERENCES app_user (id),
    status          VARCHAR(20)  NOT NULL, -- PENDING | ACCEPTED
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_friendship_status CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT ck_friendship_distinct CHECK (requester_id <> addressee_id),
    CONSTRAINT ux_friendship_pair UNIQUE (requester_id, addressee_id)
);

CREATE INDEX ix_friendship_requester ON friendship (requester_id, status);
CREATE INDEX ix_friendship_addressee ON friendship (addressee_id, status);
