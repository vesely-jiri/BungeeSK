package fr.zorg.bungeesk.bungee.utils;

import fr.zorg.bungeesk.bungee.BungeeSK;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;

public enum BungeeConfig {

    PORT(20000, "The port BungeeSK will listen to. This port needs to be opened."),
    PASSWORD(generatePassword(), "Password of the connection. Needs to be the same for every connection"),
    ENCRYPT(true, "This isn't recommended to turn this off. " +
            "The password will be less efficient than before and the connection more exposed. " +
            "Turn this off only if you know what you are doing"),
    DEBUG(false, "You will be notified in the console when the server receives a packet"),
    FILES$SYNC_AT_CONNECT(true, "Global scripts located at \"/plugins/BungeeSK/scripts\" will be synchronised whenever a spigot connects to the BungeeSK's server",
            "If you need to retrieve scripts manually, please refer to https://skripthub.net/docs/?id=5689"),
    FILES$AUTO_UPDATE(false, "On change of every script, this will be updated in every BungeeSK server"),
    FILES$AUTO_DELETE(true, "On deletion of a global script, it will be deleted all across the network"),
    WHITELIST_IP$ENABLE(false, "If enabled and if the client doesn't match any IP below, it will be disconnected"),
    WHITELIST_IP$WHITELIST(new String[]{"127.0.0.1"}, "List of all whitelisted IPs"),
    MESSAGES(false, "If enabled, a message will be sent when a client connects/disconnects from the server"),
    REDIS$ENABLED(false, "Store global variables in Redis instead of the built-in SQLite file.",
            "Useful to share global variables across multiple proxies. Leave false to keep local SQLite storage."),
    REDIS$HOST("127.0.0.1", "Redis host (used only when redis.enabled is true)."),
    REDIS$PORT(6379, "Redis port."),
    REDIS$PASSWORD("", "Redis password, or empty for no authentication.");

    private Object value;
    private final String[] comments;

    BungeeConfig(final Object defaultValue, final String... comments) {
        this.value = defaultValue;
        this.comments = comments;
    }

    public Object getValue() {
        return this.value;
    }

    public String[] getComments() {
        return this.comments;
    }

    public <T> T get() {
        return (T) this.value;
    }

    public ArrayList<?> getList() {
        return ((ArrayList<?>) this.value);
    }

    private void setValue(final Object newValue) {
        this.value = newValue;
    }

    public static void init() {
        try {
            final File file = new File(BungeeSK.getInstance().getDataFolder(), "config.yml");
            YamlFile config;
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                config = new YamlFile(file);
            } else
                config = YamlFile.loadConfiguration(file);

            for (final BungeeConfig field : values()) {
                final String key = field.name().toLowerCase().replaceAll("\\$", ".");

                if (config.get(key) == null)
                    config.set(key, field.get());

                if (field.equals(PASSWORD)) {
                    String password = (String) config.get(key);
                    password = password.replaceAll("&", "");
                    password = password.replaceAll("§", "");
                    field.setValue(password);
                } else {
                    field.setValue(config.get(key));
                }

                final StringBuilder sb = new StringBuilder(field.getComments().length);
                for (int i = 0; i < field.getComments().length; i++) {
                    sb.append(field.getComments()[i]);
                    if (i != (field.getComments().length - 1))
                        sb.append("\n");
                }
                config.setComment(key, sb.toString());

            }
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String generatePassword() {
        final String[] dataSet = {
                "a", "b", "c", "d", "e", "f", "g", "h", "i",
                "j", "k", "l", "m", "n", "o", "p", "q", "r",
                "s", "t", "u", "v", "w", "x", "y", "z", "0",
                "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "!", "/", ":", ";", ",", "*"
        };
        // SecureRandom (not java.util.Random/Math.random): the password guards the socket, so it must
        // be cryptographically strong, not seeded from wall-clock time. Local instance on purpose —
        // this runs inside enum-constant init, before any static field below the constants exists.
        final SecureRandom random = new SecureRandom();
        final StringBuilder builder = new StringBuilder();
        for (int loop = 0; loop < 16; loop++) {
            final String value = dataSet[random.nextInt(dataSet.length)];
            if (random.nextBoolean())
                builder.append(value.toUpperCase());
            else
                builder.append(value);
        }
        return builder.toString();
    }

}