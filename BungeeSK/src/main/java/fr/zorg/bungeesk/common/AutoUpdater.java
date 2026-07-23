package fr.zorg.bungeesk.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

public class AutoUpdater {

    public static boolean isUpToDate(String currentVersion) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) URI
                    .create("https://api.github.com/repos/ZorgBtw/BungeeSK/releases/latest")
                    .toURL()
                    .openConnection();
            connection.setRequestProperty("User-Agent", "BungeeSK-UpdateChecker");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            if (connection.getResponseCode() != 200)
                return true;
            final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
            final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            final JsonObject result = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            final int latestVersionInt = Integer.parseInt(result.get("tag_name").getAsString().replaceAll("\\D", ""));
            final int currentVersionInt = Integer.parseInt(currentVersion.replaceAll("\\D", ""));
            return latestVersionInt <= currentVersionInt;
        } catch (Exception ex) {
            return true;
        }
    }

    //TODO: add auto downloader

}
