# RHIS shared header and dataset UI — Implementation Plan

**Goal:** Intégrer la maquette explicitement validée le 28 août 2026 dans Angular.

**Architecture:** Un `SharedPageLayoutComponent` dans `src/app/shared/page-layout`
contient le header, le titre de route et un router-outlet. Il est utilisé uniquement
par la source `/rapports` et `/administration/datasets`. Les pages gardent leur logique,
et le guard de brouillon reste sur la route du composant datasets, pas sur le layout.

**Tech stack:** Angular 20.3.26, TypeScript 5.9, PrimeNG 20.4, RxJS 7.8, CSS/SCSS.

## Global Constraints

- Accord reçu : « Je valide cette maquette, passe à l’implémentation. »
- Ponytail full : réutiliser les Signals/deltas, PrimeNG et styles existants.
- Aucun commit, push, changement backend/API, dépendance ou modification des permissions.
- Quatre modes conservés ; aucune perte du brouillon au changement de table.
- Configuration et Export restent hors périmètre du header partagé.
- Sauvegarder une copie des sources concernées avant modification pour distinguer
  nos changements des modifications préexistantes. Checkout déjà sur branche codex.
- Exécution locale séquentielle avec executing-plans, sans subagents.

## File Map

Chemins relatifs au frontend `../Frontend/Rhis_report_gen` :

| Fichier | Responsabilité |
| --- | --- |
| `src/app/shared/page-layout/shared-page-layout.component.{ts,html,scss,spec.ts}` | Nouveau layout réutilisé, navigation et rôle issu de AuthService.me |
| `src/app/app.routes.ts` | Layout parent de datasets ; guards inchangés sur les niveaux appropriés |
| `src/app/features/rapports/rapports.routes.ts` | Layout sur la seule route source vide |
| `src/app/features/rapports/pages/source_de_donnes/rapports.component.html` | Retirer uniquement header et titre désormais portés par le layout |
| `src/app/features/administration/dataset-exposure/dataset-exposure.component.{ts,html,scss,spec.ts}` | Détails, barre conditionnelle, erreur persistante, animations, accessibilité |
| `src/app/shared/page-layout/shared-page-layout.routes.spec.ts` | Vérifier navigation réelle, accès admin et guard de sortie avec layout |

## Progress

### 1. Header partagé

- [x] Ajouter tests pour les liens exacts, ROLE_ADMIN, non-admin, erreur /auth/me et
  unicité du header sur les deux routes. Attendre la réponse sans afficher Données.
- [x] Créer le layout avec `toSignal(auth.me().pipe(catchError(() => of(null))))`
  et `computed(() => user()?.roles.includes('ROLE_ADMIN') ?? false)` ; pas de cache
  global de rôle, aucune modification de AuthService/adminGuard.
- [x] Associer les métadonnées de route `page: 'datasets' | 'reports'` au layout,
  conserver Aide/Paramètres désactivés sur Rapports et toutes les actions du contenu.
- [x] Déplacer header/titres hors des deux pages ; préserver le parcours Rapports.
- [x] Tester avec `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/shared/page-layout/*.spec.ts"`.

### 2. Datasets fidèle à la maquette

- [x] Ajouter des assertions DOM : barre absente initialement, apparition après delta,
  disparition après retour initial/annulation/succès, erreur persistante et retry.
- [x] Restaurer les titres `tables-title`, `dataset-detail-title`, recherche des champs,
  compteurs et caption ; aligner label/select dans une ligne flex sans changement métier.
- [x] `@if (dirty())` rend la barre ; `animate.enter`/`animate.leave` + CSS 160–180 ms
  pilotent le rendu sans timer métier. Une erreur de PUT reste dans la barre et le toast.
- [x] Conserver un seul brouillon global ; snapshot remplacé seulement au succès.
  Réinitialisation/succès rendent le focus au détail ou à la liste si le bouton disparaît.
- [x] Animer le détail avec une vue suivie par dataset.id ; conserver sélection,
  recherches indépendantes, inactive/NONE et contrôles désactivés pendant PUT.
- [x] Reprendre tailles, palette et espacement de la maquette en tokens existants,
  conserver le skeleton PrimeNG et respecter prefers-reduced-motion.
- [x] Exécuter `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/administration/dataset-exposure/*.spec.ts"`.

### 3. Integrated Validation

- [x] Exécuter `npm.cmd test -- --watch=false --browsers=ChromeHeadless` puis
  `npm.cmd run build`, consigner erreurs, skips et budgets sans les masquer.
- [x] Démarrer le frontend local. Vérifier le vrai rendu Angular dans le navigateur
  intégré : deux pages, rôles, sélection, mode, delta/revert/reset/succès/échec,
  navigation avec brouillon, skeleton, focus, bureau et mobile.
- [x] Si backend/auth indisponible, demander une connexion pour le serveur réel ;
  un éventuel serveur de fixtures local isolé ne constitue pas une validation backend.
- [x] Revoir le diff par rapport à la copie préimplémentation, puis mettre à jour
  spec/progress avec les résultats exacts et les limites.

## Surprises & Discoveries

- Les deux dossiers Git résolvent le parent commun ; topologie inchangée.
- Le template datasets actuel a des références à des titres/recherches manquants,
  déjà attendus par ses tests. Le test de référence est exécuté avant correction.
- Angular 20.3 permet animate.enter/leave ; documentation officielle vérifiée :
  https://angular.dev/guide/animations. Aucun package d'animation ajouté.

## Decision Log

- Layout partagé instancié pour les deux routes plutôt qu'un header dans App global :
  empêche son extension involontaire à login/configuration/export.
- Réutiliser le endpoint /auth/me pour la visibilité. Un GET de présentation peut
  s'ajouter au GET du guard admin ; éviter un cache d'auth global hors périmètre.

## Rollback

Retirer uniquement les ajouts de cette tâche et restaurer les portions concernées
depuis la copie préimplémentation, après comparaison. Ne jamais reset les fichiers
vers HEAD : ils contiennent des changements utilisateur. Aucun état serveur à migrer.

## Outcomes & Retrospective

Implémentation livrée. 32 tests ciblés réussis ; suite complète 135 succès et 1 échec Export indépendant. Build réussi avec avertissements, CSS 7.96 kB. 22 contrôles du build Angular avec fixtures dans le navigateur. Backend réel non validé. Rapport exact : ../progress/2026-08-28-admin-ui-preview-validation.md. Aucun commit/push.
