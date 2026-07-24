package fr.zorg.bungeesk.bungee.storage;

import fr.zorg.bungeesk.bungee.BungeeSK;
import fr.zorg.bungeesk.common.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;

public class GlobalVariables {

    private static final File VARIABLES_FILE = new File(BungeeSK.getInstance().getDataFolder().getAbsolutePath(), "variables.db");
    private static Connection connection;

    public static void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        if (!VARIABLES_FILE.exists()) {
            try {
                VARIABLES_FILE.getParentFile().mkdirs();
                VARIABLES_FILE.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        checkConnection();
        initTables();
    }

    private static void initTables() {
        checkConnection();
        try {
            final Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS global_variables (name TEXT PRIMARY KEY, value TEXT, type TEXT);");
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static Pair<byte[], String> getGlobalVariable(String variableName) {
        checkConnection();
        try (final PreparedStatement statement = connection.prepareStatement("SELECT value, type FROM global_variables WHERE name = ?;")) {
            statement.setString(1, variableName);
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    final byte[] value = Base64.getDecoder().decode(resultSet.getString("value"));
                    final String type = resultSet.getString("type");
                    return Pair.from(value, type);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void setGlobalVariable(String name, byte[] value, String type) {
        checkConnection();
        final String base64Value = Base64.getEncoder().encodeToString(value);
        try (final PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO global_variables VALUES (?, ?, ?);")) {
            statement.setString(1, name);
            statement.setString(2, base64Value);
            statement.setString(3, type);
            statement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void deleteGlobalVariable(String name) {
        checkConnection();
        try (final PreparedStatement statement = connection.prepareStatement("DELETE FROM global_variables WHERE name = ?;")) {
            statement.setString(1, name);
            statement.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + VARIABLES_FILE.getAbsolutePath());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}