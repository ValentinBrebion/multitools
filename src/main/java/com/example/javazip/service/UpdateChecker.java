package com.example.javazip.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {
    
    private static final String GITHUB_API_URL = "https://api.github.com/repos/ValentinBrebion/JavaZip/releases/latest";
    private static final String CURRENT_VERSION = "1.0.0";
    
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
            URL url = URI.create(GITHUB_API_URL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "JavaZip");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String jsonResponse = response.toString();
                String latestVersion = extractJsonValue(jsonResponse, "tag_name");
                String downloadUrl = extractZipDownloadUrl(jsonResponse);
                String releaseNotes = extractJsonValue(jsonResponse, "body");
                
                if (latestVersion != null && isNewerVersion(latestVersion)) {
                    return new UpdateInfo(true, latestVersion, downloadUrl, releaseNotes);
                }
            }
            
            return new UpdateInfo(false, CURRENT_VERSION, null, null);
            
        } catch (Exception e) {
            e.printStackTrace();
            return new UpdateInfo(false, CURRENT_VERSION, null, null);
        }
    }
    
    private String extractZipDownloadUrl(String jsonResponse) {
        try {
            int assetsIndex = jsonResponse.indexOf("\"assets\"");
            if (assetsIndex == -1) return null;
            
            int browserDownloadUrlIndex = jsonResponse.indexOf("\"browser_download_url\"", assetsIndex);
            if (browserDownloadUrlIndex == -1) return null;
            
            int urlStart = jsonResponse.indexOf("\"", browserDownloadUrlIndex + 24) + 1;
            int urlEnd = jsonResponse.indexOf("\"", urlStart);
            
            return jsonResponse.substring(urlStart, urlEnd);
        } catch (Exception e) {
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
            String current = CURRENT_VERSION.replace("v", "").replace("V", "");
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
        return CURRENT_VERSION;
    }
}
