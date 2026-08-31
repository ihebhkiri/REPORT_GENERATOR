# rhis_bot — Interface Angular de l'assistant de rapports

**Date :** 30 août 2026
**Statut :** Design approuvé en session
**Branche :** `rhis_bot`
**Task slug :** `rhis-bot-angular-interface`

## Objectif

Fournir une page Angular dédiée permettant à un utilisateur authentifié de décrire un
rapport en français, de répondre à une éventuelle clarification et, une fois la génération
créée, de rejoindre l'écran d'export existant.

Le résultat observable est une route `/assistant` accessible depuis la navigation RHIS.
Elle appelle `POST /api/v1/bot/reports`, affiche la réponse du backend et propose le bouton
« Suivre la génération » lorsque la réponse contient un `generationId`.

## Périmètre

### Inclus

- page lazy-loaded `/assistant` dans le layout RHIS existant ;
- lien « Assistant » dans la navigation principale ;
- conversation locale entre utilisateur et assistant ;
- choix explicite du format `PDF` ou `XLSX` ;
- gestion de `READY`, `NEEDS_CLARIFICATION`, `FAILED` et des erreurs HTTP ;
- conservation côté Angular du contexte nécessaire à une clarification ;
- navigation manuelle vers `/rapports/export/{generationId}` ;
- tests du service, du composant et du routing.

### Hors périmètre

- streaming de tokens ;
- historique serveur, `localStorage` ou restauration après rechargement ;
- conversations multiples ;
- markdown riche, pièces jointes, voix, avatars ou animations complexes ;
- modification de l'API backend ou du pipeline génération/export ;
- déduction du format depuis la phrase ; le body reste autoritatif.

## Contraintes du dépôt

- Angular 20 standalone, TypeScript 5.9, Signals, RxJS 7.8 et PrimeNG 20 existants ;
- aucune nouvelle dépendance ;
- utilisation de `environment.apiBaseUrl` et `withCredentials: true` ;
- préservation stricte des changements live-preview déjà présents dans le worktree ;
- spec, plan et progression canoniques conservés sous `RHIS/docs/superpowers` dans le
  monorepo parent.

## Approche retenue

Une feature dédiée `features/report-assistant` contient la responsabilité conversationnelle.
Elle ne réutilise pas `ReportGenerationService`, car celui-ci reste responsable du suivi,
de l'export et du téléchargement des jobs déjà créés.

```text
/assistant
  → SharedPageLayoutComponent
  → ReportAssistantComponent
      → BotReportService.createReport()
      → POST /api/v1/bot/reports
          ├─ NEEDS_CLARIFICATION → question dans la conversation
          ├─ READY → résumé + bouton vers l'écran export
          ├─ FAILED → erreurs métier dans la conversation
          └─ erreur HTTP → message d'erreur, saisie réutilisable
```

Alternatives rejetées :

- méthode bot ajoutée à `ReportGenerationService` : moins de fichiers, mais responsabilité
  différente et modèles conversationnels mélangés aux états de jobs ;
- drawer global : état transversal et gestion d'overlay sans besoin actuel ;
- assistant intégré à la page des sources : chevauchement avec le flow manuel et avec les
  changements live-preview en cours.

## Routes et layout

`app.routes.ts` ajoute `/assistant` avec `data: {page: 'assistant'}`. La route charge le
`SharedPageLayoutComponent`, puis `ReportAssistantComponent` comme enfant lazy-loaded.

Le layout représente explicitement les pages `reports`, `datasets` et `assistant` afin de
produire le titre, le breadcrumb, la description et `aria-current` sans duplication de
header. La navigation principale ajoute « Assistant » avec une icône PrimeIcons existante.

## Contrats TypeScript et service HTTP

Le frontend modélise le contrat backend vérifié :

```typescript
type BotReportStatus = 'READY' | 'NEEDS_CLARIFICATION' | 'FAILED';

interface BotReportRequest {
  readonly message: string;
  readonly format: 'PDF' | 'XLSX';
}

interface BotReportResponse {
  readonly status: BotReportStatus;
  readonly question: string | null;
  readonly generationId: string | null;
  readonly format: 'PDF' | 'XLSX' | null;
  readonly planSummary: string | null;
  readonly errors: readonly string[];
}
```

`BotReportService` expose une seule méthode. Elle crée un UUID via `crypto.randomUUID()`,
envoie le header `Idempotency-Key`, le body typé et `withCredentials: true` vers
`${environment.apiBaseUrl}/bot/reports`.

## État local et conversation

Le composant utilise des Signals locaux :

- liste readonly de messages avec ID stable, auteur `user|assistant` et texte ;
- texte saisi et format sélectionné ;
- état `isSubmitting` ;
- erreur HTTP courante ;
- contexte de clarification éventuel ;
- génération prête éventuelle (`generationId`, format, résumé).

Il n'existe ni store, ni facade, ni persistance. Un rechargement réinitialise la conversation.

### Envoi initial

1. Refuser un message vide et respecter la limite 2000 caractères.
2. Ajouter le message utilisateur à la conversation.
3. Désactiver l'envoi pendant la requête.
4. Envoyer le texte et le format sélectionné.
5. Mapper la réponse sans masquer les erreurs.

### Clarification

Pour `NEEDS_CLARIFICATION`, l'interface affiche la question et conserve :

- la demande initiale ;
- la question du backend ;
- le format sélectionné.

La réponse utilisateur suivante est envoyée sous forme d'un message textuel unique :

```text
Demande initiale : <demande>
Question de clarification : <question>
Réponse : <réponse utilisateur>
```

Ce mécanisme donne une expérience conversationnelle sans mémoire serveur. Si le backend
demande une nouvelle clarification, le contexte est remplacé par la dernière requête
complète et la nouvelle question. La limite de 2000 caractères est vérifiée avant envoi ;
si le texte combiné la dépasse, l'utilisateur doit reformuler une demande complète.

### Génération prête

Pour `READY`, le composant affiche le résumé et le format retournés. Il ne navigue pas
automatiquement. Le bouton « Suivre la génération » utilise le `generationId` pour ouvrir
`/rapports/export/{generationId}`, qui conserve le polling et l'export existants.

## Présentation

La page reste cohérente avec le design RHIS existant : surface centrale lisible, messages
visuellement différenciés, formulaire fixé en bas de la carte sur les grands écrans et flux
normal sur mobile. PrimeNG est réutilisé pour le bouton et le choix de format ; aucun widget
conversationnel externe n'est ajouté.

La page contient :

- un message d'accueil avec deux exemples de demandes ;
- une liste de messages utilisateur/assistant ;
- une zone de texte multilignes limitée à 2000 caractères ;
- un choix `Excel (XLSX)` / `PDF` ;
- un bouton « Envoyer » ;
- une confirmation `READY` et son bouton de navigation.

`Ctrl+Enter` soumet le formulaire ; `Enter` reste disponible pour les retours à la ligne.

Le design approuvé exclut explicitement `aria-live`. Les contrôles gardent néanmoins leurs
labels, noms accessibles, ordre de tabulation naturel, focus visible et états `disabled`.

## Erreurs et états

| Cas | Comportement UI |
| --- | --- |
| Requête en cours | Envoi désactivé, libellé de chargement, pas de double POST. |
| `NEEDS_CLARIFICATION` | Question assistant, contexte conservé pour la réponse suivante. |
| `READY` | Résumé, format et bouton « Suivre la génération ». |
| `FAILED` / `422` | Liste d'erreurs métier, possibilité d'envoyer une nouvelle demande. |
| `400` | Message de validation ; saisie conservée ou restaurée. |
| `401` | Message de session expirée avec lien vers `/login`. |
| `502` | Modèle indisponible ; bouton réutilisable pour réessayer. |
| Réseau / autre `5xx` | Erreur générique sans produire de faux message de succès. |

Une nouvelle demande après `READY` réinitialise seulement la génération prête et le contexte
de clarification ; les messages déjà visibles restent jusqu'au rechargement de la page.

## Tests

### Service

- POST vers `/api/v1/bot/reports` ;
- body `message + format` ;
- header UUID `Idempotency-Key` ;
- `withCredentials: true` ;
- réponse typée inchangée.

### Composant

- message vide non envoyé ;
- double envoi bloqué pendant le POST ;
- message utilisateur ajouté et `READY` rendu avec le lien correct ;
- clarification affichée et contexte concaténé à la réponse suivante ;
- dépassement de 2000 caractères combinés refusé ;
- `FAILED`, `400`, `401`, `502` et erreur réseau rendus distinctement ;
- format sélectionné transmis ;
- `Ctrl+Enter` soumet, `Enter` seul ne soumet pas.

### Intégration frontend

- route `/assistant` lazy et lien de navigation ;
- test ciblé service/composant/layout ;
- suite Angular complète ;
- build production ;
- vérification manuelle avec backend et clé Mistral uniquement après configuration sûre.

## Risques et limites

- La mémoire de clarification est locale et perdue au rechargement.
- Le texte concaténé consomme la limite backend de 2000 caractères.
- Le format n'est pas déduit de la phrase ; le sélecteur UI est autoritatif.
- Le backend n'a pas encore passé l'E2E Mistral réel ; l'interface peut être validée avec
  service mocké avant cette vérification externe.
- Le layout partagé possède déjà des tests avec une anomalie baseline de libellé ; les
  résultats devront être comparés sans corriger implicitement les tests hors périmètre.

## Critères d'acceptation

- `/assistant` est accessible depuis la navigation et fonctionne sur desktop/mobile.
- Une demande produit exactement un POST authentifié avec UUID d'idempotence.
- Les trois statuts backend sont rendus correctement.
- Une clarification peut recevoir une réponse sans reformulation complète.
- `READY` n'entraîne aucune navigation automatique et expose le bouton demandé.
- Le bouton ouvre l'écran export existant avec le bon `generationId`.
- Aucun secret, donnée métier retournée, historique persistant ou dépendance supplémentaire.
- Les changements live-preview préexistants restent intacts.
