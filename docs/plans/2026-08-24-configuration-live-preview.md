# Intégrer un aperçu automatique à la configuration des rapports

This ExecPlan is a living document governed by `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` current throughout implementation.

Date de référence demandée : 2026-08-24  
Rédigé le : 2026-08-29 (Europe/Berlin)  
Status: Implemented — validations automatisées terminées, qualification manuelle partielle  
Research: `docs/research/2026-08-24-configuration-live-preview.md`  
Related issue: N/A — demande utilisateur

L'ExecPlan et les décisions D1–D5 ont été approuvés explicitement avant le premier changement source.

## Purpose and observable outcome

Après une modification valide des colonnes, de leur ordre, des filtres ou des tris/priorités, l’utilisateur voit automatiquement un aperçu intégré après 300 ms de stabilité. Il n’ouvre et ne ferme plus de popup.

À partir de 48rem, la configuration occupe le haut de l’espace de travail, les actions restent accessibles et « Aperçu en temps réel » occupe une ligne de layout ancrée en bas. En dessous de 48rem, deux onglets Configuration/Aperçu permettent d’utiliser l’espace disponible. L’actualisation continue lorsque la table est cachée ou réduite. La génération utilise toujours la définition courante, pas la définition d’un ancien résultat affiché.

« Temps réel » signifie ici après modification de configuration : aucun polling, WebSocket ou rafraîchissement périodique de la base.

## Scope and non-goals

In scope :

- transformer le présentateur de preview existant en panneau sans dialog ;
- remplacer le déclenchement manuel par une orchestration locale avec debounce, annulation et déduplication ;
- layout intégré desktop/tablette, réduction/développement et onglets mobiles accessibles ;
- conserver les états et la saisie incomplète entre présentations ;
- messages de preview sûrs et inline, tests, documentation des flows 02/03.

Non-goals :

- aucun changement backend, SQL, autorisation, endpoint, contrat HTTP ou modèle public ;
- aucune modification des règles de colonnes/filtres/tris ni du drag existant de ColumnSelector ;
- aucune nouvelle dépendance, Store, Facade, service d’état, cache global ou abstraction générique ;
- aucune nouvelle persistance, aucun remplacement de Générer par Enregistrer ;
- aucun resize manuel du panneau, drag sur la table, pagination ou changement de formatage ;
- aucune refonte du shell global, de l’administration ou de l’export ;
- aucune correction opportuniste des échecs de tests/budgets préexistants.

Contraintes : Angular installé 20.3.26, PrimeNG installé 20.4.0, OnPush, standalone, TS/HTML/SCSS séparés ; Signals synchrones et computed pour les dérivés ; RxJS limité à l’orchestration concrète ; tokens et espacements existants (4/8/16/24/32 px).

## Current behavior

Racine effective : `C:/Users/Surface Pro/Downloads/RHIS`. **F** = `Frontend/Rhis_report_gen` ; **C** = `Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration`. Tout chemin abrégé ci-dessous se développe avec ces préfixes.

La recherche liée donne les preuves détaillées. Points nécessaires pour reprendre sans conversation :

1. La route `configuration/:datasetId` charge directement `ConfigurationComponent`, sans SharedPageLayout.
2. `ReportConfigurationLoader.load` résout les datasets sortants directs et charge les champs supportés. Le parent possède sélection/filtres exécutables/tris, loading/error et preview.
3. `updateSelectedFields` purge les filtres/tris des colonnes retirées. `updateFilters` conserve les derniers filtres valides si le brouillon est invalide. Les FormControl incomplets restent dans FilterEditor.
4. `createPreviewRequest` renvoie rootDatasetId, selectedFieldIds ordonnés, filters, sorts ordonnés. Le conserver comme constructeur unique pour preview/génération.
5. `openPreview → loadPreview → ReportPreviewService.preview` poste avec credentials sur `/reports/preview` sous la base API. Le backend renvoie jusqu’à 6 lignes, sans total métier.
6. `PreviewDialogComponent` contient déjà table/formatage/loading/empty/error/retry/stale. Les lignes restent affichées pendant refresh/échec. Seule la destruction de la page annule actuellement HTTP.
7. `continueToExport` sauvegarde le draft version 1, démarre la génération avec un UUID d’idempotence, puis navigue vers `/rapports/export/:generationId`. Une preview réussie n’est pas un prérequis.
8. Le test `C/configuration.component.spec.ts:191`, **`never calls preview automatically when columns, filters or sorts change`**, impose l’ancien comportement : le remplacer. Conserver le test de purge ligne 268, mais adapter son assertion zéro appel ligne 290.
9. MessageService et p-toast existent déjà globalement. Aucune nouvelle notification n’est nécessaire.
10. Les docs historiques sont partiellement obsolètes : Générer est actif ; SecurityConfig exige authenticated pour reports/datasets. Le code courant prévaut.

Les commandes Git de ce checkout renvoient un seul dépôt parent, malgré la documentation en submodules. Revérifier avant branche/worktree ; ne pas créer artificiellement un second dépôt.

## Proposed approach

### Responsabilités et instance unique

Conserver l’orchestration dans ConfigurationComponent. Renommer les quatre fichiers `preview-dialog.component.*` en `components/preview-panel/preview-panel.component.*`, classe `PreviewPanelComponent`, selector `app-preview-panel`. Une seule occurrence de ce composant dans le template de configuration, un seul p-table dans son template.

Le présentateur garde les inputs existants utiles response/loading/error/stale et l’output retry ; retirer visible/visibleChange/close, DialogModule, Fermer et le masque. Ajouter seulement les données de présentation nécessaires : état textuel, raison d’incomplétude, réduction du corps et désactivation du retry. Aucun service HTTP injecté.

La page calcule `previewStatus` avec computed pour partager le même libellé entre l’en-tête du panneau et l’onglet mobile. Type local exporté depuis le fichier du panneau : `'À jour' | 'Mise à jour…' | 'Aperçu précédent' | 'Configuration incomplète' | 'Erreur' | 'En attente'`. Pas de nouveau fichier de modèle public.

L’en-tête du présentateur contient titre, état, compteur `returnedRowCount` dès qu’une réponse existe, indication que le compteur porte sur un résultat précédent si nécessaire, et retry après erreur. `hasMore` peut être explicité par « échantillon » ; ne pas afficher un total ou inventer une pagination.

La réduction est un Signal de la page, initialement false. Projeter le bouton de réduction via un unique emplacement `[preview-actions]` dans l’en-tête : le panneau reste un présentateur, son seul événement métier est retry. Le bouton du parent contrôle le corps identifié `report-preview-body`, avec aria-expanded/aria-controls et un nom explicite. Le corps utilise hidden sans détruire le tableau ; le header, le message d’erreur et le retry restent visibles réduit.

Conserver formatValue et l’ordre API. Restreindre les annonces live à l’état/messages, pas à toutes les cellules. Aucun autofocus après HTTP. Le conteneur de table peut scroller et être focalisé au clavier avec un nom accessible ; ne pas ajouter de rôles ARIA grid à un tableau qui n’implémente pas cette interaction.

### Layout desktop/tablette : >= 48rem

Seuil retenu : Tailwind md=48rem (768 CSS px avec police de référence 16 px), déjà utilisé par ColumnSelector ; lg=64rem reste le seuil existant des trois colonnes de configuration.

Conserver le header propre à la page (hauteur h-16). Transformer la zone sous ce header en grille occupant le reste de 100dvh, avec trois lignes **dans le flux** :

```text
[ configuration défilable : titre, étapes, colonnes/filtres/tris ]
[ actions : Générer, Précédent, erreurs de génération             ]
[ Aperçu en temps réel : en-tête permanent + corps défilable      ]
```

Grille développée : `minmax(0, 1fr) auto minmax(0, 1fr)` ; réduite : `minmax(0, 1fr) auto auto`. Les deux régions partagent l’espace restant après les actions. Le tableau conserve 360 px comme plafond existant, mais son conteneur doit pouvoir rétrécir à l’espace disponible. Ce partage évite une hauteur fixe additionnelle ou un calcul JS de pixels.

Mettre min-height:0 et min-width:0 aux limites de grille/flex nécessaires ; overflow-y:auto pour la configuration et le corps de preview. Les actions ont une hauteur intrinsèque et peuvent revenir à la ligne. Le titre et les étapes sont dans la partie défilable pour ne pas consommer en permanence l’espace d’une fenêtre basse.

Aucun position:fixed/sticky superposé pour le panneau ; aucune réserve artificielle de padding-bottom basée sur sa hauteur. Le panneau reste ancré parce que sa ligne est dans la grille. Le header général reste hors des deux scrolls. Le focus vers un contrôle doit faire défiler sa région sans le placer sous une autre ligne.

Un viewport très bas à 200 % doit conserver des régions utilisables ; si la qualification montre que le partage ne suffit pas, adapter localement le layout au contenu en gardant tous les contrôles accessibles, puis documenter la mesure. Ne pas masquer un contrôle ni réduire automatiquement le panneau après une édition.

### Mobile : < 48rem

Revenir au scroll naturel de la page, sans panneau permanent ni hauteur de workspace figée. Deux onglets Configuration/Aperçu, Configuration initialement sélectionné. Une seule barre d’actions commune, hors des deux contenus masquables, reste accessible depuis chaque onglet.

**Choix proposé : deux boutons natifs avec le pattern accessible tabs, localement dans ConfigurationComponent.** Ce n’est pas un nouveau composant générique.

PrimeNG 20.4.0 a été vérifié dans les fichiers installés et la [documentation officielle](https://v20.primeng.org/tabs). TabsModule/value/valueChange/lazy/selectOnFocus existent. TabPanel conserve son contenu avec lazy=false, mais impose hidden sur tout panneau inactif, ainsi que role/labels d’onglets. Il n’offre pas de mode affichant deux panneaux simultanément sur desktop.

Alternatives évaluées :

| Option | Conséquence | Décision proposée |
|---|---|---|
| Deux onglets natifs + deux sections stables | Quelques bindings et un handler clavier ; rôles adaptés au mobile, aucun démontage | Retenue pour ce cas à deux sections |
| PrimeNG Tabs persistant + forcer ses panneaux inactifs visibles sur desktop | Surcharges de hidden/host/ARIA dépendantes du composant | Écartée pour éviter un comportement accessible incohérent |
| Branche mobile Tabs et branche desktop avec templates réutilisés | Pas de duplication textuelle nécessaire, mais instances des éditeurs recréées au changement de branche | Écartée : perd le filtre incomplet et le focus |

Les deux sections restent montées en permanence pendant l’édition : utiliser hidden pour le contenu inactif mobile, jamais @if/isMobile autour des éditeurs, jamais deux branches de p-table. Sur desktop les deux sections sont visibles et nommées comme régions ordinaires.

Un seul `matchMedia('(min-width: 48rem)')` natif, initialisé depuis le document de la page, actualise un Signal `isMobile`. Retirer le listener via DestroyRef. Aucun abonnement responsive ne déclenche schedulePreview, loadConfiguration, resetReportDefinition ou save draft. Le même seuil est repris en SCSS.

État local `activeMobileTab: 'configuration' | 'preview'`. Sélection conservée aux changements de largeur ; la première entrée mobile sans choix antérieur reste Configuration. Le Signal de réduction desktop est indépendant ; le corps de preview est toujours développé dans l’onglet Aperçu mobile sans modifier la préférence desktop.

Pattern [WAI-ARIA Tabs](https://www.w3.org/WAI/ARIA/apg/patterns/tabs/) :

- tablist avec nom « Configuration du rapport », deux button type=button role=tab ;
- IDs stables, aria-controls vers les sections, aria-selected et tabindex 0/-1 ;
- sections role=tabpanel uniquement en mobile, aria-labelledby vers le bouton correspondant ;
- gauche/droite cyclent et activent l’onglet local sans délai ; Home/End vont au premier/dernier ; Entrée/Espace passent par le click natif ; Tab quitte normalement la liste ;
- le focus reste sur le bouton choisi ; une modification de configuration ne change jamais l’onglet ;
- si un changement de breakpoint masque l’élément focalisé, déplacer uniquement ce focus vers l’onglet actif ou le titre/contrôle de région visible correspondant, sans réinitialiser le contenu ;
- l’onglet Aperçu affiche aussi le statut textuel loading/error/précédent. Ne pas utiliser seulement une couleur ou une animation ;
- prévoir focus visible, cibles d’au moins 44 px lorsque possible et aucune duplication d’annonce live entre le tab visible et le panneau actif.

Les overlays PrimeNG attachés à body doivent se fermer lors d’une interaction normale qui quitte leur contrôle ; vérifier particulièrement un changement de largeur pendant un picker ouvert. Si une fuite de focus reproductible exige un changement dans un éditeur, revenir au plan avec la preuve avant d’élargir les fichiers affectés.

### Orchestration automatique dans la page

Remplacer openPreview/loadPreview manuels par une seule souscription locale. Garder les Signals pour configuration, réponse, erreur, loading/stale et UI responsive.

Utiliser un `Subject` privé typé d’intentions `{request: ReportPreviewRequest | null, immediate: boolean}`. Il ne représente pas une seconde source de vérité : les intentions sont des captures immuables construites par `createPreviewRequest` depuis les Signals. Un `null` signifie « annuler sans demander d’aperçu ».

`schedulePreview(immediate = false)` est appelé après les mutations **complètes** de updateSelectedFields, updateFilters, updateSorts. Il est aussi appelé aux frontières du chargement/reset/restauration et de la génération ci-dessous. Pas d’appel depuis un onglet ou la réduction.

Conditions d’éligibilité : dataset principal connu, au moins une colonne, filtres valides, pas de chargement/erreur initiale et pas de génération en cours. **Ne pas inclure previewLoading dans l’éligibilité automatique** : une nouvelle définition doit remplacer une requête active.

Ordre du pipeline, à conserver :

1. Capturer la requête courante ou null ; aucun filtre ne doit supprimer les null avant switchMap.
2. Dédupliquer les intentions ordinaires selon leur contenu ordonné (racine, IDs, tuples de filtres et valeurs, tuples de tris). Une sérialisation JSON de ces tableaux suffit ; ne pas trier les listes sémantiques. Les intentions immediate de retry contournent cette déduplication. Null est distinct de toute requête, permettant valide A → invalide → valide A.
3. Un **switchMap externe immédiat** annule l’ancien timer ou HTTP dès la réception d’une intention. Dans son projecteur, après la finalisation de l’ancienne souscription, installer le nouvel état UI.
4. Null : loading=false, conserver le résultat, le marquer précédent si présent, aucun HTTP.
5. Requête valide : erreur effacée, loading=true (inclut les 300 ms d’attente), résultat précédent conservé/stale. Puis timer(300) avant HTTP ; pour retry, `of(0)` lance immédiatement sans timer.
6. Souscription HTTP via switchMap interne vers le service existant. Au succès, remplacer le résultat et remettre stale=false. À l’erreur, message sûr + résultat conservé/stale, puis EMPTY.
7. catchError **à l’intérieur** du flux de l’intention : une erreur n’arrête pas l’écoute des éditions suivantes. finalize remet loading=false ; l’ordre externe évite qu’une finalisation annulée éteigne le spinner d’une nouvelle requête.
8. takeUntilDestroyed après l’orchestration annule timer et HTTP quand la page disparaît.

Ne pas utiliser seulement `debounceTime(300) → switchMap(HTTP)` : l’ancien HTTP resterait actif pendant les 300 ms de la nouvelle édition. Ne pas s’appuyer uniquement sur un effect/toObservable différé pour invalider : les mutateurs existants permettent une notification synchrone.

L’annulation de souscription HttpClient empêche les émissions obsolètes d’atteindre les Signals ; elle ne prouve pas l’arrêt immédiat d’une requête SQL serveur. [Documentation Angular HTTP](https://angular.dev/guide/http/making-requests). Aucun identifiant de version HTTP public n’est ajouté.

### Chargement, génération, retry et déduplication

- Souscrire au flux avant loadConfiguration dans le constructeur.
- Au début d’un reload, annuler la preview en cours. Conserver les comportements de reset distincts existants. Après résultat valide et fin de loading, programmer une seule preview si un draft complet a été restauré ; sans sélection, zéro appel. Les émissions de reset de FilterEditor sont fusionnées par le debounce/déduplication.
- Une erreur de chargement initial bloque preview. Un retry de configuration reste distinct du retry de preview.
- Au début d’une génération, suspendre/annuler le timer ou HTTP de preview, en conservant le résultat. Ne pas modifier canGenerate ni le payload/navigation. Si la génération échoue et que la page reste active, reprendre l’auto-preview de la définition courante valide. Après navigation réussie, aucune relance inutile.
- Générer reste possible pendant un chargement/échec de preview si la définition est valide. Les Signals UI responsive et le résultat précédent ne participent jamais au draft.
- Réessayer relance immédiatement la dernière définition valide courante après erreur, même si identique à la précédente tentative. Zéro double départ si déjà loading.
- Sans colonne ou avec un brouillon invalide, **l’interdiction d’appel prime sur retry**. Conserver une erreur utile du dernier échec, mais désactiver retry et expliquer « Terminez ou corrigez la configuration ». Ne pas réexécuter silencieusement un ancien filtre valide pendant une saisie invalide. Cette priorité est soumise à approbation.
- Déduplication des doublons consécutifs seulement, pas de cache permanent. A → B → A peut relancer A ; une erreur ne bloque jamais un retry explicite. Les variations de visibilité et de largeur ne sont pas des intentions HTTP.

### États et messages

Priorité de l’en-tête : configuration incomplète ; puis mise à jour ; puis erreur ; puis résultat précédent ; sinon à jour si résultat. Le seul cas valide sans résultat et sans chargement, lors de la suspension pour génération, affiche « En attente » avec « Génération en cours » ; ne pas le présenter comme un succès ou une configuration invalide. Si la configuration devient incomplète avec un ancien résultat, le libellé principal est « Configuration incomplète » et une indication secondaire dit « Aperçu précédent ». Durant loading avec résultat : « Mise à jour… » et mention que les lignes sont celles du dernier succès.

| # | Situation | Affichage et comportement cible |
|---|---|---|
| 1 | Chargement initial de configuration | Spinner de page ; aucun appel preview avant résolution |
| 2 | Erreur de chargement initial | Erreur inline de page + retry de configuration ; aucun auto-preview |
| 3 | Aucune colonne | Configuration incomplète ; choisir une colonne ; ancien résultat conservé/précédent |
| 4 | Filtre incomplet/invalide | Configuration incomplète ; conserver FormControls, derniers filtres valides et résultat ; annuler tout appel |
| 5 | Premier appel preview | Mise à jour… ; spinner local, sans bloquer les éditeurs |
| 6 | Réponse réussie | À jour, compteur retourné, tableau API |
| 7 | Modification valide en attente/HTTP | Mise à jour… dès l’édition, 300 ms puis requête |
| 8 | Anciennes lignes pendant refresh | Tableau maintenu ; mention explicite aperçu précédent |
| 9 | Échec avec résultat précédent | Erreur + message inline sûr + retry ; ancienne table et compteur identifiés |
| 10 | Échec sans résultat | Erreur + message inline sûr + retry, aucune table inventée |
| 11 | Réponse vide | À jour, 0 ligne, Aucune donnée ; une réponse vide reste un succès |
| 12 | Panneau réduit | En-tête/état/compteur/erreur/retry visibles ; corps caché, requêtes maintenues |
| 13 | Panneau développé | Même instance/table ; scroll interne sans chevauchement |
| 14 | Mobile Configuration | Éditeurs visibles, preview masquée mais actualisée ; statut dans l’onglet Aperçu |
| 15 | Mobile Aperçu | Même panneau/table, aucune sélection/saisie réinitialisée ; actions communes accessibles |
| 16 | Génération en cours/erreur | Démarrage…/disabled selon règles existantes ; erreur inline ; draft/navigation inchangés |

L’état Aperçu précédent est aussi applicable à une interruption volontaire de preview pendant le démarrage de génération. Une modification ne doit jamais laisser un résultat ancien marqué À jour.

Messages preview proposés, définis localement dans `previewErrorMessage` :

| Cas | Message |
|---|---|
| Réseau (0) | « Le serveur est momentanément inaccessible. Vérifiez votre connexion puis réessayez. » |
| 401 | « Votre session a expiré. Reconnectez-vous avant de demander un aperçu. » |
| 403 | « Vous n’avez pas accès à cet aperçu. » |
| 400 | « La configuration du rapport n’a pas pu être validée. Vérifiez les colonnes, filtres et tris. » |
| 409 | « Certaines données de la configuration ne sont plus disponibles. Rechargez la configuration. » |
| 504 | « L’aperçu a pris trop de temps. Réessayez ou précisez les filtres. » |
| Autre | « Impossible de charger l’aperçu du rapport. Réessayez dans quelques instants. » |

Ne pas afficher ni logger ici un detail technique brut, une réponse SQL ou les valeurs du rapport. Le détail backend actuellement accepté sans distinction n’est pas un contrat UI à préserver. Cette réduction d’information de validation doit être approuvée ; ne pas étendre ce changement au mapper d’erreur de génération dans cette tâche.

## Affected files and symbols

Liste fermée attendue pour l’implémentation :

| Fichier | Symboles/changements |
|---|---|
| `C/configuration.component.ts` | imports du panneau, orchestration/schedulePreview/retryPreview, computed de statut, Signals responsive/réduction, listener matchMedia ; préserver createPreviewRequest/canGenerate/restoreDraft/continueToExport |
| `C/configuration.component.html` | supprimer bouton Aperçu et montage dialog ; une occurrence du panneau, régions stables, actions communes et onglets |
| `C/configuration.component.scss` | grille dockée, réduction, mode mobile, min sizes, focus et styles locaux utilisant les tokens |
| `C/configuration.component.spec.ts` | remplacer test manuel, étendre tests de temporalité, état, responsive et génération |
| `C/components/preview-dialog/preview-dialog.component.ts` → `C/components/preview-panel/preview-panel.component.ts` | renommage, suppression API dialog, données de présentation et slot d’action ; formatValue/tableRows préservés |
| `C/components/preview-dialog/preview-dialog.component.html` → `C/components/preview-panel/preview-panel.component.html` | en-tête persistant, table unique, messages, retry et corps masquable |
| `C/components/preview-dialog/preview-dialog.component.scss` → `C/components/preview-panel/preview-panel.component.scss` | host block/flex, dimensions et scroll appropriés |
| `C/components/preview-dialog/preview-dialog.component.spec.ts` → `C/components/preview-panel/preview-panel.component.spec.ts` | conserver tests existants, couvrir états manquants, ordre strict, absence dialog |
| `RHIS/docs/flows/02-report-definition-fields-filters-sorts.md` | décrire invalidation/auto-preview sans changer les invariants métier |
| `RHIS/docs/flows/03-report-preview.md` | nouveau déclenchement, panneau, erreurs, annulation ; corriger la description de sécurité en conformité avec le code existant |

`ReportPreviewService`, son test, modèles publics, éditeurs, loader, services génération/draft, routes, app.config, styles globaux, package.json et backend **ne nécessitent pas de modification**. Exécuter leurs tests pertinents sans les inscrire comme fichiers affectés. Si un besoin nouveau est démontré, mettre à jour ce tableau et demander revue avant élargissement.

Les deux documents de cette phase restent vivants. Les fichiers Graphify générés ne sont pas des sources à modifier manuellement.

## Milestone 1: panneau de présentation non modal

Result : le même aperçu peut être consulté inline, avec son tableau et ses états, sans dialog. Le déclenchement reste provisoirement manuel jusqu’au milestone 2 ; ce jalon n’est pas la livraison finale.

Work :

- [x] Renommer les quatre fichiers du présentateur et actualiser l’unique import/montage dans ConfigurationComponent.
- [x] Retirer DialogModule, visible/visibleChange/close/Fermer. Conserver l’output retry et les inputs de contenu.
- [x] Monter le panneau une seule fois dans le flux, sous la configuration/actions. Adapter le clic manuel provisoire pour demander un résultat inline, sans état previewVisible.
- [x] Ajouter header, compteur, statut, messages sûrs et slot d’action ; conserver tableRows/formatValue/ordre API.
- [x] Adapter les tests de preview pour loading initial, error sans résultat + retry, stale, null/undefined/booleans/texte, ordre exact et compteur zéro.
- [x] Vérifier l’absence de p-dialog, masque, Fermer et visibleChange dans les sources de production de cette page.

Validation depuis F :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts"
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts"
```

Expected observation : le clic provisoire affiche un résultat dans la page sans overlay ; retry et erreur ne détruisent pas les anciennes lignes. Tests du tableau et des règles existantes réussis. Mettre à jour Progress et la prochaine action, puis revue du diff.

## Milestone 2: actualisation automatique et layout docké desktop/tablette

Result : à partir de 48rem, configuration/actions/preview sont séparées dans le layout, la preview s’actualise après 300 ms et aucune réponse obsolète ne peut s’installer. Sur mobile, le contenu reste provisoirement empilé jusqu’au milestone 3.

Work :

- [x] Remplacer le test `never calls preview automatically...` par un test fakeAsync vérifiant zéro appel à 299 ms puis un appel à 300 ms.
- [x] Brancher schedulePreview sur les trois mutateurs, les resets/restaurations/reloads et la suspension/reprise autour de génération. Préserver le payload et les méthodes métier.
- [x] Introduire l’unique Subject/pipeline défini plus haut, sa déduplication et ses garde-fous ; capturer les définitions avant envoi et annuler immédiatement à toute nouvelle intention.
- [x] Supprimer le bouton Aperçu et les méthodes/états manuels devenus sans appelant ; ne pas garder une compatibilité privée inutilisée.
- [x] Ajouter les tests d’annulation pendant le debounce, invalidation en vol, reprise après erreur et valide A → invalide → valide A.
- [x] Implémenter la grille dockée, le Signal de réduction, la projection du bouton et le statut/compteur visible réduit. Ne pas modifier ColumnSelector.
- [x] Afficher les messages preview locaux sûrs et conserver les lignes après une erreur.
- [x] Tester génération pendant l’attente d’une preview et absence d’utilisation du résultat affiché comme définition.

Validation depuis F :

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts"
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts"
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/services/report-preview.service.spec.ts"
```

Expected observation : modifier une colonne/ordre/filtre/tri/priorité actualise sans clic ; masquer le corps ne change pas les appels ; les actions ne sont pas couvertes ; un ancien HTTP ne remplace pas la dernière configuration. Passer les tests et contrôler manuellement au moins tablette portrait et desktop avant le jalon mobile.

## Milestone 3: onglets mobiles, persistance responsive et qualification finale

Result : l’UX complète fonctionne sur mobile/tablette/desktop, les états restent identiques au changement de présentation et les preuves de régression sont consignées.

Work :

- [x] Ajouter le seul listener matchMedia, isMobile et activeMobileTab ; nettoyer le listener à la destruction.
- [x] Garder les sections/éditeurs/panneau stables et masquer uniquement le contenu mobile inactif. Rendre les actions communes accessibles depuis les deux onglets.
- [x] Implémenter le pattern clavier/ARIA/focus défini plus haut et le statut de l’onglet Aperçu. Aucun appel HTTP dans les handlers de tabs/breakpoint/réduction.
- [x] Vérifier que l’onglet Aperçu ignore visuellement la réduction desktop, sans la réinitialiser.
- [x] Tester avec un vrai FilterEditor enfant un brouillon incomplet avant/après onglets et changement de breakpoint, pas seulement les Signals du parent.
- [~] Exécuter la matrice manuelle complète : géométrie et onglets vérifiés sur six largeurs ; données, overlays, génération et zoom 200 % non exerçables sans backend/session de test.
- [x] Actualiser seulement les flows 02/03 affectés ; garder la spec historique comme historique.
- [x] Exécuter suite entière/build et comparer aux échecs/warnings mesurés : aucune nouvelle régression.
- [x] `graphify update .` exécuté avec succès le 2026-08-30 ; artefacts générés restaurés ensuite, car aucune politique du dépôt n'impose de les versionner.
- [x] Relire le diff final, confirmer les fichiers inchangés et mettre à jour les registres. Aucun commit/push effectué.

Validation : toutes les commandes et tous les critères de la section suivante. Le succès exige le comportement observable, pas seulement la compilation. Un échec préexistant restant doit être rapporté et ne devient pas un test « vert ».

## Validation and acceptance

### Commandes reproductibles

Toutes les commandes npm partent de `C:/Users/Surface Pro/Downloads/RHIS/Frontend/Rhis_report_gen`. Les fichiers preview-panel n’existent qu’après milestone 1.

```powershell
# Configuration
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/configuration.component.spec.ts"

# Présentateur
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/components/preview-panel/preview-panel.component.spec.ts"

# Régression locale : tous les éditeurs/loader et contrat HTTP inchangé
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/rapports/pages/configuration/**/*.spec.ts" --include="src/app/features/rapports/services/report-preview.service.spec.ts"

# Commande complète demandée, ou variante headless pour cet environnement
npm.cmd test -- --watch=false
npm.cmd test -- --watch=false --browsers=ChromeHeadless

# Production et budgets
npm.cmd run build
```

Il suffit de l’une des deux variantes de la suite complète pour une exécution donnée ; documenter laquelle. Ne pas installer un runner supplémentaire. Jasmine/Karma, fakeAsync/tick, Subjects et HttpTestingController existent déjà.

Depuis la racine effective :

```powershell
git status --short
git diff --check
git diff --stat
rg -n 'p-dialog|app-preview-dialog|openPreview|previewVisible|visibleChange' Frontend/Rhis_report_gen/src/app/features/rapports/pages/configuration
graphify update .
```

Le rg doit être sans occurrence dans les sources finales ; ses éventuelles assertions négatives dans les tests ne sont pas des régressions. Vérifier également l’unique occurrence de app-preview-panel et de p-table dans les templates pertinents.

### Baseline réelle avant toute modification de sources

Recherche effectuée sur HEAD 394ad29, arbre suivi initialement propre.

| Commande exécutée | Résultat observé |
|---|---|
| Tests ciblés configuration/**/*.spec.ts + report-preview.service.spec.ts, ChromeHeadless | 54 SUCCESS, exit 0 |
| Suite complète ChromeHeadless, deux exécutions | 130 SUCCESS, 6 FAILED sur 136, exit 1 ; aucun skip signalé |
| npm.cmd run build | exit 0 ; deux warnings existants |
| graphify query ciblé | exit 0, navigation utile mais résultat tronqué ; conclusions vérifiées dans les sources |
| git status/diff avant rédaction | aucun changement de source |

Les six échecs de suite complète, reproduits **sans modifier les sources** :

1. `SharedPageLayoutComponent — renders one header and role-aware navigation on reports` : expected undefined to contain Rapports (spec ligne 30).
2. `ExportComponent — keeps the Option B action disabled until the selected file is ready` : expected null not to be null.
3. `DatasetExposureComponent — shows dirty markers and distinct search empty states` : textes de recherche attendus différents.
4. `DatasetExposureComponent — renders the selected table, its field states and associated labels` : expected undefined to contain Employés.
5. `DatasetExposureComponent — moves focus into and out of the mobile detail view` : référence de focus attendue null, focus reçu sur body.
6. `DatasetExposureComponent — restores focus after an asynchronous save removes the action buttons` : même famille d’attente de focus.

Des warnings 404 sur primeicons.woff2/woff/ttf apparaissent sous Karma. Leur cause n’est pas diagnostiquée ici.

Budgets mesurés : initial **604,67 kB** (warning 500 kB, erreur 1 MB), administration/dataset-exposure.component.scss **7,97 kB** (warning 4 kB, erreur 8 kB). Chunk lazy configuration **479,62 kB**. Ne pas augmenter les seuils pour masquer un dépassement ; relever la variation et tout nouveau warning de composant.

La suite complète n’est donc pas verte. Les six cas hors périmètre ne doivent être ni supprimés ni adaptés pour faire passer cette tâche. Si un échec apparaît dans le périmètre modifié, l’isoler et traiter la régression avant clôture. Pas de test backend exécuté : aucune modification backend prévue.

### Tests à adapter ou ajouter

Étendre les fichiers de tests existants et leurs fixtures. Pas de framework, suite end-to-end ou fichier de test supplémentaire sans lacune concrète.

| Cas | Mise en situation et assertion observable | Emplacement |
|---|---|---|
| Absence de commande manuelle | Aucun bouton d’action dont le libellé exact est Aperçu ; l’onglet mobile Aperçu reste autorisé ; aucun p-dialog/masque | Configuration + panneau |
| Sélection valide | Sélectionner un champ, tick(299) : zéro appel ; tick(1) : un appel payload exact | Configuration |
| Modifications rapides | A, tick(100), B, tick(100), C ; avant 300 ms depuis C aucun nouveau départ, après exactement C | Configuration |
| Colonnes/ordre | Ajout, retrait, permutation : nouvelle requête ordonnée ; purge des filtres/tris conservée | Configuration, tests éditeur inchangés |
| Filtres | Modification d’une valeur valide et suppression de filtre relancent ; FormControl incomplet bloque | Configuration avec enfant réel |
| Tris/priorités | Modification direction/champ, ajout/retrait et permutation envoient l’ordre final | Configuration, tests éditeur inchangés |
| Invalidité avant deadline | Programmer A puis passer à aucune colonne/filtre invalide avant 300 ms : zéro HTTP | Configuration |
| Annulation immédiate en vol | Démarrer A puis saisir B : teardown de A avant les 300 ms de B ; emission A durant cette attente ignorée | Configuration |
| Réponse tardive | B réussit puis A émet : B reste affiché, stale/loading/error non corrompus ; idem pour erreur tardive A | Configuration |
| Annulation vers invalide | A en vol → filtre invalide/aucune colonne : A annulée, résultat précédent conservé et identifié | Configuration |
| Déduplication | Même contenu dans de nouveaux arrays : pas de nouvel appel ni faux stale ; A → invalide → A autorise un départ ; pas de cache A/B/A imposé | Configuration |
| Ancien succès | Pendant attente/HTTP/erreur, même previewResult et lignes ; succès courant le remplace | Configuration + panneau |
| Retry | Après erreur, clic relance immédiatement sans tick ; même payload accepté ; second clic pendant loading ignoré | Configuration + panneau |
| Retry invalide | Après erreur, invalider filtre ou retirer tout : aucun départ, explication et retry désactivé | Configuration |
| Flux après erreur | Erreur A puis modification B valide : B part et réussit ; souscription toujours active | Configuration |
| Chargement/reload | Zéro preview avant résolution ; un draft restauré valide déclenche une seule requête ; reload/destroy annulent timer et HTTP | Configuration |
| Génération inchangée | Définition courante ≠ ancien tableau : startReportGeneration et draft reçoivent la courante, UUID, même route ; pas de double submit | Configuration |
| Génération sans preview réussie | Générer fonctionne si définition valide pendant loading/erreur preview ; invalide et isGenerating bloquent comme avant | Configuration |
| Erreur de génération | Reste inline et réactive Générer ; preview reprise si la page reste ouverte | Configuration |
| Panneau réduit/développé | aria-expanded change, corps caché sans perte d’instance, statut/compteur/retry toujours visibles ; édition réduit actualise | Configuration + panneau |
| Mobile initial | Configuration actif, contenu Aperçu hidden, tableau/éditeur instanciés une fois | Configuration |
| Tab switch | Aucun reset, aucune navigation de route, aucun save draft et aucun HTTP supplémentaire | Configuration |
| Brouillon incomplet | Saisir une ligne invalide avec touched ; onglet puis resize puis retour : mêmes contrôles, valeurs, état touched/invalid | Configuration |
| Responsive en vol | Passer mobile↔tablette/desktop pendant attente/HTTP : même souscription, même résultat et même sélection | Configuration |
| Clavier | Flèches/Home/End, Entrée/Espace, Tab et Shift+Tab : focus/selection/aria-controls cohérents, pas de piège | Configuration + manuel |
| Statut onglet | Loading/error/précédent visibles sur Aperçu depuis Configuration ; édition ne change pas activeMobileTab | Configuration |
| Présentation | Loading initial, empty, error seul, error+précédent, stale seul, ordre strict headers/cellules, valeurs null/bool/texte et compteur | Panneau |
| Message technique | Réponse 500 avec detail contenant SQL/stack fictif : texte absent, message local présent ; pas de HTML dynamique | Configuration |
| Transport | Service existant conserve URL/POST/credentials/body et réponse | Test service inchangé |

Exemple précis du test remplaçant l’ancien comportement, à intégrer aux fixtures de Configuration (ajouter fakeAsync/tick aux imports) :

```typescript
it('requests preview after a valid selection settles', fakeAsync(() => {
  createComponent();
  component.updateSelectedFields([component.fieldGroups()[0].fields[0]]);
  fixture.detectChanges();
  tick(299);
  expect(reportPreviewService.preview).not.toHaveBeenCalled();
  tick(1);
  expect(reportPreviewService.preview).toHaveBeenCalledOnceWith({
    rootDatasetId: 1,
    selectedFieldIds: [11],
    filters: [],
    sorts: [],
  });
}));
```

Pour l’annulation, remplacer le retour du spy par un Observable autour d’un Subject et un spy de teardown. Après A en vol, appeler updateSorts pour B, puis vérifier teardown **avant tick(300)**. Faire émettre le Subject A pendant le debounce et après le succès B ; ni ses valeurs ni son erreur ne doivent altérer le dernier résultat. Réutiliser cette mécanique pour null et fixture.destroy. Ce test distingue une vraie invalidation immédiate d’un simple debounce suivi de switchMap.

Les tests responsive unitaires utilisent un stub typé de matchMedia avec changement de matches déclenché et vérification du retrait du listener. Ne pas rendre ces tests conditionnels à la largeur de Karma avec pending : cela masquerait la couverture. Les contrôles de géométrie réels restent dans la matrice manuelle.

### Matrice responsive manuelle obligatoire

Préconditions : compte de test autorisé, source et champs sans données sensibles dans les preuves. Utiliser des valeurs de test et le panneau Network. Ne pas enregistrer de rapports réels dans le dépôt.

Pour **chaque ligne**, exécuter tous les contrôles V1–V8 ci-dessous, en état développé et réduit lorsque ce mode existe.

| Profil | Viewport de référence en CSS px | Mode attendu | Points particuliers |
|---|---|---|---|
| Mobile étroit | 320 × 740 | Onglets | Actions empilables, libellés longs, aucune largeur de page excédentaire |
| Mobile large | 430 × 932 | Onglets | Saisie invalide et alternance des deux onglets |
| Tablette portrait | 768 × 1024 | Panneau docké | Frontière md exacte, ColumnSelector à deux moitiés |
| Tablette paysage | 1024 × 768 | Panneau docké | Frontière lg, filtre/tri dans la colonne latérale |
| Desktop | 1440 × 900 | Panneau docké | Configuration et table simultanées, action bar séparée |
| Desktop large | 1920 × 1080 | Panneau docké | max-width 1440 conservé, pas d’étirement inutile |
| Zoom navigateur 200 % | Fenêtres 1440 × 900 et 1920 × 1080 à 200 % | Selon largeur CSS réellement mesurée | La première peut passer mobile ; la seconde reste assez large pour tester le dock en faible hauteur |
| Frontière responsive | 767 puis 768 puis 769 de large | Transition attendue | Requête en vol, focus dans une section qui devient cachée |

Contrôles à consigner par ligne :

- **V1 — Contrôles :** tous les champs, filtres, tris, labels et erreurs restent visibles ou accessibles par scroll de leur région ; aucun contenu masqué par un overflow incorrect.
- **V2 — Géométrie :** pas de chevauchement entre éditeurs, actions, header et preview ; aucun scroll horizontal de la page. Seule la table peut défiler horizontalement.
- **V3 — Clavier :** parcourir toutes les actions, naviguer les onglets au clavier, entrer/sortir du scroll de table et des Select/DatePicker, Escape pour fermer les overlays.
- **V4 — Focus :** indicateur visible ; aucun focus dans une région hidden ; pas de saut après actualisation ; repli/dépli et changement de breakpoint rendent le focus atteignable.
- **V5 — Tableau :** ordre API, valeurs/null/bool, longues cellules, scroll horizontal/vertical, 0 et 6 lignes ; la table précédente reste visible pendant une réponse lente et après erreur.
- **V6 — Actions :** Générer et Précédent accessibles et non recouverts ; vérifier état disabled, Démarrage… et erreur inline.
- **V7 — Persistance :** conserver colonnes/ordre, filtre valide puis incomplet/touched, tris et dernier résultat en changeant d’onglet. Sur tablette/desktop, traverser aussi le breakpoint pour effectuer ce contrôle.
- **V8 — Réseau :** un seul POST après la dernière modification valide, aucun sur tab/resize/réduction, annulation d’un appel obsolète, zéro appel sans colonne/invalide, retry immédiat après erreur.

Scénarios transverses : ralentir un appel, modifier deux fois ; couper le réseau après un succès puis retry ; démarrer avec un draft restauré ; charger une configuration introuvable ; vérifier les états 1–16. Consigner viewport, zoom, navigateur, résultat et anomalie ; un cas non exercé reste « non vérifié ». La génération réelle doit utiliser seulement le compte/dataset de test autorisé.

#### Exécution manuelle du 2026-08-29

Le serveur Angular local a été contrôlé dans le navigateur intégré. Le backend/session de test n'était pas disponible : la page affichait l'erreur de chargement initiale. Les contrôles ci-dessous portent donc sur le shell, les régions, les onglets et le panneau dans cet état ; V5–V8 avec données restent non vérifiés manuellement et sont couverts seulement par les tests automatisés.

| Viewport | Résultat vérifié | Limite |
|---|---|---|
| 320×740 | onglets visibles, Configuration initiale, bascule Aperçu, flèche gauche, focus et ARIA cohérents, aucun overflow horizontal | éditeurs/actions absents dans l'état d'erreur |
| 430×932 | même comportement mobile, contenu inactif masqué sans seconde instance | données/table non vérifiées |
| 768×1024 | seuil dock exact, configuration et preview dans deux lignes sans chevauchement | état d'erreur seulement |
| 1024×768 | panneau docké, régions séparées et contenues dans le viewport | overlays/table non vérifiés |
| 1440×900 | panneau docké, aucune largeur de page excédentaire | génération non vérifiée |
| 1920×1080 | max-width 1440 respecté, panneau/configuration alignés | données non vérifiées |
| Panneau réduit à 768×1024 | corps masqué, header/statut visible, configuration agrandie, aria-expanded=false | actualisation réseau couverte par test unitaire |
| Zoom 200 % | non vérifié | contrôle exact du zoom indisponible dans cette session |

Une première passe a trouvé un placement incorrect du panneau lorsque la barre d'actions conditionnelle était absente. L'affectation explicite des lignes de grille a été ajoutée, puis les six viewports ont été rejoués avec succès.

### Critères de clôture observables

- [x] Aucun bouton manuel Aperçu, aucun dialog/masque, un seul présentateur et un seul tableau.
- [x] L’édition valide actualise automatiquement ; aucune réponse antérieure ne peut prendre la place d’une définition plus récente.
- [x] Aucune colonne/filtre invalide ne déclenche de requête, même si une ancienne demande attendait ou si retry est activé.
- [x] L’utilisateur distingue à jour/mise à jour/précédent/incomplet/erreur, y compris panneau réduit et onglet inactif.
- [~] Les régions testées ne se recouvrent pas et la table scrolle localement ; les contrôles/table avec données restent à qualifier manuellement.
- [x] Aucun changement d’onglet ou de largeur ne perd le brouillon incomplet, la définition ou les anciennes lignes.
- [x] Génération et contrat HTTP inchangés ; aucune écriture de données métier ajoutée.
- [x] Tests ciblés réussis, suite complète comparée explicitement à la baseline, build et warnings rapportés.
- [x] Matrice manuelle partielle effectuée et lacunes explicitement présentées ; diff relu, aucun changement sans rapport.
- [x] Approval obtenue avant le premier changement de source et Progress à jour après chaque jalon.

## Risks and rollback

| Risk | Prevention/Detection | Rollback |
|---|---|---|
| Plus de requêtes de lecture SQL | 300 ms, déduplication, invalidation immédiate ; pas de polling ; mesure Network | Revenir au déclenchement manuel avec restauration coordonnée du présentateur/parent |
| Ancien résultat marqué à jour | Tests A/B, A/invalide, late success/error et finalize | Retirer uniquement le jalon d’orchestration défaillant après revue |
| Brouillon perdu au responsive | Instances stables ; tests FormControl et identité, pas seulement Signals | Revenir au layout précédent sans effacer sessionStorage |
| Rôles/focus tabs incorrects | Pattern natif limité à deux onglets, clavier et lecteur d’écran | Réviser le jalon mobile ; pas de widget global de remplacement improvisé |
| Régions trop petites / overlays body | Matrice tablette/200 %, réduction et scroll ; vérifier focus et picker | Revenir au layout antérieur du seul composant |
| Messages locaux moins détaillés | Arbitrage explicite ; messages par statut ; conserver retry inline | Restaurer seulement un détail métier reconnu après revue, jamais accepter un détail arbitraire |
| Régressions noyées dans baseline rouge | Conserver noms des six cas existants et commandes ; isoler tout nouvel échec | Bloquer la clôture de la régression, sans supprimer les tests |
| Budget proche de la limite admin | Ne pas toucher budgets ni styles admin ; comparer tailles | Revoir uniquement les ajouts de cette feature |

Pas de migration de données ni changement backend : rollback frontend uniquement. Avant implémentation, recontrôler status/diff et utiliser une branche `codex/configuration-live-preview` si elle n’existe pas déjà, ou un worktree dédié conforme à AGENTS, après approbation et permissions nécessaires. Ne pas faire de reset global. Sans commits autorisés, conserver les modifications par jalon et restaurer uniquement les hunks de cette tâche après revue ; avec commits autorisés ultérieurement, revenir sur les commits de feature de manière coordonnée.

Aucune donnée de preview, valeur de filtre réelle ou credential dans les logs/docs. Ne pas promettre qu’un unsubscribe navigateur arrête immédiatement PostgreSQL. La sécurité et le nettoyage des exports restent gérés par le backend existant.

## Progress

- [x] 2026-08-29 00:49 +02:00 — Consignes, flux, composants, modèles, tests, versions installées et API Tabs vérifiés.
- [x] 2026-08-29 00:50 +02:00 — Baseline ciblée : 54 succès ; build réussi avec deux warnings préexistants.
- [x] 2026-08-29 00:54 +02:00 — Suite entière reproduite : 130 succès et 6 échecs avant changement ; anomalies consignées.
- [x] 2026-08-29 — Recherche et ExecPlan rédigés et relus pour validation humaine.
- [x] 2026-08-29 — ExecPlan et choix D1–D5 approuvés explicitement.
- [x] 2026-08-29 — Branche codex/configuration-live-preview créée et activée.
- [x] 2026-08-29 — Milestone 1 implémenté ; 27 tests Configuration/PreviewPanel réussis.
- [x] 2026-08-29 22:40 +02:00 — Milestone 2 implémenté ; debounce, annulation, déduplication, stale/retry, génération et layout docké couverts.
- [x] 2026-08-29 22:48 +02:00 — Milestone 3 implémenté ; onglets natifs, matchMedia, focus et persistance du vrai FilterEditor couverts.
- [x] 2026-08-29 22:55 +02:00 — Régression locale : 70 succès. Build réussi avec les deux warnings préexistants. Suite complète : 146 succès/6 échecs, mêmes cas que la baseline.
- [~] 2026-08-29 22:56 +02:00 — Qualification navigateur partielle terminée.
- [x] 2026-08-29 22:57 +02:00 — Revue finale et Outcomes complétés. Aucun commit ni push.
- [x] 2026-08-30 00:15 +02:00 — Changements rattachés à `codex/configuration-live-preview` après arbitrage humain ; documents `rhis_bot` absents du checkout ; `graphify update .` réussi.
- [~] 2026-08-30 — Validation avec données tentée sur un PostgreSQL 15 éphémère isolé : connexion JDBC et initialisation Hibernate réussies, puis démarrage Tomcat bloqué par le runtime (`Unable to establish loopback connection`). Conteneur arrêté et supprimé ; aucune base existante utilisée.

**État de reprise :** implémentation non commitée sur `codex/configuration-live-preview`. Restent uniquement la matrice avec backend/session de test, le zoom 200 % et les overlays PrimeNG.

## Surprises & Discoveries

- 2026-08-29 — La source de vérité parent ne contient pas tout le brouillon : les lignes invalides et touched vivent dans FilterEditor. Une simple restauration des filtres valides ne suffit pas.
- 2026-08-29 — Le verrou previewLoading actuel empêche les doubles clics, pas les réponses d’une définition dépassée. Le nouveau flux doit annuler avant le debounce.
- 2026-08-29 — Les 4 tests du présentateur ne couvrent pas tous ses états ; l’assertion d’ordre des headers est moins forte que son titre.
- 2026-08-29 — PrimeNG Tabs 20.4.0 conserve le contenu non lazy mais force hidden sur le panneau inactif. Pas de mode multi-actif public constaté.
- 2026-08-29 — MessageService/p-toast sont déjà globaux ; ne pas ajouter de notification.
- 2026-08-29 — Les docs anciennes de Générer et permitAll contredisent le code courant. Préserver le code et corriger seulement la documentation pertinente lors de l’implémentation.
- 2026-08-29 — Les frontières Git observées sont celles d’un dépôt parent unique, pas celles annoncées par la documentation.
- 2026-08-29 — Baseline complète rouge et deux warnings build confirmés sans changement de sources ; ce n’est pas une régression de cette demande.
- 2026-08-29 — Le navigateur local sans backend/session a permis de vérifier la géométrie et les onglets, mais pas les éditeurs alimentés, la table ni la génération en conditions réelles.
- 2026-08-29 — La première vérification géométrique a révélé que l'absence conditionnelle de la barre d'actions décalait le panneau dans la ligne centrale. Des `grid-row` explicites ont corrigé la cause ; les six largeurs ont ensuite été rejouées sans chevauchement ni overflow horizontal.
- 2026-08-29 — Un autre travail a changé le checkout vers `rhis_bot` et y a créé un commit pendant cette session. Les changements non commités ont suivi le checkout ; aucune opération Git corrective n'a été tentée pour préserver ce travail concurrent.
- 2026-08-29 — `graphify update .` était disponible mais son autorisation d'exécution a été refusée à cause de la limite d'usage de l'environnement.
- 2026-08-30 — Après arbitrage humain, le working tree a été replacé sans conflit sur `codex/configuration-live-preview`. Les deux documents commités propres à `rhis_bot` sont absents. Graphify a ensuite réussi ; ses caches/artefacts ont été restaurés pour garder le diff limité à la feature.
- 2026-08-30 — La base existante `reportdb` n'a pas été démarrée car le profil local utilise `ddl-auto: create`. Une base PostgreSQL éphémère sur le port 55432 a évité tout risque sur les données. Le backend l'a jointe, mais le runtime a refusé la connexion loopback nécessaire à Tomcat ; la matrice avec données reste donc non vérifiée dans cette session.

## Decision Log

Les décisions D1–D5 ont été approuvées explicitement avant implémentation.

- 2026-08-29 — **D1 : 48rem pour tablette/desktop ; panneau développé au départ.**
  Raison : réutilise md de Tailwind/ColumnSelector ; hauteur partagée avec configuration et réduction disponible. Alternatives écartées : seuil indépendant, panneau fixe superposé, resize manuel.
- 2026-08-29 — **D2 : debounce 300 ms et invalidation immédiate.**
  Raison : feedback local immédiat, HTTP après saisie stabilisée ; switchMap externe coupe aussi l’attente. Alternatives écartées : appel à chaque frappe, debounce placé avant toute annulation, nouveau service d’état.
- 2026-08-29 — **D3 : deux onglets natifs accessibles, sections montées une seule fois.**
  Raison : le besoin d’affichage simultané desktop ne correspond pas au host hidden de TabPanel. Alternatives écartées : surcharge fragile PrimeNG ou branches responsive qui recréent les éditeurs. Si l’usage de PrimeNG Tabs est impératif, revoir ce choix avant milestone 3, avec une solution démontrant la persistance du brouillon et des rôles corrects.
- 2026-08-29 — **D4 : retry immédiat uniquement si la définition courante est exécutable.**
  Raison : l’interdiction d’appel pendant un filtre invalide est absolue. Alternative écartée : retry d’un ancien filtre exécutable pendant une saisie invalide. La preview est suspendue pendant le démarrage d’une génération et reprise en cas d’échec, en conservant l’interdiction existante de démarrer une nouvelle preview pendant la génération ; l’annulation de la requête déjà active est un choix nouveau proposé.
- 2026-08-29 — **D5 : messages de preview locaux par statut, sans detail arbitraire.**
  Raison : le frontend actuel accepte tout detail string sans identifier un message métier sûr. Alternative écartée : nouvelle infrastructure de toast ou restitution inconditionnelle des textes techniques.
- 2026-08-29 — **D6 : conserver architecture, contrats, éditeurs et génération.**
  Raison : les responsabilités nécessaires existent ; aucun défaut métier n’impose leur refonte. La définition courante et l’échantillon affiché restent distincts.
- 2026-08-29 — **D7 : documents à la racine demandée, date de fichier conservée.**
  Raison : instruction explicite de l’utilisateur ; la date réelle de recherche est indiquée pour ne pas antidater les preuves. Aucun fichier de production pendant cette phase.

Arbitrage indispensable : approbation du plan, particulièrement D3 et D5. D1/D2/D4 sont les valeurs/règles par défaut proposées, pas des contraintes déjà présentes dans l’application.

## Outcomes & Retrospective

- Delivered behavior : preview intégrée unique, automatique après 300 ms, annulation/déduplication/retry, maintien du dernier succès, panneau docké et réductible dès 48rem, onglets mobiles accessibles, génération inchangée.
- Commands run and results : 41 tests Configuration/PreviewPanel réussis ; régression locale 70/70 ; build réussi (604,29 kB initial et SCSS administration 7,97 kB, deux warnings préexistants) ; suite complète 146 succès/6 échecs, mêmes noms et assertions que les 130 succès/6 échecs de baseline.
- Manual verification : 320×740, 430×932, 768×1024, 1024×768, 1440×900 et 1920×1080 vérifiés sur l'état d'erreur initial disponible. Aucun overflow horizontal ni chevauchement ; bascule/ARIA/focus des onglets et réduction desktop vérifiés. Le défaut de ligne de grille détecté a été corrigé puis revérifié.
- Deviations from the approved plan : aucune dépendance ou couche ajoutée. La matrice complète avec données, overlays et génération n'a pas pu être exécutée : le backend atteint la base éphémère mais Tomcat est bloqué par la restriction loopback du runtime ; zoom 200 % non exercé. Le checkout externe vers `rhis_bot` a été corrigé après arbitrage humain.
- Remaining risks or unverified checks : comportement visuel des contrôles/table avec vraies données, overlays PrimeNG, lecteur d'écran, zoom 200 %, charge serveur réelle et arrêt SQL après unsubscribe. Les six échecs hors périmètre restent présents.
- Required follow-up : compléter la matrice avec un backend et un compte/dataset de test autorisés.
- Exact next action if incomplete : lancer l'application complète avec ce jeu de test, puis vérifier overlays, table, génération et zoom 200 % aux viewports consignés.
