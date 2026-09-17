package com.example.javazip.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.DoubleConsumer;

public class UpdateChecker {
    
    private static final String FALLBACK_VERSION = "1.2.1";
    
    public static class UpdateInfo {
        public final boolean hasUpdate;
        public final String latestVersion;
        public final String downloadUrl;
        public final String releaseNotes;
        
        public UpdateInfo(boolean hasUpdate, String latestVersion, String downloadUrl, String releaseNotes) {
            this.hasUpdate = hasUpdate;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.releaseNotes = releaseNotes;
        }
    }
    
    public UpdateInfo checkForUpdates() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.github.com/repos/ValentinBrebion/multitools/releases/latest"
                    ))
                    .header("User-Agent", "JavaZip")
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {

                String jsonResponse = response.body();

                String latestVersion = extractJsonValue(jsonResponse, "tag_name");
                String downloadUrl = extractZipDownloadUrl(jsonResponse);
                String releaseNotes = extractJsonValue(jsonResponse, "body");

                if (latestVersion != null && isNewerVersion(latestVersion)) {
                    return new UpdateInfo(
                            true,
                            latestVersion,
                            downloadUrl,
                            releaseNotes
                    );
                }
            }

            return new UpdateInfo(false, getCurrentVersion(), null, null);

        } catch (Exception e) {
            e.printStackTrace();
            return new UpdateInfo(false, getCurrentVersion(), null, null);
        }
    }
    
    private String extractZipDownloadUrl(String jsonResponse) {
        try {
            String key = "\"browser_download_url\":\"";

            int start = jsonResponse.indexOf(key);

            if (start == -1) {
                return null;
            }

            start += key.length();

            int end = jsonResponse.indexOf("\"", start);

            if (end == -1) {
                return null;
            }
            return jsonResponse.substring(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String extractJsonValue(String json, String key) {
        try {
            String searchPattern = "\"" + key + "\":\"";
            int index = json.indexOf(searchPattern);
            if (index == -1) {
                searchPattern = "\"" + key + "\":";
                index = json.indexOf(searchPattern);
                if (index == -1) return null;
                index += searchPattern.length();
                if (json.charAt(index) == '"') {
                    index++;
                    int endIndex = json.indexOf("\"", index);
                    return json.substring(index, endIndex);
                }
                return null;
            }
            index += searchPattern.length();
            int endIndex = json.indexOf("\"", index);
            return json.substring(index, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean isNewerVersion(String latestVersion) {
        try {
            String current = getCurrentVersion().replace("v", "").replace("V", "");
            String latest = latestVersion.replace("v", "").replace("V", "");
            
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static String getCurrentVersion() {
        String version = UpdateChecker.class.getPackage().getImplementationVersion();
        return (version != null) ? version : FALLBACK_VERSION;
    }

    /**
     * Télécharge le fichier de mise à jour vers un dossier temporaire.
     * onProgress reçoit une valeur entre 0.0 et 1.0 (ou -1 si la taille est inconnue).
     * Retourne le chemin du fichier téléchargé.
     */
    public Path downloadUpdate(String downloadUrl, DoubleConsumer onProgress) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "JavaZip")
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Échec du téléchargement, code HTTP : " + response.statusCode());
        }

        long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1);

        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        if (fileName.isBlank()) {
            fileName = "JavaZip-update.exe";
        }

        Path tempFile = Files.createTempDirectory("javazip-update").resolve(fileName);

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int len;

            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
                downloaded += len;

                if (onProgress != null) {
                    if (totalBytes > 0) {
                        onProgress.accept((double) downloaded / totalBytes);
                    } else {
                        onProgress.accept(-1);
                    }
                }
            }
        }

        return tempFile;
    }

   /**
     * Chemin de l'exécutable JavaZip actuellement lancé (le .exe portable).
     */
    public static Path getCurrentExecutablePath() {
        return ProcessHandle.current()
                .info()
                .command()
                .map(Path::of)
                .orElseThrow(() -> new IllegalStateException(
                        "Impossible de déterminer le chemin de l'exécutable courant."));
    }
 
    /**
     * Génère un script Windows (.bat) qui, une fois JavaZip fermé :
     * remplace l'ancien .exe par le nouveau, relance l'appli, puis se supprime.
     * Retourne le chemin du script généré.
     */
    public Path createUpdateScript(Path currentExe, Path newExe) throws IOException {
        Path scriptPath = Files.createTempFile("javazip-update", ".bat");
        Path logPath = scriptPath.resolveSibling("javazip-update.log");
 
        String script =
                "@echo off\r\n" +
                "chcp 65001 >nul\r\n" +
                "setlocal enabledelayedexpansion\r\n" +
                "set \"CURRENT=" + currentExe.toAbsolutePath() + "\"\r\n" +
                "set \"NEW=" + newExe.toAbsolutePath() + "\"\r\n" +
                "set \"LOG=" + logPath.toAbsolutePath() + "\"\r\n" +
                "set \"IMG=" + currentExe.getFileName() + "\"\r\n" +
                "set /a TRIES=0\r\n" +
                "\r\n" +
                "echo [%date% %time%] Script demarre > \"%LOG%\"\r\n" +
                "echo CURRENT=%CURRENT% >> \"%LOG%\"\r\n" +
                "echo NEW=%NEW% >> \"%LOG%\"\r\n" +
                "echo IMG=%IMG% >> \"%LOG%\"\r\n" +
                "\r\n" +
                ":waitloop\r\n" +
                "set /a TRIES+=1\r\n" +
                "timeout /t 1 /nobreak >nul\r\n" +
                "tasklist /fi \"imagename eq %IMG%\" | find /i \"%IMG%\" >nul\r\n" +
                "if !errorlevel! equ 0 (\r\n" +
                "    echo [tentative !TRIES!] %IMG% tourne encore, on attend >> \"%LOG%\"\r\n" +
                "    if !TRIES! lss 30 goto waitloop\r\n" +
                "    echo [tentative !TRIES!] Abandon apres 30 tentatives, %IMG% semble toujours actif >> \"%LOG%\"\r\n" +
                "    goto end\r\n" +
                ")\r\n" +
                "echo [tentative !TRIES!] %IMG% ne tourne plus, on procede au remplacement >> \"%LOG%\"\r\n" +
                "\r\n" +
                "if not exist \"%NEW%\" (\r\n" +
                "    echo ERREUR: le fichier source %NEW% n'existe pas >> \"%LOG%\"\r\n" +
                "    goto end\r\n" +
                ")\r\n" +
                "\r\n" +
                "copy /y \"%NEW%\" \"%CURRENT%\" >> \"%LOG%\" 2>&1\r\n" +
                "if !errorlevel! neq 0 (\r\n" +
                "    echo ERREUR: la copie a echoue avec le code !errorlevel! >> \"%LOG%\"\r\n" +
                "    goto end\r\n" +
                ")\r\n" +
                "echo Copie reussie >> \"%LOG%\"\r\n" +
                "\r\n" +
                "start \"\" \"%CURRENT%\"\r\n" +
                "echo Relance demandee >> \"%LOG%\"\r\n" +
                "\r\n" +
                "del \"%NEW%\" >nul 2>&1\r\n" +
                "\r\n" +
                ":end\r\n" +
                "echo [%date% %time%] Script termine >> \"%LOG%\"\r\n" +
                "(goto) 2>nul & del \"%~f0\"\r\n";
 
        Files.writeString(scriptPath, script);
        return scriptPath;
    }
 
    /**
     * Lance le script de mise à jour en arrière-plan (détaché du process JavaZip),
     * puis ferme immédiatement l'application courante.
     */
    public static void runUpdateScriptAndExit(Path scriptPath) throws IOException {
        new ProcessBuilder("cmd.exe", "/c", scriptPath.toAbsolutePath().toString())
                .directory(scriptPath.getParent().toFile())
                .start();
 
        javafx.application.Platform.exit();
        System.exit(0);
    }
}