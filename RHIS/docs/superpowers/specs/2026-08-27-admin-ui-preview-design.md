# RHIS — direction et maquette de l’administration

Date : 2026-08-27. Task slug : `admin-ui-preview`.
Statut : maquette explicitement validée le 28 août 2026 ; implémentation Angular livrée.
Résultats et limites : `../progress/2026-08-28-admin-ui-preview-validation.md`.

## Accord reçu

La réponse utilisateur « ok je valide » suit la proposition de conserver les quatre
modes dans un select horizontal, de réserver le header à l'administration et d'utiliser
une seconde page fictive. Elle valide la direction et la création de l'aperçu uniquement.
La validation explicite a ensuite été reçue : « Je valide cette maquette, passe à l’implémentation. »
Révision demandée ensuite : remplacer Utilisateurs par Rapports, partager le header
avec `/rapports`, réserver Données au rôle admin et conserver le skeleton.

## Design proposé

Un seul SharedHeader porte identité RHIS, navigation, contexte, titre et description
sur Données et Rapports. Cette extension à `/rapports` est expressément demandée par
l'utilisateur ; ne pas l'étendre à d'autres écrans sans accord. Rapports est un lien
stylé comme bouton vers `/rapports`. Données pointe vers `/administration/datasets`
et n'est rendu que pour `ROLE_ADMIN`, valeur vérifiée dans le guard existant.
La visibilité du lien ne remplace pas le guard ni l'autorisation backend.
L'intégration Angular devra conserver les informations et actions des pages et
privilégier un layout partagé si adapté à leurs routes.

Le prototype propose ADMIN et Utilisateur non-admin dans « Profil simulé » et utilise
ces deux chemins locaux. La vue Rapports sert uniquement à vérifier le header ; ses
sources fictives ne simulent pas le parcours métier de création d'un rapport.
Le skeleton de chargement des données est conservé.

Conserver le master-detail, les recherches indépendantes, les statuts et informations
du DTO actuel. Restaurer une hiérarchie claire : titre de page, table sélectionnée,
mode d'exposition, champs. Pas de noms SQL ou types absents du contrat.

Le libellé Mode d'exposition est à gauche et le select associé à droite sur une seule
ligne. Les quatre états NONE, MAIN_ONLY, RELATED_ONLY, MAIN_AND_RELATED sont conservés.
Les checkboxes restent réservées à l'exposition des champs.

La barre globale Annuler/Enregistrer n'est rendue que lorsqu'un delta existe, avec une
transition de retrait après résolution du delta. Le delta reste calculé par comparaison
au snapshot enregistré. Le brouillon et l'annulation sont globaux, la sélection de table
ne les réinitialise pas. Succès : nouvelle baseline issue de la réponse ; échec :
brouillon conservé, message fonctionnel et réessai. Aucun timer n'efface un delta.

Transitions CSS discrètes, aucune dépendance d'animation. En mode réduit, mouvement
et transitions désactivés. Sur petit écran, navigation liste/détail, focus transféré
vers le titre puis rendu à la table au retour. Retrait de la barre après une action :
restituer le focus à un élément utile si le contrôle actif disparaît.

## Artefact et validation

`opendesign/mockups/admin-ui-preview/` : React isolé, bibliothèques vendored autorisées,
logo/PrimeIcons existants, contrôles natifs stylés, données fictives et aucun backend.
URL locale : `http://127.0.0.1:4387` lorsque `node serve.cjs` est actif.

Le README du prototype, `screenshots/verification.json` (première version) et
`screenshots/header-verification.json` (révision du header) décrivent les contrôles,
résultats et limites. Pas de preuve d'intégration Angular à ce stade.

## Suite après validation explicite

Attendre « Je valide cette maquette, passe à l’implémentation. ».
Ensuite seulement : writing-plans, plan proportionné, intégration Angular et tests
des scénarios approuvés, build frontend et vérification navigateur. Préserver les
guards, les API, les permissions, les changements Git préexistants et le backend.
La topologie Git particulière du checkout doit rester intacte. Pas de commit/push.
