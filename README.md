# Multitools Foundation

Application de bureau Java modulaire offrant plusieurs outils utilitaires.

## Fonctionnalités

### Outil de Compression (Actif)
- Ajouter des fichiers individuels à compresser
- Ajouter des dossiers complets à compresser
- Supprimer des fichiers de la liste
- Vider la liste complète
- Compresser les fichiers sélectionnés en un fichier ZIP
- Affichage du taux de compression
- Glisser-déposer de fichiers

### Outils Futurs (En développement)
- Outil de manipulation de texte
- Outil de manipulation d'images
- Architecture extensible pour ajouter de nouveaux outils

## Prérequis

- Java 21 ou supérieur
- Maven 3.6 ou supérieur

## Compilation

```bash
mvn clean compile
```

## Exécution

### Avec Maven

```bash
mvn javafx:run
```

### Après compilation

```bash
mvn clean package
java -jar target/multitools-1.2.0.jar
```

## Utilisation

1. Lancez l'application
2. L'outil de compression s'ouvre par défaut
3. Cliquez sur "Ajouter fichiers" pour sélectionner des fichiers
4. Cliquez sur "Ajouter dossier" pour sélectionner un dossier complet
5. Les fichiers/dossiers apparaissent dans la liste
6. Cliquez sur "Compresser" pour créer le fichier ZIP
7. Choisissez l'emplacement et le nom du fichier ZIP
8. L'application affiche le taux de compression après la réussite

## Structure du Projet

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── multitools/
│   │               ├── MainApp.java                    # Point d'entrée de l'application
│   │               ├── core/
│   │               │   ├── Tool.java                  # Interface pour les outils
│   │               │   ├── ToolManager.java           # Gestionnaire des outils
│   │               │   └── NavigationManager.java     # Gestionnaire de navigation
│   │               ├── tools/
│   │               │   ├── compression/
│   │               │   │   ├── CompressionTool.java   # Implémentation de l'outil compression
│   │               │   │   ├── CompressionController.java # Contrôleur de l'interface compression
│   │               │   │   └── ZipService.java        # Service de compression ZIP
│   │               │   ├── text/
│   │               │   │   ├── TextTool.java          # Outil de texte (placeholder)
│   │               │   │   └── TextController.java     # Contrôleur texte (placeholder)
│   │               │   └── image/
│   │               │       ├── ImageTool.java         # Outil d'image (placeholder)
│   │               │       └── ImageController.java    # Contrôleur image (placeholder)
│   │               └── update/
│   │                   └── UpdateChecker.java          # Système de mise à jour
│   └── resources/
│       ├── fxml/
│       │   └── main.fxml                              # Interface utilisateur compression
│       └── version.properties                          # Version de l'application
```

## Technologies

- JavaFX 21.0.6 - Framework d'interface graphique
- Java 21 - Langage de programmation
- Maven - Gestion des dépendances

## Architecture

L'application utilise une architecture modulaire basée sur une interface `Tool` qui permet d'ajouter facilement de nouveaux outils:

- **Core**: Contient les gestionnaires et l'interface de base
- **Tools**: Contient les implémentations des différents outils
- **Update**: Gère les mises à jour automatiques de l'application

## Développement

Pour ajouter un nouvel outil:

1. Créer une classe implémentant l'interface `Tool`
2. Créer le contrôleur FXML correspondant
3. Enregistrer l'outil dans le `ToolManager`
4. Ajouter l'interface FXML dans `src/main/resources/fxml/`
