# RHIS — documentation ciblée du flux de rapports

Date : 2026-08-11  
Statut : approuvé pour implémentation

## Objectif

Ajouter une documentation concise au flux de configuration et de preview des rapports afin qu’un développeur comprenne les responsabilités, règles métier non évidentes, transformations et contraintes techniques sans devoir reconstruire ces invariants depuis l’implémentation.

La documentation ne change aucun comportement, contrat, endpoint, état Angular, requête SQL ou transaction.

## Périmètre frontend

- `ConfigurationComponent` : responsabilité d’orchestration de page et séparation avec le chargement de métadonnées.
- `ReportConfigurationLoader` : résolution de la source principale, validation des relations sortantes directes, ordre des datasets et transformation des champs backend en modèles de configuration.
- `DatasetService` et `ReportPreviewService` : documenter uniquement si une contrainte de transport non évidente mérite d’être exposée ; ne pas commenter les simples appels HTTP.

Les commentaires frontend utilisent des blocs TSDoc `/** ... */` et expliquent les règles ou responsabilités, jamais la syntaxe Angular/RxJS.

## Périmètre backend

- `ReportPreviewService` : orchestration, validation d’accès, relations directes et règles de sélection/tri.
- `ReportSqlBuilder` : requête paramétrée, ordre déterministe, échappement `LIKE` et récupération d’une ligne supplémentaire pour `hasMore`.
- `ReportPreviewExecutor` : limite visible, timeout JDBC, réduction des sept lignes à six et conversions JDBC explicites.
- `ReportQueryModel` : rôle des records résolus et différence entre définition validée et requête SQL préparée.

Les contrôleurs minces, DTO évidents, exceptions simples, constructeurs et méthodes triviales restent sans Javadoc.

## Règles rédactionnelles

- Expliquer pourquoi une contrainte existe ou quelle règle est appliquée.
- Documenter une hypothèse uniquement si sa violation pourrait modifier le résultat, la sécurité ou l’ordre.
- Ne pas paraphraser le nom d’une méthode ou ses instructions.
- Ne pas ajouter de commentaire ligne par ligne.
- Préférer une documentation de classe ou de record ; commenter une méthode privée seulement lorsque sa règle ne ressort pas clairement de sa signature.
- Garder chaque bloc court et vérifiable contre le code.

## Invariants à rendre explicites

- Les datasets liés sont accessibles uniquement par une clé étrangère sortante directe depuis la source principale.
- Le frontend conserve la source principale en premier et trie les sources liées par libellé métier.
- L’ordre backend des champs sélectionnés doit être conservé jusqu’aux colonnes du résultat.
- Les filtres et valeurs SQL restent paramétrés ; les identifiants proviennent du catalogue validé et sont échappés séparément.
- Sans tri utilisateur, les clés primaires de la source principale fournissent un ordre stable lorsque disponibles.
- Le SQL récupère sept lignes afin que l’exécuteur expose au maximum six lignes et calcule `hasMore` sans requête de comptage.
- Le timeout de preview est appliqué au `PreparedStatement` JDBC.

## Validation

- Aucun changement fonctionnel dans le diff.
- Aucun commentaire trivial ou devenu faux par rapport au code.
- Compilation frontend et backend réussie.
- Suites de tests pertinentes inchangées et réussies.
