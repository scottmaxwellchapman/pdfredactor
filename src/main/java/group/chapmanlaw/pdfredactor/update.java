package group.chapmanlaw.pdfredactor;

import javax.swing.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class update {

    private static final String CURRENT_VERSION = "1.1.0";
    private static final String VERSION_URL = "https://www.chapmanlaw.group/wordpress/index.php/pdf-redactor/version.txt";
    private static final int TIMEOUT_MS = 2000;

    private static String exitUrl = "https://www.chapmanlaw.group/wordpress/index.php/pdf-redactor/";

    public static void checkForUpdates() {
        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion == null || latestVersion.isBlank()) {
                return;
            }

            if (isRemoteVersionNewer(CURRENT_VERSION, latestVersion.trim())) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "A new version (v" + latestVersion + ") may be available.\nVisit: " + exitUrl,
                        "Update available",
                        JOptionPane.INFORMATION_MESSAGE));
            }
        } catch (Exception e) {
            System.out.println("Update check skipped: " + e.getMessage());
        }
    }

    private static String fetchLatestVersion() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(VERSION_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    static boolean isRemoteVersionNewer(String current, String remote) {
        String[] currentParts = current.split("\\.");
        String[] remoteParts = remote.split("\\.");
        int max = Math.max(currentParts.length, remoteParts.length);

        for (int i = 0; i < max; i++) {
            int cur = (i < currentParts.length) ? parsePart(currentParts[i]) : 0;
            int rem = (i < remoteParts.length) ? parsePart(remoteParts[i]) : 0;
            if (rem > cur) {
                return true;
            }
            if (rem < cur) {
                return false;
            }
        }
        return false;
    }

    private static int parsePart(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static String getExitUrl() {
        return exitUrl;
    }
}
