# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

## [Unreleased]

### Added

- Assistant de génération de rapports à partir d'une demande en langage naturel.
- Endpoint backend protégé pour planifier, valider et générer un rapport avec Mistral via Spring AI.
- Catalogue compact des datasets et relations transmis au planneur LLM.
- Clarifications structurées et correction automatique unique des plans invalides.
- Panneau de prévisualisation intégré au configurateur de rapports.
- Tests backend de sécurité, configuration, planification et génération, ainsi que tests frontend de l'assistant et de la prévisualisation.

### Changed

- Amélioration de la lisibilité du tableau de prévisualisation.
- Documentation du flux de génération de rapports en langage naturel et de sa validation.

### Fixed

- Suppression de la boucle de questions inutiles lorsque la demande contient déjà les informations requises.
