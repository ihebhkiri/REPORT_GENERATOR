-- À exécuter avant le déploiement sur une base existante.
-- Vérifier le schéma de métadonnées ; public est le schéma par défaut du projet.
BEGIN;
ALTER TABLE public.datasets
    ADD COLUMN IF NOT EXISTS description varchar(1000) DEFAULT '',
    ADD COLUMN IF NOT EXISTS aliases varchar(2000) DEFAULT '';
ALTER TABLE public.dataset_fields
    ADD COLUMN IF NOT EXISTS description varchar(1000) DEFAULT '',
    ADD COLUMN IF NOT EXISTS aliases varchar(2000) DEFAULT '';
COMMIT;
