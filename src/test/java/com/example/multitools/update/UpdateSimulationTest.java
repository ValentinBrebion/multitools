package com.example.multitools.update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulation de test pour le processus de mise à jour.
 * Cette classe simule le téléchargement et l'installation sans effectuer
 * réellement le remplacement de l'exécutable.
 */
public class UpdateSimulationTest {

    public static void main(String[] args) {
        System.out.println("=== Simulation de mise à jour Multitools ===\n");

        UpdateChecker checker = new UpdateChecker();

        // 1. Vérifier les mises à jour
        System.out.println("1. Vérification des mises à jour...");
        UpdateChecker.UpdateInfo updateInfo = checker.checkForUpdates();

        if (!updateInfo.hasUpdate) {
            System.out.println("⚠ Aucune mise à jour disponible. Version actuelle: " + updateInfo.latestVersion);
            System.out.println("  Pour tester, nous allons simuler avec la version actuelle...\n");

            // Pour le test, utiliser la même version pour simuler un téléchargement
            // Dans un vrai scénario, il y aurait une nouvelle version
            System.out.println("Note: En production, ce processus se déclenche uniquement quand une nouvelle version est disponible.");
            System.out.println("Pour tester le téléchargement, nous utilisons l'URL de la dernière release GitHub.\n");

            try {
                // Récupérer l'URL de téléchargement via la méthode existante
                String latestUrl = getLatestReleaseDownloadUrl();
                if (latestUrl != null) {
                    updateInfo = new UpdateChecker.UpdateInfo(
                        true,
                        updateInfo.latestVersion + " (test)",
                        latestUrl,
                        "Test de simulation"
                    );
                    System.out.println("✓ Utilisation de la dernière release pour le test");
                    System.out.println("  URL de téléchargement: " + updateInfo.downloadUrl);
                    System.out.println();
                } else {
                    System.out.println("✗ Impossible de récupérer l'URL de la dernière release");
                    return;
                }
            } catch (Exception e) {
                System.out.println("✗ Erreur lors de la récupération de l'URL: " + e.getMessage());
                return;
            }
        } else {
            System.out.println("✓ Mise à jour disponible: " + updateInfo.latestVersion);
            System.out.println("  URL de téléchargement: " + updateInfo.downloadUrl);
            System.out.println();
        }

        // 2. Simuler le téléchargement
        System.out.println("2. Simulation du téléchargement...");
        AtomicBoolean downloadComplete = new AtomicBoolean(false);

        try {
            Path downloadedFile = checker.downloadUpdate(updateInfo.downloadUrl, progress -> {
                if (progress >= 0) {
                    System.out.printf("  Progression: %.1f%%\n", progress * 100);
                } else {
                    System.out.println("  Progression: indéterminée");
                }
            });

            downloadComplete.set(true);
            System.out.println("✓ Téléchargement terminé: " + downloadedFile);
            System.out.println("  Taille du fichier: " + Files.size(downloadedFile) + " octets");
            System.out.println();

            // 3. Simuler la création du script de mise à jour
            System.out.println("3. Simulation de la création du script de mise à jour...");
            String currentExePath = getCurrentExecutablePathForSimulation();
            System.out.println("  Exécutable actuel: " + currentExePath);

            Path scriptPath = checker.createUpdateScript(
                Path.of(currentExePath),
                downloadedFile
            );

            System.out.println("✓ Script créé: " + scriptPath);
            System.out.println("  Contenu du script (premières lignes):");
            Files.lines(scriptPath)
                .limit(10)
                .forEach(line -> System.out.println("    " + line));
            System.out.println();

            // 4. Simuler le processus de mise à jour (sans exécution réelle)
            System.out.println("4. Simulation du processus de mise à jour...");
            System.out.println("  Ce que le script ferait:");
            System.out.println("    - Attendre que l'application se ferme");
            System.out.println("    - Remplacer " + Path.of(currentExePath).getFileName() + " par le nouveau fichier");
            System.out.println("    - Relancer l'application");
            System.out.println("    - Supprimer le fichier temporaire");
            System.out.println("    - Se supprimer lui-même");
            System.out.println();

            // 5. Nettoyage
            System.out.println("5. Nettoyage des fichiers temporaires...");
            Files.deleteIfExists(downloadedFile);
            Files.deleteIfExists(scriptPath);
            Files.deleteIfExists(downloadedFile.getParent());
            System.out.println("✓ Fichiers temporaires supprimés");
            System.out.println();

            System.out.println("=== Simulation terminée avec succès ===");
            System.out.println("\nLe processus de mise à jour fonctionnerait comme suit en production:");
            System.out.println("1. L'utilisateur accepte la mise à jour");
            System.out.println("2. La barre de progression affiche le téléchargement");
            System.out.println("3. Le script .bat est généré");
            System.out.println("4. L'application quitte");
            System.out.println("5. Le script remplace l'exécutable et relance l'application");

        } catch (IOException | InterruptedException e) {
            System.err.println("✗ Erreur lors de la simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Simule la récupération du chemin de l'exécutable pour le test.
     * En mode développement, retourne un chemin factice.
     */
    private static String getCurrentExecutablePathForSimulation() {
        try {
            return UpdateChecker.getCurrentExecutablePath();
        } catch (IllegalStateException e) {
            // En mode développement, utiliser un chemin factice
            return "C:\\Program Files\\Multitools\\Multitools.exe";
        }
    }

    /**
     * Récupère l'URL de téléchargement de la dernière release GitHub.
     */
    private static String getLatestReleaseDownloadUrl() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/ValentinBrebion/multitools/releases/latest"))
                .header("User-Agent", "Multitools")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String jsonResponse = response.body();

            // Extraire browser_download_url comme dans UpdateChecker
            String key = "\"browser_download_url\":\"";
            int start = jsonResponse.indexOf(key);

            if (start != -1) {
                start += key.length();
                int end = jsonResponse.indexOf("\"", start);

                if (end != -1) {
                    return jsonResponse.substring(start, end);
                }
            }
        }

        return null;
    }
}
