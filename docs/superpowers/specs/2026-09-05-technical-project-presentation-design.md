# Présentation technique du projet RHIS

## Objectif

Créer une présentation PowerPoint professionnelle de 15 minutes destinée à un superviseur technique ou un team lead. Le deck doit expliquer le système réellement implémenté, ses responsabilités, ses flux critiques et ses compromis sans langage marketing ni fonctionnalité inventée.

## Sources vérifiées

- Code Angular sous `Frontend/Rhis_report_gen`.
- Code Spring Boot sous `RHIS/src/main`.
- Tests frontend et backend sous les répertoires `src/test`.
- Documentation des flux sous `RHIS/docs/flows`.
- Dépendances déclarées dans `Frontend/Rhis_report_gen/package.json` et `RHIS/pom.xml`.
- Règles visuelles du projet dans `DESIGN.md`.

## Architecture constatée

Le frontend Angular 20 utilise des standalone components organisés par feature. Les Signals portent l'état synchrone de l'interface. RxJS orchestre les appels HTTP, le debounce de preview et le polling des traitements asynchrones. Le brouillon de configuration reste dans le `sessionStorage` du navigateur.

Le backend Spring Boot 4.1 expose des controllers REST protégés par Spring Security. Les services résolvent et valident les définitions de rapport, construisent les requêtes SQL, créent des jobs persistés et exécutent la génération dans le même processus JVM. JPA gère les métadonnées et les états de job. JDBC lit les données métier. Le filesystem stocke temporairement snapshots et exports.

PostgreSQL contient le catalogue exposable (`datasets`, `dataset_fields`), les utilisateurs et rôles, les états de génération et d'export, ainsi que les tables métier RH. Les relations de datasets proviennent des foreign keys visibles dans `information_schema`.

L'assistant appelle XKiro via son endpoint compatible OpenAI et l'adapter OpenAI de Spring AI. La configuration courante sélectionne le modèle `qwen/qwen3-max:free`. Le modèle reçoit une version compacte du catalogue autorisé, la date et la demande utilisateur. Il retourne une structure typée ou une question de clarification. Il ne reçoit ni ligne métier, ni nom physique SQL, ni requête SQL. `ReportDefinitionResolver` revalide ensuite tous les IDs, types, opérateurs, valeurs, tris et chemins de jointure. Une seule tentative de correction LLM est permise avant un échec métier.

## Direction narrative retenue

Le deck suit une narration « architecture puis flux ». Elle convient à un public technique car elle établit d'abord les frontières de responsabilité, puis montre comment une demande traverse le système. Le mode manuel et le mode LLM convergent vers le même pipeline backend.

## Structure des slides

1. **RHIS Report Generator** — titre, périmètre et technologies principales.
2. **Problème métier et objectif** — besoin de rapports RH configurables sans exposer directement la structure SQL.
3. **Périmètre fonctionnel réel** — parcours manuel, assistant naturel, administration de l'exposition, formats PDF/XLSX.
4. **Architecture globale** — diagramme Angular, API Spring, PostgreSQL, stockage temporaire et XKiro.
5. **Architecture frontend** — features, composants, services, Signals et RxJS.
6. **Architecture backend** — controllers, services, resolver, SQL builder, workers, JPA et JDBC.
7. **Modèle de données et métadonnées** — utilisateurs/rôles, datasets/fields, générations/exports et principales relations RH.
8. **Parcours utilisateur principal** — sélection, configuration, preview, génération et export.
9. **Pipeline de génération et d'export** — validation, transaction, dispatch après commit, snapshot, writers et téléchargement.
10. **Assistant LLM sous contrôle** — catalogue, structured output, clarification, correction unique et convergence vers le pipeline existant.
11. **Validation, sécurité et autorisation** — cookies JWT, endpoints authentifiés, rôle ADMIN, ownership, idempotence et validation backend.
12. **Décisions techniques et difficultés résolues** — décisions importantes et problèmes concrets traités.
13. **Démonstration, limites et suite** — scénario de démonstration, limites actuelles et améliorations possibles.

## Direction visuelle

- Format 16:9, fond principalement blanc avec slides d'ouverture et de conclusion bleu nuit.
- Palette issue de l'interface RHIS : bleu nuit dominant, turquoise comme accent, gris neutres pour la structure.
- Typographie Office sûre : Cambria pour les titres et Arial pour le corps.
- Une idée principale par slide, titres de 32 à 42 pt et corps d'au moins 17 pt.
- Diagrammes éditables pour l'architecture, le workflow, le pipeline LLM et le modèle simplifié.
- Captures réelles de l'application lorsque l'environnement permet d'afficher les écrans représentatifs.
- Aucun mur de texte, aucune décoration gratuite, aucun faux indicateur ou chiffre inventé.

## Démonstration prévue

1. Connexion.
2. Sélection d'un dataset principal et d'une source liée.
3. Choix des colonnes, filtre, tri et preview.
4. Lancement d'une génération puis suivi de son état.
5. Export XLSX ou PDF et téléchargement.
6. Variante courte avec l'assistant en langage naturel et, si pertinent, une clarification.

## Limites à présenter explicitement

- Aucun template de rapport partagé ou persistant en base.
- Brouillon manuel limité au `sessionStorage`.
- Jobs exécutés par un `TaskExecutor` dans la JVM, sans broker ni worker externe.
- Snapshots et exports stockés sur le filesystem local temporaire.
- Aucun historique conversationnel serveur pour l'assistant.
- Deux appels LLM maximum par demande.
- Le format d'export provient du champ de requête, pas du texte naturel.
- Le frontend ne met pas en œuvre de refresh token automatique.
- La configuration courante contient la clé XKiro en clair dans `application.yaml`; elle doit être révoquée puis externalisée avant un déploiement.

## Critères d'acceptation

- Le fichier `.pptx` s'ouvre sans erreur et reste éditable.
- Les 13 slides tiennent dans une présentation de 15 minutes.
- Chaque affirmation fonctionnelle importante peut être reliée au code ou à la documentation vérifiée.
- Les diagrammes décrivent les composants et flux réellement présents.
- Les captures utilisées proviennent de l'application actuelle.
- Le deck passe la validation structurelle et une inspection visuelle de toutes les slides.

## Hors périmètre

- Ajouter ou modifier une fonctionnalité applicative.
- Présenter un déploiement Kubernetes ou AWS non démontré dans le dépôt.
- Inventer des métriques d'usage, de performance ou de gains métier.
- Transformer les limites actuelles en fonctionnalités déjà livrées.
