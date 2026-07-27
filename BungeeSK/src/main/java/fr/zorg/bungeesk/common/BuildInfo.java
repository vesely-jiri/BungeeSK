package fr.zorg.bungeesk.common;

import java.io.InputStream;
import java.util.Properties;

/**
 * Build metadata baked into the jar at compile time by the {@code generateBuildInfo} Gradle task
 * (short git commit + build time). Lets the version commands report exactly which build is running,
 * which is handy for confirming a dev build. Falls back to {@code "unknown"} if the resource is
 * missing (e.g. an IDE run without the generation task).
 */
public final class BuildInfo {

    private static final String VERSION;
    private static final String COMMIT;
    private static final String BUILD_TIME;

    static {
        String version = "unknown", commit = "unknown", buildTime = "unknown";
        try (InputStream in = BuildInfo.class.getResourceAsStream("/bungeesk-build.properties")) {
            if (in != null) {
                final Properties props = new Properties();
                props.load(in);
                version = props.getProperty("version", version);
                commit = props.getProperty("commit", commit);
                buildTime = props.getProperty("buildTime", buildTime);
            }
        } catch (Exception ignored) {
        }
        VERSION = version;
        COMMIT = commit;
        BUILD_TIME = buildTime;
    }

    private BuildInfo() {
    }

    public static String version() {
        return VERSION;
    }

    public static String commit() {
        return COMMIT;
    }

    public static String buildTime() {
        return BUILD_TIME;
    }

    /** e.g. {@code 2.3.0 (git a1b2c3d, built 2026-07-27T12:34:56Z)}. */
    public static String describe() {
        return VERSION + " (git " + COMMIT + ", built " + BUILD_TIME + ")";
    }
}
