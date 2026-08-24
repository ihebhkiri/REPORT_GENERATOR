# Rapport d'avancement — Projet RHIS

**Date :** 28 juillet 2026  
**Technologies principales :** Java 17, Spring Boot, Spring Data JPA, PostgreSQL, Maven et Lombok

## 1. Objectif actuel du projet

Le projet RHIS vise à construire une application permettant de gérer des jeux de données et des modèles de rapports.

À ce stade, j'ai principalement travaillé sur :

- la structure initiale du backend ;
- la modélisation des entités JPA ;
- la connexion à PostgreSQL ;
- la création d'une architecture CRUD générique ;
- l'exposition d'un premier endpoint REST pour récupérer les datasets actifs.

## 2. Travaux réalisés

### Initialisation et configuration du projet

- Initialisation d'un projet Spring Boot avec Maven et Java 17.
- Ajout de Spring Web MVC pour créer l'API REST.
- Ajout de Spring Data JPA pour la persistance.
- Ajout du driver PostgreSQL.
- Configuration de la connexion à la base `reportdb`.
- Ajout de Lombok pour réduire le code répétitif.

### Modélisation de la base de données

J'ai créé les entités suivantes :

- `DataSetEntity` : représente un jeu de données ;
- `DataSetField` : représente un champ appartenant à un dataset ;
- `ReportTemplateEntity` : représente un modèle de rapport ;
- `ReportDataEntity` : représente l'association entre un dataset et un modèle de rapport.

Les relations JPA mises en place sont :

- une relation `OneToMany` entre un dataset et ses champs ;
- une relation `ManyToOne` entre un champ et son dataset ;
- une relation `ManyToOne` entre `ReportDataEntity` et un dataset ;
- une relation `ManyToOne` entre `ReportDataEntity` et un modèle de rapport.

Une contrainte d'unicité empêche d'associer plusieurs fois le même dataset au même rapport.

### Audit des modèles de rapports

- Ajout de la date de création avec `@CreatedDate`.
- Ajout de la date de dernière modification avec `@LastModifiedDate`.
- Activation de l'audit JPA avec `@EnableJpaAuditing`.

### Architecture CRUD générique

J'ai créé une interface et une implémentation génériques afin de mutualiser les opérations CRUD :

- création d'une entité ;
- mise à jour ;
- recherche par identifiant ;
- récupération de toutes les entités ;
- suppression ;
- vérification de l'existence d'une entité.

J'ai également ajouté une exception `RessourceNotFoundException` pour signaler qu'une ressource demandée n'existe pas.

### Gestion des datasets

- Création de `DataSetRepository` avec Spring Data JPA.
- Ajout de la méthode `findByActiveTrue()` pour récupérer uniquement les datasets actifs.
- Création de l'interface `DataSetService`.
- Création de `DataSetServiceImpl`, basée sur le service CRUD générique.
- Correction du service afin qu'il interroge réellement le repository au lieu de retourner systématiquement une liste vide.

### API REST et format de réponse

- Création du contrôleur `DataSetController`.
- Création de l'endpoint :

```http
GET /api/v2/datasets
```

- L'endpoint retourne uniquement les datasets actifs.
- Création du DTO `DataSetResponse` afin de ne pas exposer directement l'entité JPA.
- Création de `DataSetMapper` pour convertir une entité en réponse API.
- La réponse contient actuellement le nom d'affichage du dataset.
- Ajout de `@JsonIgnore` sur la collection des champs afin d'éviter une récursion JSON entre `DataSetEntity` et `DataSetField`.

## 3. Problèmes identifiés et corrigés

### Mapping JPA invalide

Le premier mapping de `ReportDataEntity` combinait `@ManyToMany` avec `@JoinColumn`, ce qui empêchait Hibernate et l'application de démarrer.

Le modèle a été corrigé avec deux relations `ManyToOne`, cohérentes avec les colonnes `dataset_id` et `report_id` de la table `report_data`.

### Liste de datasets toujours vide

La première implémentation de `DataSetServiceImpl.findAll()` retournait directement `List.of()`. Le repository n'était donc jamais interrogé.

Le service hérite maintenant de `GenericCrudServiceImpl`, dont la méthode `findAll()` appelle correctement `repository.findAll()`.

### Risque de récursion pendant la sérialisation JSON

La relation bidirectionnelle entre un dataset et ses champs pouvait provoquer une récursion pendant la génération du JSON.

La collection des champs est maintenant ignorée dans la sérialisation de `DataSetEntity`.

## 4. Vérifications effectuées

- Compilation Maven effectuée avec succès.
- Les 15 fichiers Java du code principal compilent avec Java 17.
- Vérification de l'endpoint `GET /api/v2/datasets`.
- L'endpoint répond avec le statut HTTP `200 OK`.
- Vérification directe de la table `reportdb.public.datasets`.
- La table contenait zéro ligne au moment du contrôle, ce qui expliquait la réponse vide `[]`.

## 5. État actuel

Le backend possède actuellement :

- une connexion fonctionnelle à PostgreSQL ;
- les premières entités du domaine ;
- les relations principales entre les entités ;
- une base CRUD générique ;
- un repository pour les datasets ;
- un service métier récupérant les datasets actifs ;
- un DTO et un mapper ;
- un premier endpoint REST fonctionnel.

## 6. Prochaines étapes proposées

- Remplacer `ddl-auto: create`, qui recrée les tables au démarrage et efface les données.
- Déplacer les identifiants PostgreSQL vers des variables d'environnement.
- Ajouter les endpoints de création, modification, consultation par identifiant et suppression.
- Ajouter des DTO de requête et leur validation.
- Ajouter une gestion globale des exceptions HTTP.
- Ajouter des tests unitaires pour les services et les mappers.
- Ajouter des tests d'intégration pour les repositories et les endpoints.
- Mettre en place des migrations de base de données avec Flyway ou Liquibase.
- Compléter la gestion des modèles de rapports et de leurs associations avec les datasets.

## 7. Résumé

J'ai mis en place les fondations techniques du backend RHIS et développé le premier flux complet de lecture des datasets actifs :

```text
Requête HTTP
    → Contrôleur
    → Service
    → Repository JPA
    → PostgreSQL
    → DTO de réponse
```

Le projet compile et le premier endpoint REST répond correctement. Les prochaines étapes concernent principalement la sécurisation de la configuration, la persistance durable des données, l'ajout des autres opérations métier et l'amélioration de la couverture de tests.
