-- Photo de profil (avatar) : clé de l'objet stocké (MinIO), servie via /api/media. NULL = pas de
-- photo (repli sur les initiales côté UI).
ALTER TABLE app_user ADD COLUMN avatar_object_key VARCHAR(255);
