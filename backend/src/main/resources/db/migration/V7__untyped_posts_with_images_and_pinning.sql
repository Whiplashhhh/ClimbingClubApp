-- Simplification des posts (décision produit, retours de test n°3) : plus de catégorie —
-- un post = titre + texte + 0..n images. Les posts d'annulation générés par les créneaux
-- sont marqués « important » et épinglés en tête du fil jusqu'à la fin du jour de la séance.

ALTER TABLE post ADD COLUMN important BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE post ADD COLUMN pinned_until TIMESTAMPTZ;

UPDATE post SET important = TRUE WHERE type = 'CANCELLATION';

CREATE TABLE post_image (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organization (id),
    post_id         UUID         NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    object_key      VARCHAR(255) NOT NULL,
    position        INT          NOT NULL,
    CONSTRAINT ux_post_image UNIQUE (post_id, position)
);

-- Reprise des affiches existantes (ex-type POSTER)
INSERT INTO post_image (id, organization_id, post_id, object_key, position)
SELECT gen_random_uuid(), organization_id, id, image_object_key, 0
FROM post
WHERE image_object_key IS NOT NULL;

ALTER TABLE post DROP CONSTRAINT ck_post_type;
ALTER TABLE post DROP CONSTRAINT ck_post_poster_image;
ALTER TABLE post DROP COLUMN type;
ALTER TABLE post DROP COLUMN image_object_key;
