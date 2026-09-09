# JavaZip - Application de Compression de Fichiers

Application de bureau Java permettant de compresser des fichiers et dossiers en format ZIP.

## Fonctionnalités

- Ajouter des fichiers individuels à compresser
- Ajouter des dossiers complets à compresser
- Supprimer des fichiers de la liste
- Vider la liste complète
- Compresser les fichiers sélectionnés en un fichier ZIP
- Affichage du taux de compression

## Prérequis

- Java 11 ou supérieur
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
java -jar target/javazip-1.0.0.jar
```

## Utilisation

1. Lancez l'application
2. Cliquez sur "Ajouter fichiers" pour sélectionner des fichiers
3. Cliquez sur "Ajouter dossier" pour sélectionner un dossier complet
4. Les fichiers/dossiers apparaissent dans la liste
5. Cliquez sur "Compresser" pour créer le fichier ZIP
6. Choisissez l'emplacement et le nom du fichier ZIP
7. L'application affiche le taux de compression après la réussite

## Structure du Projet

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── javazip/
│   │               ├── JavaZipApp.java          # Point d'entrée de l'application
│   │               ├── controller/
│   │               │   └── MainController.java # Contrôleur de l'interface
│   │               └── service/
│   │                   └── ZipService.java      # Service de compression ZIP
│   └── resources/
│       └── fxml/
│           └── main.fxml                       # Interface utilisateur
```

## Technologies

- JavaFX 17.0.2 - Framework d'interface graphique
- Java 11 - Langage de programmation
- Maven - Gestion des dépendances
