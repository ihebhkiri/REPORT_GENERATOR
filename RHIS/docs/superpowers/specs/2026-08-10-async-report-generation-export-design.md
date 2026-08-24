# RHIS — génération asynchrone et export temporaire des rapports

Date : 2026-08-10  
Statut : design validé, non implémenté

## 1. Objectif

Ajouter un parcours en deux étapes après la configuration d’un rapport :

1. préparer de manière asynchrone un snapshot contenant toutes les lignes correspondant à la définition du rapport ;
2. permettre à l’utilisateur de transformer ce snapshot en PDF et/ou Excel, avec une progression distincte pour chaque export.

La cible de volume est comprise entre 10 000 et 50 000 lignes. Le PDF doit rester autorisé sur toutes les lignes malgré sa taille potentielle.

La preview existante reste indépendante, synchrone et limitée à six lignes.

## 2. Principes retenus

- Le navigateur transmet la définition du rapport une seule fois ; il ne transporte jamais les lignes complètes entre `/generate` et `/export`.
- La génération et l’export sont des ressources temporaires identifiées par UUID, et non des réponses volumineuses chaînées par Angular.
- La validation, la résolution du catalogue, les filtres, les tris et les jointures sont partagés avec la preview.
- L’exécution complète est réalisée en streaming et ne charge jamais 10 000 lignes dans une collection Java.
- PDF et Excel sont produits depuis le même snapshot immuable.
- Les données temporaires sont supprimées à la sortie de la page Export en best effort et obligatoirement par expiration serveur.
- L’utilisateur peut produire les deux formats tant qu’il reste sur la page Export.
- La configuration légère est conservée dans l’onglet afin que « Précédent » restaure exactement les colonnes, filtres et tris.

## 3. Parcours utilisateur

### 3.1 Depuis Configuration

Le bouton « Générer » utilise les mêmes conditions d’activation que « Aperçu » :

- au moins une colonne sélectionnée ;
- filtres valides ;
- aucune soumission déjà active.

Au clic :

1. Angular construit la définition ordonnée actuellement utilisée par la preview ;
2. il conserve cette définition dans `sessionStorage` ;
3. il appelle `POST /api/v1/report-generations` ;
4. le backend répond avec `202 Accepted` et un `generationId` ;
5. Angular navigue vers `/rapports/export/{generationId}`.

Si la création échoue, l’utilisateur reste sur Configuration et le `ProblemDetail` est affiché. Le bouton reste protégé contre les doubles clics.

### 3.2 Page Export — préparation des données

La page interroge `GET /api/v1/report-generations/{generationId}` toutes les deux secondes. Le polling s’arrête lors de la destruction du composant ou lorsque la génération devient `READY`, `FAILED` ou `EXPIRED`.

La page affiche :

- le pourcentage ;
- le nombre de lignes traitées et le total lorsqu’ils sont connus ;
- un libellé correspondant à la phase courante ;
- une action de nouvelle tentative en cas d’échec récupérable.

Un rafraîchissement de la page reprend le suivi grâce au `generationId` de l’URL.

### 3.3 Page Export — création du fichier

Lorsque la génération est `READY`, la page affiche le nombre total de lignes et les actions :

- « Exporter en PDF » ;
- « Exporter en Excel ».

Le choix d’un format crée un job d’export distinct. Une seconde progression est affichée jusqu’à ce que le fichier soit prêt. L’utilisateur peut ensuite le télécharger et produire l’autre format sans relancer la requête SQL.

Le téléchargement n’entraîne pas immédiatement la suppression du snapshot afin de permettre les deux formats.

### 3.4 Navigation et restauration

« Précédent » :

1. conserve la définition légère dans `sessionStorage` ;
2. demande la suppression de la génération et de ses fichiers temporaires ;
3. retourne vers Configuration ;
4. restaure source, tables liées, colonnes, filtres et tris après le chargement du catalogue.

Une navigation vers une autre fonctionnalité déclenche également la suppression en best effort. Une fermeture d’onglet ou une perte réseau peut empêcher cet appel ; l’expiration serveur reste donc la garantie de nettoyage.

## 4. API publique

### 4.1 Créer une génération

```http
POST /api/v1/report-generations
Content-Type: application/json
Idempotency-Key: 06aa6f18-4dcc-4455-86fc-bfc13a49b188
```

Le corps réutilise la structure métier de `ReportPreviewRequest` :

```json
{
  "rootDatasetId": 1,
  "selectedFieldIds": [11, 12, 18],
  "filters": [],
  "sorts": []
}
```

Réponse `202 Accepted` :

```json
{
  "generationId": "66a6b933-3bd8-4d9b-99ae-4eac2d95f73a",
  "status": "PENDING",
  "progress": 0,
  "createdAt": "2026-08-10T18:00:00Z",
  "expiresAt": "2026-08-10T18:30:00Z"
}
```

Le client fournit une clé d’idempotence UUID par tentative afin qu’une répétition réseau ne crée pas deux jobs. La clé est unique par utilisateur.

### 4.2 Consulter une génération

```http
GET /api/v1/report-generations/{generationId}
```

```json
{
  "generationId": "66a6b933-3bd8-4d9b-99ae-4eac2d95f73a",
  "status": "RUNNING",
  "phase": "READING_ROWS",
  "progress": 64,
  "processedRowCount": 25600,
  "totalRowCount": 40000,
  "createdAt": "2026-08-10T18:00:00Z",
  "expiresAt": "2026-08-10T18:30:00Z"
}
```

États :

```text
PENDING → RUNNING → READY
                  ↘ FAILED
PENDING/READY/FAILED → EXPIRED
```

### 4.3 Supprimer une génération

```http
DELETE /api/v1/report-generations/{generationId}
```

Réponse `204 No Content` lorsque la ressource existe et appartient à l’utilisateur, sinon `404 Not Found`. Répéter la suppression reste sans effet secondaire même si la seconde réponse est `404`. Elle annule les travaux encore actifs et supprime en cascade snapshot, exports et métadonnées associées.

### 4.4 Créer un export

```http
POST /api/v1/report-generations/{generationId}/exports
Content-Type: application/json

{
  "format": "PDF"
}
```

Formats V1 : `PDF` et `XLSX`.

Réponse `202 Accepted` :

```json
{
  "exportId": "8574b46f-a936-46e7-92a9-cc49ec41b652",
  "generationId": "66a6b933-3bd8-4d9b-99ae-4eac2d95f73a",
  "format": "PDF",
  "status": "PENDING",
  "progress": 0
}
```

Créer plusieurs fois le même format pour une génération réutilise l’export `READY` existant ou refuse la duplication lorsqu’un job équivalent est actif.

### 4.5 Consulter et télécharger un export

```http
GET /api/v1/report-exports/{exportId}
GET /api/v1/report-exports/{exportId}/file
```

États :

```text
PENDING → RUNNING → READY
                  ↘ FAILED
```

Le fichier n’est téléchargeable qu’en état `READY`. Le backend définit le nom, le `Content-Type` et `Content-Disposition`; aucun chemin de stockage n’est exposé.

## 5. Modèle de données temporaire

PostgreSQL contient seulement les métadonnées des jobs et leur propriété :

```text
report_generation
  id UUID PK
  owner_user_id FK NOT NULL
  idempotency_key UUID NOT NULL
  definition_json JSONB NOT NULL
  status NOT NULL
  phase
  progress
  processed_row_count
  total_row_count
  snapshot_location
  created_at
  expires_at
  heartbeat_at
  error_code

report_export
  id UUID PK
  generation_id FK NOT NULL
  format NOT NULL
  status NOT NULL
  progress
  file_location
  created_at
  error_code
```

Contraintes importantes :

- unicité `(owner_user_id, idempotency_key)` ;
- unicité `(generation_id, format)` tant que l’export est actif ou prêt ;
- index sur `owner_user_id`, `status` et `expires_at` ;
- suppression en cascade depuis `report_generation`.

Les lignes du rapport ne sont pas stockées comme entités JPA.

## 6. Snapshot et stockage

Le worker écrit un snapshot NDJSON compressé, accompagné de métadonnées de colonnes ordonnées. NDJSON permet une lecture et une écriture ligne par ligne tout en conservant nombres, booléens, chaînes et `null`.

En production multi-instance, le snapshot et les exports sont placés dans un stockage objet partagé tel que S3. Un disque local temporaire est acceptable uniquement pour le développement mono-instance.

Les objets ne sont jamais publics. Le téléchargement passe par une autorisation backend ou une URL signée de très courte durée créée uniquement après contrôle du propriétaire.

Expiration V1 : 30 minutes après le passage à `READY` ou après la dernière activité d’export. Un job `RUNNING` possède un heartbeat et n’est pas expiré tant que son worker est actif. Un heartbeat trop ancien fait passer le job à `FAILED`.

À l’expiration, le snapshot et les exports sont supprimés et la génération passe à `EXPIRED`. Les métadonnées minimales sans définition ni emplacement de stockage peuvent être conservées 24 heures afin que l’interface distingue une expiration d’un identifiant inconnu, puis elles sont purgées.

## 7. Architecture backend

Le module `report` existant est conservé. La responsabilité actuellement concentrée dans `ReportPreviewService` est séparée uniquement là où la génération exige une réutilisation réelle :

```text
ReportDefinitionResolver
  requête publique → ResolvedReportDefinition validée

ReportSqlBuilder
  ├── buildPreview(definition) → SELECT ... LIMIT 7
  ├── buildCount(definition)   → COUNT(*) avec mêmes joins/filtres
  └── buildFull(definition)    → SELECT complet ordonné

ReportPreviewExecutor
  requête preview → réponse JSON limitée à six lignes

ReportGenerationWorker
  count + requête complète streaming → snapshot

ReportExportWorker
  snapshot → PDF ou XLSX
```

`ReportDefinitionResolver` reprend :

- résolution batch des champs ;
- validation visibilité/activité/types ;
- parsing des valeurs ;
- règles des opérateurs et arités ;
- résolution des jointures FK sortantes directes ;
- validation des tris et doublons.

Les noms de tables et colonnes continuent de provenir exclusivement du catalogue. Les valeurs restent bindées. La preview conserve son timeout de cinq secondes.

La requête complète utilise un timeout configurable plus long, un `fetchSize` PostgreSQL et un callback ligne par ligne. Elle ne retourne pas de `List<Map<...>>` contenant toutes les lignes.

Le comptage est exécuté avant la lecture afin de fournir un pourcentage réel. La progression est monotone et répartie entre validation, comptage, lecture et finalisation du snapshot.

## 8. Exécution asynchrone et concurrence

Pour la V1, Spring utilise un `TaskExecutor` explicitement borné. Le pool, la file et le nombre de jobs actifs par utilisateur sont configurables afin qu’un PDF volumineux ne sature pas les requêtes HTTP.

Le traitement long n’est pas entouré d’une unique transaction JPA. Les transitions d’état et la progression utilisent de petites transactions séparées.

Si le besoin évolue vers une reprise garantie après crash ou une forte concurrence, le worker pourra être remplacé par une file durable. Kafka ou RabbitMQ ne sont pas introduits dans la V1 sans ce besoin.

## 9. Génération PDF et Excel

### Excel

Le fichier XLSX est écrit avec une API streaming : fenêtre de lignes limitée, styles réutilisés, colonnes ordonnées selon la définition et valeurs typées.

### PDF

Le PDF contient toutes les lignes, même jusqu’à 50 000. La génération est paginée et progressive, avec répétition des en-têtes de colonnes. Elle applique des limites de concurrence et de taille technique configurables afin de protéger le service, mais ne tronque pas silencieusement les résultats.

Le renderer PDF utilise JasperReports 7.0.7 avec un JRXML de branding RHIS, une table construite dynamiquement depuis les métadonnées du snapshot et un virtualizer disque pour borner la mémoire. JasperReports est distribué sous LGPL et son module PDF s’appuie sur OpenPDF ; ce choix de dépendance reste interne et ne modifie pas le contrat public.

## 10. Sécurité et erreurs

- Toutes les ressources exigent un utilisateur authentifié.
- Toute lecture, suppression, export ou téléchargement vérifie `owner_user_id`.
- Un UUID valide appartenant à un autre utilisateur répond comme une ressource introuvable afin de ne pas révéler son existence.
- La définition est revalidée à la création de la génération ; une preview réussie n’est pas une autorisation suffisante.
- Les erreurs publiques utilisent `ProblemDetail` et n’exposent ni SQL, ni chemin de stockage, ni stack trace.
- Les erreurs techniques détaillées sont journalisées avec `generationId` ou `exportId`.
- Un échec d’export conserve le snapshot jusqu’à expiration et autorise une nouvelle tentative.
- Un job expiré redirige vers Configuration avec restauration du brouillon de l’onglet.
- Une perte réseau suspend visuellement le suivi et reprend le polling sans recréer le job.

## 11. Frontend Angular

Nouveaux éléments limités à la feature `rapports` :

- route lazy `/rapports/export/:generationId` ;
- page `ReportExportComponent` ;
- modèles stricts des états de génération et d’export ;
- service HTTP pour créer, suivre, supprimer et télécharger ;
- stockage de la définition légère dans `sessionStorage` ;
- restauration différée après chargement des datasets et champs.

RxJS gère :

- la création HTTP ;
- le polling avec annulation à la destruction ;
- les reprises réseau contrôlées ;
- l’arrêt immédiat sur état terminal.

Les Signals portent les états synchrones d’affichage dérivés. Aucun store global n’est ajouté pour cette V1.

## 12. Tests et critères d’acceptation

### Backend

- La preview reste limitée par `LIMIT 7` et retourne six lignes maximum.
- La requête complète ne contient aucune limite et conserve colonnes, filtres, tris et jointures.
- Le comptage et la lecture utilisent la même définition logique.
- Les paramètres restent bindés et les identifiants proviennent du catalogue.
- Le worker traite 0, 1, 10 000 lignes sans accumulation complète en mémoire.
- La progression ne diminue jamais et atteint 100 uniquement après finalisation.
- Un utilisateur ne peut pas consulter, supprimer ou télécharger les ressources d’un autre.
- Une clé d’idempotence répétée ne crée pas deux générations.
- Les suppressions explicites et expirations nettoient métadonnées et objets.
- La disparition d’un worker ne laisse pas indéfiniment un job `RUNNING`.
- PDF et XLSX contiennent toutes les lignes et respectent l’ordre des colonnes et des tris.
- Une erreur d’export ne détruit pas le snapshot.

### Frontend

- « Générer » n’est actif que pour une configuration valide.
- Un clic valide crée un seul job et navigue avec son identifiant.
- Une erreur de création conserve la page Configuration.
- Le polling s’arrête sur état terminal et à la destruction du composant.
- Un rafraîchissement reprend la génération ou l’export.
- Les deux progressions sont présentées séparément.
- PDF et Excel peuvent être produits depuis le même snapshot.
- « Précédent » restaure exactement la définition et déclenche le nettoyage.
- Une génération expirée restaure le brouillon et explique la situation.
- Aucun tableau complet de données n’est placé dans le Router state ou `sessionStorage`.

### Validation

- Tests unitaires Java des résolveurs, builders, transitions d’état et workers.
- Tests PostgreSQL/Testcontainers pour comptage, streaming et concurrence.
- Tests d’intégration du stockage temporaire avec une implémentation de test isolée.
- Tests HTTP de sécurité, idempotence, polling, suppression et téléchargement.
- Tests Angular des services, polling, restauration, erreurs et navigation.
- Mesure mémoire sur un export de 50 000 lignes.

## 13. Hors périmètre

- Historique durable des rapports.
- Sauvegarde de modèles de rapports.
- Partage entre utilisateurs.
- Envoi par email.
- Planification récurrente.
- Export CSV.
- Reprise exacte d’un flux au milieu d’une ligne après crash du worker.
- Kafka, RabbitMQ ou orchestration distribuée des jobs en V1.

## 14. Ordre de réalisation recommandé

1. Extraire la résolution/validation partagée sans modifier le comportement de preview.
2. Introduire les entités et endpoints de génération avec sécurité propriétaire.
3. Implémenter le streaming vers le snapshot et sa progression.
4. Ajouter la page Export et la restauration de configuration.
5. Implémenter XLSX en streaming.
6. Implémenter PDF paginé sur toutes les lignes.
7. Ajouter suppression, expiration, reprise des jobs abandonnés et tests de charge.
