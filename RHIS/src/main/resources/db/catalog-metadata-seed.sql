-- Complète uniquement les valeurs absentes. Les alias sont séparés par des sauts de ligne.
WITH seed(source_name, description, aliases) AS (VALUES
    ('rhis_employee', 'Salariés, identité, dates de présence dans l''entreprise et restaurant de rattachement.', E'Employés\nSalariés\nPersonnel\nÉquipe'),
    ('rhis_restaurant', 'Restaurants et établissements, leur désignation, leur adresse et leurs références.', E'Restaurants\nÉtablissements\nPoints de vente'),
    ('rhis_contrat', 'Contrats de travail des salariés : dates, durée hebdomadaire et rémunération.', E'Contrats\nContrats de travail\nConditions d''emploi'),
    ('rhis_shift', 'Créneaux de travail prévus au planning, avec salarié, restaurant et horaires.', E'Planning\nPlannings\nCréneaux de travail\nServices planifiés'),
    ('rhis_pointage', 'Pointages des salariés : horaires enregistrés et temps pointé.', E'Pointages\nHeures pointées\nPrésences enregistrées'),
    ('rhis_absence_conge', 'Absences et congés des salariés, leurs dates, horaires, durées et statut.', E'Absences\nCongés\nAbsences et congés'),
    ('rhis_detail_evenement', 'Détail par date des événements rattachés aux absences et congés.', E'Détails des événements\nDétails des absences\nDétails des congés')
), defaults AS (
    SELECT d.id, coalesce(s.description, 'Informations relatives à « ' || d.display_name || ' ».') AS description,
           coalesce(s.aliases, d.display_name) AS aliases
    FROM public.datasets d LEFT JOIN seed s USING (source_name)
)
UPDATE public.datasets d
SET description = CASE WHEN coalesce(btrim(d.description), '') = '' THEN v.description ELSE d.description END,
    aliases = CASE WHEN coalesce(btrim(d.aliases), '') = '' THEN v.aliases ELSE d.aliases END
FROM defaults v WHERE d.id = v.id
  AND (coalesce(btrim(d.description), '') = '' OR coalesce(btrim(d.aliases), '') = '');

WITH seed(source_name, description, aliases) AS (VALUES
    ('uuid', 'Référence unique de cet élément.', E'Référence unique\nRéférence universelle'),
    ('emp_pk_id', 'Référence interne du salarié, distincte de son matricule.', E'Référence du salarié\nRéférence employé'),
    ('restau_pk_id', 'Référence interne du restaurant, distincte de son matricule.', E'Référence du restaurant\nRéférence établissement'),
    ('cont_pk_id', 'Référence unique du contrat de travail.', E'Référence du contrat\nNuméro du contrat'),
    ('shift_pk_id', 'Référence unique du créneau de travail planifié.', E'Référence du créneau\nRéférence du service planifié'),
    ('pointage_pk_id', 'Référence unique du pointage.', E'Référence du pointage\nNuméro du pointage'),
    ('absence_conge_pk_id', 'Référence unique de l''absence ou du congé.', E'Référence de l''absence\nRéférence du congé'),
    ('detail_event_pk_id', 'Référence unique du détail de l''événement.', E'Référence du détail\nNuméro du détail'),
    ('nom', 'Nom de famille du salarié.', E'Nom de famille\nPatronyme'),
    ('prenom', 'Prénom du salarié.', E'Prénom\nPrénom du salarié'),
    ('matricule', 'Matricule attribué à cet élément, distinct de sa référence interne.', E'Matricule\nNuméro matricule'),
    ('date_entree', 'Date d''entrée du salarié dans l''entreprise.', E'Date d''embauche\nDate d''entrée\nDébut d''emploi'),
    ('date_sortie', 'Date de sortie du salarié de l''entreprise, lorsqu''elle est renseignée.', E'Date de départ\nDate de sortie\nFin d''emploi'),
    ('statut', 'Indique si le salarié est actif.', E'Salarié actif\nStatut du salarié'),
    ('hebdo_courant', 'Durée hebdomadaire courante de travail du salarié.', E'Heures hebdomadaires courantes\nHoraire hebdomadaire actuel'),
    ('libelle', 'Désignation du restaurant.', E'Nom du restaurant\nNom de l''établissement\nDésignation'),
    ('adresse', 'Adresse du restaurant.', E'Adresse du restaurant\nAdresse de l''établissement'),
    ('code_pointeuse', 'Code de pointeuse associé au restaurant.', E'Code de pointeuse\nCode de badgeuse'),
    ('periode_restaurant', 'Période renseignée pour le restaurant.', E'Période du restaurant\nPériode de l''établissement'),
    ('hebdo', 'Durée hebdomadaire de travail prévue au contrat.', E'Heures hebdomadaires contractuelles\nHoraire du contrat'),
    ('tx_horaire', 'Taux de rémunération horaire prévu au contrat.', E'Taux horaire\nRémunération horaire\nSalaire horaire'),
    ('salaire', 'Montant du salaire renseigné dans le contrat ; la devise et la périodicité ne sont pas précisées ici.', E'Salaire\nRémunération contractuelle'),
    ('date_effective', 'Date de prise d''effet du contrat.', E'Début du contrat\nDate d''effet'),
    ('date_fin', 'Date de fin du contrat, de l''absence ou du congé concerné.', E'Date de fin\nFin de période'),
    ('actif', 'Indique si le contrat est actif.', E'Contrat actif\nStatut du contrat'),
    ('temps_partiel', 'Indique si le contrat prévoit un travail à temps partiel.', E'Temps partiel\nContrat à temps partiel'),
    ('date_journee', 'Date de la journée concernée par le planning ou le pointage.', E'Journée\nDate de travail'),
    ('heure_debut', 'Heure de début du créneau, pointage, événement ou absence concerné.', E'Heure de début\nHoraire de début'),
    ('heure_fin', 'Heure de fin du créneau, pointage, événement ou absence concerné.', E'Heure de fin\nHoraire de fin'),
    ('total_heure', 'Durée totale renseignée pour le créneau planifié, dans l''unité de la source.', E'Durée planifiée\nTotal du temps planifié'),
    ('from_planning_manager', 'Indique si le créneau provient du planning manager.', E'Issu du planning manager\nOrigine planning manager'),
    ('from_planning_leader', 'Indique si le créneau provient du planning leader.', E'Issu du planning leader\nOrigine planning leader'),
    ('create_from_reference', 'Indique si le créneau a été créé à partir d''un planning de référence.', E'Issu du planning de référence\nCréé depuis une référence'),
    ('temps_pointes', 'Temps enregistré par le pointage, dans l''unité de la source.', E'Temps pointé\nDurée pointée'),
    ('is_acheval', 'Indique si le pointage est à cheval sur deux journées.', E'Pointage à cheval\nPointage sur deux jours'),
    ('date_debut', 'Date de début de l''absence ou du congé.', E'Début de l''absence\nDébut du congé'),
    ('duree_jour', 'Durée de l''absence ou du congé exprimée en jours.', E'Durée en jours\nNombre de jours d''absence'),
    ('periode_horaire', 'Indique si l''absence ou le congé est défini sur une période horaire.', E'Absence horaire\nPériode horaire'),
    ('status', 'Statut renseigné pour l''absence ou le congé.', E'Statut de l''absence\nStatut du congé'),
    ('date_event', 'Date de l''événement détaillé.', E'Date de l''événement\nJour de l''événement'),
    ('nb_heure', 'Nombre d''heures renseigné pour le détail de l''événement.', E'Nombre d''heures\nHeures de l''événement'),
    ('repartition_heure', 'Répartition des heures renseignée pour le détail de l''événement.', E'Répartition des heures\nVentilation des heures'),
    ('emp_fk_id', 'Référence du salarié concerné.', E'Salarié concerné\nRéférence du salarié'),
    ('employee_fk_id', 'Référence du salarié affecté au créneau.', E'Salarié planifié\nRéférence du salarié'),
    ('id_employee', 'Référence du salarié ayant pointé.', E'Salarié pointé\nRéférence du salarié'),
    ('restau_fk_id', 'Référence du restaurant de rattachement du salarié.', E'Restaurant de rattachement\nÉtablissement du salarié'),
    ('restaurant_fk_id', 'Référence du restaurant du créneau planifié.', E'Restaurant du planning\nÉtablissement du créneau'),
    ('id_restaurant', 'Référence du restaurant concerné.', E'Restaurant concerné\nRéférence du restaurant'),
    ('id_shift', 'Référence du créneau planifié associé au pointage.', E'Créneau associé\nPlanning du pointage'),
    ('id_franchise', 'Référence de la franchise concernée.', E'Franchise\nRéférence de la franchise'),
    ('type_cont_fk_id', 'Référence du type de contrat.', E'Type de contrat\nCatégorie du contrat'),
    ('grp_trv_fk_id', 'Référence du groupe de travail.', E'Groupe de travail\nRéférence du groupe'),
    ('post_trav_fk_id', 'Référence du poste de travail prévu.', E'Poste de travail\nPoste planifié'),
    ('type_pointage_fk_id', 'Référence du type de pointage.', E'Type de pointage\nCatégorie du pointage'),
    ('type_event_fk_id', 'Référence du type d''événement associé à l''absence ou au congé.', E'Type d''événement\nCatégorie d''absence'),
    ('absence_conge_fk_id', 'Référence de l''absence ou du congé dont cet élément fournit le détail.', E'Absence associée\nCongé associé')
), defaults AS (
    SELECT f.id,
           coalesce(s.description, 'Information « ' || f.display_name || ' » de « ' || d.display_name || ' ».') AS description,
           coalesce(s.aliases, f.display_name) AS aliases
    FROM public.dataset_fields f JOIN public.datasets d ON d.id = f.dataset_id
    LEFT JOIN seed s ON s.source_name = f.source_name
)
UPDATE public.dataset_fields f
SET description = CASE WHEN coalesce(btrim(f.description), '') = '' THEN v.description ELSE f.description END,
    aliases = CASE WHEN coalesce(btrim(f.aliases), '') = '' THEN v.aliases ELSE f.aliases END
FROM defaults v WHERE f.id = v.id
  AND (coalesce(btrim(f.description), '') = '' OR coalesce(btrim(f.aliases), '') = '');
