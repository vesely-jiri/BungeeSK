package fr.zorg.velocitysk.storage;

import fr.zorg.bungeesk.common.utils.Pair;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.utils.BungeeConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.Map;

/**
 * Storage for global variables on the proxy. Uses the built-in SQLite file by default; when
 * {@code redis.enabled} is set it uses Redis instead (handy to share variables across proxies). All
 * Redis calls are fail-safe — on any error they log and behave as if the value is absent.
 */
public class GlobalVariables {

    private static final File VARIABLES_FILE = new File(BungeeSK.getDataDirectory().toFile(), "variables.db");
    private static final String REDIS_PREFIX = "bungeesk:gvar:";

    private static Connection connection;
    private static boolean useRedis;
    private static JedisPool redisPool;

    public static void init() {
        useRedis = BungeeConfig.REDIS$ENABLED.get();
        if (useRedis) {
            initRedis();
        } else {
            initSqlite();
        }
    }

    // ---- backend selection --------------------------------------------------

    public static Pair<byte[], String> getGlobalVariable(String variableName) {
        return useRedis ? redisGet(variableName) : sqliteGet(variableName);
    }

    public static void setGlobalVariable(String name, byte[] value, String type) {
        if (useRedis)
            redisSet(name, value, type);
        else
            sqliteSet(name, value, type);
    }

    public static void deleteGlobalVariable(String name) {
        if (useRedis)
            redisDelete(name);
        else
            sqliteDelete(name);
    }

    // ---- Redis backend ------------------------------------------------------

    private static void initRedis() {
        final String host = BungeeConfig.REDIS$HOST.get();
        final int port = BungeeConfig.REDIS$PORT.get();
        final String password = BungeeConfig.REDIS$PASSWORD.get();
        if (password == null || password.isEmpty())
            redisPool = new JedisPool(host, port);
        else
            redisPool = new JedisPool(new JedisPoolConfig(), host, port, 2000, password);
    }

    private static Pair<byte[], String> redisGet(String name) {
        try (Jedis jedis = redisPool.getResource()) {
            final Map<String, String> map = jedis.hgetAll(REDIS_PREFIX + name);
            if (map == null || map.isEmpty())
                return null;
            final String base64 = map.get("value");
            final String type = map.get("type");
            if (base64 == null || type == null)
                return null;
            return Pair.from(Base64.getDecoder().decode(base64), type);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static void redisSet(String name, byte[] value, String type) {
        try (Jedis jedis = redisPool.getResource()) {
            final String key = REDIS_PREFIX + name;
            jedis.hset(key, "value", Base64.getEncoder().encodeToString(value));
            jedis.hset(key, "type", type);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void redisDelete(String name) {
        try (Jedis jedis = redisPool.getResource()) {
            jedis.del(REDIS_PREFIX + name);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ---- SQLite backend (default) -------------------------------------------

    private static void initSqlite() {
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

    private static Pair<byte[], String> sqliteGet(String variableName) {
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

    private static void sqliteSet(String name, byte[] value, String type) {
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

    private static void sqliteDelete(String name) {
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
