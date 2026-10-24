package com.example.multitools.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simulation du téléchargement de l'installateur de mise à jour.
 * L'installation n'est pas lancée afin de ne pas modifier l'application.
 */
public class UpdateSimulationTest {

    public static void main(String[] args) throws IOException {
        System.out.println("=== Simulation de mise à jour Multitools ===\n");

        UpdateChecker checker = new UpdateChecker();

        // 1. Vérifier les mises à jour
        System.out.println("1. Vérification des mises à jour...");
        UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();

        if (!updateInfo.hasUpdate) {
            System.out.println("Aucune mise à jour disponible. Version actuelle: "
                    + UpdateChecker.getCurrentVersion());
            return;
        }

        System.out.println("✓ Mise à jour disponible: " + updateInfo.latestVersion);
        System.out.println("  URL de téléchargement: " + updateInfo.downloadUrl);
        System.out.println();

        // 2. Télécharger l'installateur sans le lancer
        System.out.println("2. Téléchargement de l'installateur (sans installation)...");
        Path downloadedFile = null;
        try {
            downloadedFile = checker.downloadUpdate(updateInfo.downloadUrl, progress -> {
                if (progress >= 0) {
                    System.out.printf("  Progression: %.1f%%\n", progress * 100);
                } else {
                    System.out.println("  Progression: indéterminée");
                }
            });

            System.out.println("✓ Installateur téléchargé: " + downloadedFile);
            System.out.println("  Taille du fichier: " + Files.size(downloadedFile) + " octets");
            System.out.println();
            System.out.println("=== Simulation terminée avec succès ===");
            System.out.println("L'installateur n'a pas été exécuté.");
        } catch (IOException e) {
            System.err.println("✗ Erreur lors de la simulation: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("✗ Téléchargement interrompu.");
        } finally {
            if (downloadedFile != null) {
                Path downloadDirectory = downloadedFile.getParent();
                Files.deleteIfExists(downloadedFile);
                Files.deleteIfExists(downloadDirectory);
                System.out.println("Fichiers temporaires supprimés.");
            }
        }
    }
}
