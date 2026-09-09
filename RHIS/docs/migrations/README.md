# Métadonnées métier du catalogue

`2026-09-08-catalogue-metier.sql` ajoute quatre colonnes facultatives sans modifier les données existantes. Le script est transactionnel et peut être exécuté plusieurs fois. Il cible `public`, le schéma par défaut des métadonnées : adapter explicitement la qualification si l'installation utilise un autre schéma.

Avant le déploiement du backend sur une base existante, exécuter ce script avec l'outil PostgreSQL de l'installation. Il n'est pas exécuté automatiquement par l'application. Les alias sont stockés un par ligne ; l'API d'administration les normalise et supprime les doublons insensibles à la casse. Description : 1000 caractères ; alias : 2000 au total, au maximum 20 de 100 caractères chacun. Les métadonnées omises dans une mise à jour restent inchangées ; une chaîne vide les efface.

Le profil de développement du projet utilise actuellement `ddl-auto=create` : il recrée le schéma au démarrage. Pour conserver la configuration, utiliser un profil persistant avec `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` après application des migrations nécessaires. Ne pas redémarrer une base contenant des données à conserver avec `create` ou `create-drop`. La configuration locale existante n'a pas été modifiée par cette fonctionnalité.

Après déploiement, ouvrir l'administration du catalogue et renseigner les définitions métier et synonymes utiles. Les libellés actuels restent les titres des rapports. Le synchroniseur conserve ces textes lors de la redécouverte des données.

Retour arrière : restaurer la version précédente du code ; les colonnes additives peuvent rester en place. Aucun effacement de métadonnées n'est requis.
