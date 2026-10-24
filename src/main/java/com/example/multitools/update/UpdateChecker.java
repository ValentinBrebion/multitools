package com.example.multitools.update;

import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mise à jour de Multitools via les releases GitHub.
 *
 * Flux : checkForUpdates() -> downloadUpdate() -> installAndExit().
 * L'asset de la release est un installateur (.exe) : on le lance en mode silencieux,
 * avec repli sur l'installateur classique si le mode silencieux échoue,
 * puis on relance l'application.
 */
public class UpdateChecker {

    private static final String FALLBACK_VERSION = "1.2.0";
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/ValentinBrebion/multitools/releases/latest";
    private static final String USER_AGENT = "Multitools";

    /** Premier asset .exe de la release. */
    private static final Pattern INSTALLER_URL =
            Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.exe)\"", Pattern.CASE_INSENSITIVE);

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    // ------------------------------------------------------------------ Vérification

    public UpdateInfo checkForUpdates() {
        try {
            HttpResponse<String> response = http.send(request(LATEST_RELEASE_URL), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();
                String tag = jsonString(json, "tag_name");
                Matcher asset = INSTALLER_URL.matcher(json);

                // Pas d'installateur dans la release => rien à proposer
                if (tag != null && asset.find() && isNewer(tag, getCurrentVersion())) {
                    return new UpdateInfo(true, tag, asset.group(1), jsonString(json, "body"));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Vérification des mises à jour impossible : " + e.getMessage());
        }
        return new UpdateInfo(false, getCurrentVersion(), null, null);
    }

    public static String getCurrentVersion() {
        try (InputStream is = UpdateChecker.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String version = props.getProperty("version");
                if (version != null && !version.isBlank()) {
                    return version.trim();
                }
            }
        } catch (IOException e) {
            System.err.println("Impossible de lire version.properties : " + e.getMessage());
        }
        return FALLBACK_VERSION;
    }

    // ------------------------------------------------------------------ Téléchargement

    /**
     * Télécharge l'installateur dans un dossier temporaire.
     * onProgress reçoit une valeur entre 0.0 et 1.0, ou -1 si la taille est inconnue.
     */
    public Path downloadUpdate(String url, DoubleConsumer onProgress) throws IOException, InterruptedException {
        HttpResponse<InputStream> response = http.send(request(url), HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Échec du téléchargement, code HTTP : " + response.statusCode());
        }

        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        Path target = Files.createTempDirectory("multitools-update")
                .resolve(url.substring(url.lastIndexOf('/') + 1));

        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            long done = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                done += read;
                if (onProgress != null) {
                    onProgress.accept(total > 0 ? (double) done / total : -1);
                }
            }
            if (total > 0 && done != total) {
                throw new IOException("Téléchargement incomplet (" + done + "/" + total + " octets)");
            }
        }
        return target;
    }

    // ------------------------------------------------------------------ Installation

    /**
     * Exécutable Multitools actuellement lancé.
     * Échoue en mode développement (lancé via java.exe), où l'auto-update n'a pas de sens.
     */
    public static Path getCurrentExecutable() {
        Path exe = ProcessHandle.current().info().command().map(Path::of)
                .orElseThrow(() -> new IllegalStateException("Impossible de déterminer l'exécutable en cours."));

        String name = exe.getFileName().toString().toLowerCase();
        if (name.equals("java.exe") || name.equals("javaw.exe")) {
            throw new IllegalStateException(
                    "La mise à jour automatique n'est disponible que depuis l'application installée.");
        }
        return exe;
    }

    /**
     * Lance le script de mise à jour (détaché de l'application) puis ferme Multitools.
     * Le script attend la fermeture de l'appli, installe, puis relance.
     */
    public static void installAndExit(Path currentExe, Path installer) throws IOException {
        Path script = createUpdateScript(currentExe, installer);

        new ProcessBuilder("cmd.exe", "/c", "start", "Multitools-Update", "/min",
                "cmd.exe", "/c", script.toString())
                .directory(script.getParent().toFile())
                .redirectErrorStream(true)
                .redirectOutput(script.resolveSibling("multitools-launcher.log").toFile())
                .start();

        Platform.exit();
        System.exit(0);
    }

    /**
     * Script .bat : attend la fermeture de l'appli, installe en silencieux (l'installateur
     * demande lui-même l'UAC si besoin), bascule sur l'installateur classique en cas d'échec,
     * relance l'appli, puis se supprime. Journal : multitools-update.log (dans %TEMP%).
     */
    private static Path createUpdateScript(Path currentExe, Path installer) throws IOException {
        Path script = Files.createTempFile("multitools-update", ".bat");
        Path log = script.resolveSibling("multitools-update.log");

        String content = String.join("\r\n",
                "@echo off",
                "chcp 65001 >nul",
                "setlocal enabledelayedexpansion",
                "set \"CURRENT=" + currentExe.toAbsolutePath() + "\"",
                "set \"INSTALLER=" + installer.toAbsolutePath() + "\"",
                "set \"LOG=" + log.toAbsolutePath() + "\"",
                "set \"IMG=" + currentExe.getFileName() + "\"",
                "set /a TRIES=0",
                "echo [%date% %time%] Script demarre > \"%LOG%\"",
                "",
                ":waitloop",
                "set /a TRIES+=1",
                "ping -n 2 127.0.0.1 >nul",
                "tasklist /fi \"imagename eq %IMG%\" | find /i \"%IMG%\" >nul",
                "if !errorlevel! equ 0 if !TRIES! lss 30 goto waitloop",
                "if !errorlevel! equ 0 goto stillrunning",
                "",
                "echo Installation silencieuse >> \"%LOG%\"",
                "start \"\" /wait \"%INSTALLER%\" /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CLOSEAPPLICATIONS",
                "set RC=!errorlevel!",
                "echo Code retour installateur: !RC! >> \"%LOG%\"",
                "if !RC! neq 0 goto interactive",
                "goto relaunch",
                "",
                ":interactive",
                "echo Echec du mode silencieux, installateur classique >> \"%LOG%\"",
                "start \"\" /wait \"%INSTALLER%\"",
                "",
                ":relaunch",
                "start \"\" \"%CURRENT%\"",
                "echo Relance demandee >> \"%LOG%\"",
                "del \"%INSTALLER%\" >nul 2>&1",
                "goto end",
                "",
                ":stillrunning",
                "echo Abandon: %IMG% tourne toujours >> \"%LOG%\"",
                "",
                ":end",
                "echo [%date% %time%] Script termine >> \"%LOG%\"",
                "(goto) 2>nul & del \"%~f0\"",
                "");

        Files.writeString(script, content);
        return script;
    }

    // ------------------------------------------------------------------ Utilitaires

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", USER_AGENT).build();
    }

    /** Valeur d'une chaîne JSON (gère les caractères échappés), ou null si absente. */
    private static String jsonString(String json, String key) {
        Matcher m = Pattern
                .compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"")
                .matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1)
                .replace("\\r", "")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /** Compare deux versions du type "v1.2.9" ou "1.3.0-beta". */
    static boolean isNewer(String latest, String current) {
        int[] a = parseVersion(latest);
        int[] b = parseVersion(current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        String core = version.trim().replaceFirst("^[vV]", "").split("[^0-9.]", 2)[0];
        return Arrays.stream(core.split("\\."))
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}