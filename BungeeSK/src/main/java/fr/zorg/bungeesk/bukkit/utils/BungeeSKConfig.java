package fr.zorg.bungeesk.bukkit.utils;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;

/**
 * config.yml for the game-server (Bukkit) side of BungeeSK. Lets admins define the proxy connection
 * once — instead of hard-coding the address/port/password in every script — and opt into
 * auto-connect + auto-reconnect. Generated programmatically on first run (mirrors the proxy-side
 * {@code BungeeConfig}); nested keys are written with the {@code $ -> .} convention.
 */
public enum BungeeSKConfig {

    CONNECTION$AUTO_CONNECT(false, "Automatically connect to the proxy on server start using the values below.",
            "When true you no longer need a 'create new bungee connection' script."),
    CONNECTION$ADDRESS("127.0.0.1", "Address of the proxy. Use 127.0.0.1 if the proxy runs on the same machine."),
    CONNECTION$PORT(20000, "Port BungeeSK listens on, on the proxy. Must match the proxy's config.yml."),
    CONNECTION$PASSWORD("", "Connection password. Must be identical to the proxy's config.yml password."),
    RECONNECT$ENABLED(true, "Automatically try to reconnect if the connection to the proxy drops."),
    RECONNECT$INITIAL_DELAY_SECONDS(5, "Delay before the first reconnect attempt."),
    RECONNECT$MAX_DELAY_SECONDS(60, "Maximum delay between reconnect attempts (exponential backoff caps here)."),
    DEBUG(false, "Log extra information about the connection to the console.");

    private Object value;
    private final String[] comments;

    BungeeSKConfig(final Object defaultValue, final String... comments) {
        this.value = defaultValue;
        this.comments = comments;
    }

    public Object getValue() {
        return this.value;
    }

    public String[] getComments() {
        return this.comments;
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) this.value;
    }

    public int getInt() {
        return ((Number) this.value).intValue();
    }

    public boolean getBoolean() {
        return this.value instanceof Boolean ? (Boolean) this.value : Boolean.parseBoolean(String.valueOf(this.value));
    }

    public String getString() {
        return String.valueOf(this.value);
    }

    private void setValue(final Object newValue) {
        this.value = newValue;
    }

    public static void init() {
        try {
            final File file = new File(BungeeSK.getInstance().getDataFolder(), "config.yml");
            final YamlFile config;
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                config = new YamlFile(file);
            } else {
                config = YamlFile.loadConfiguration(file);
            }

            for (final BungeeSKConfig field : values()) {
                // Enum names use '$' as a nesting separator and '_' as a word separator, e.g.
                // RECONNECT$INITIAL_DELAY_SECONDS -> reconnect.initial-delay-seconds.
                final String key = field.name().toLowerCase().replace("_", "-").replaceAll("\\$", ".");

                if (config.get(key) == null)
                    config.set(key, field.get());

                field.setValue(config.get(key));

                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < field.getComments().length; i++) {
                    sb.append(field.getComments()[i]);
                    if (i != (field.getComments().length - 1))
                        sb.append("\n");
                }
                if (sb.length() > 0)
                    config.setComment(key, sb.toString());
            }
            config.save();
        } catch (IOException ex) {
            BungeeSK.getInstance().getLogger().severe("Could not load BungeeSK config.yml: " + ex.getMessage());
        }
    }
}
