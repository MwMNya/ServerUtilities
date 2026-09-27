package serverutils.client.scoreboard;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import serverutils.ServerUtilities;

public final class AnimatedScoreboardConfig {

    private static final String GENERAL = Configuration.CATEGORY_GENERAL;
    private static final String CONTENT = "content";
    private static final String APPEARANCE = "appearance";
    private static final long RELOAD_CHECK_INTERVAL_MILLIS = 2_000L;

    public static boolean enabled;
    public static boolean hideVanillaSidebar;
    public static boolean hideInDebug;
    public static int animationIntervalTicks;
    public static String[] titleFrames;
    public static String[] lines;
    public static String alignment;
    public static int xOffset;
    public static int yOffset;
    public static double scale;
    public static int lineSpacing;
    public static int horizontalPadding;
    public static boolean textShadow;
    public static int backgroundColor;
    public static int titleBackgroundColor;

    private static File file;
    private static long lastModified = Long.MIN_VALUE;
    private static long nextReloadCheck;

    private AnimatedScoreboardConfig() {}

    public static void setConfigDirectory(File configDirectory) {
        file = resolveConfigFile(configDirectory);
    }

    public static void loadFromDisk() {
        load();
    }

    public static boolean reloadIfChanged() {
        if (file == null || System.currentTimeMillis() < nextReloadCheck) return false;
        nextReloadCheck = System.currentTimeMillis() + RELOAD_CHECK_INTERVAL_MILLIS;
        if (file.lastModified() == lastModified) return false;
        load();
        return true;
    }

    public static void applyServerConfig(boolean remoteEnabled, boolean remoteHideVanillaSidebar,
            boolean remoteHideInDebug, int remoteAnimationIntervalTicks, String[] remoteTitleFrames,
            String[] remoteLines, String remoteAlignment, int remoteXOffset, int remoteYOffset, double remoteScale,
            int remoteLineSpacing, int remoteHorizontalPadding, boolean remoteTextShadow, int remoteBackgroundColor,
            int remoteTitleBackgroundColor) {
        enabled = remoteEnabled;
        hideVanillaSidebar = remoteHideVanillaSidebar;
        hideInDebug = remoteHideInDebug;
        animationIntervalTicks = Math.max(1, remoteAnimationIntervalTicks);
        titleFrames = remoteTitleFrames == null ? new String[0] : remoteTitleFrames;
        lines = remoteLines == null ? new String[0] : remoteLines;
        alignment = "LEFT".equalsIgnoreCase(remoteAlignment) ? "LEFT" : "RIGHT";
        xOffset = Math.max(0, remoteXOffset);
        yOffset = Math.max(0, remoteYOffset);
        scale = Math.max(0.25D, Math.min(4D, remoteScale));
        lineSpacing = Math.max(0, remoteLineSpacing);
        horizontalPadding = Math.max(0, remoteHorizontalPadding);
        textShadow = remoteTextShadow;
        backgroundColor = remoteBackgroundColor;
        titleBackgroundColor = remoteTitleBackgroundColor;
    }

    private static void load() {
        if (file == null) return;

        Configuration config = new Configuration(file);
        try {
            config.load();
            enabled = config.getBoolean("enabled", GENERAL, true, "Enable the animated custom scoreboard overlay.");
            hideVanillaSidebar = config.getBoolean(
                    "hideVanillaSidebar",
                    GENERAL,
                    true,
                    "Hide the vanilla sidebar objective while this custom scoreboard is enabled.");
            hideInDebug = config.getBoolean(
                    "hideInDebug",
                    GENERAL,
                    true,
                    "Hide the custom scoreboard while the F3 debug screen is visible.");
            animationIntervalTicks = config.getInt(
                    "animationIntervalTicks",
                    GENERAL,
                    10,
                    1,
                    1200,
                    "Ticks between animation frames. 20 ticks = one second.");

            titleFrames = config.getStringList(
                    "titleFrames",
                    CONTENT,
                    new String[] { "&6&lServer Utilities", "&e&lServer Utilities" },
                    "Animated title frames. Each list entry is one frame.");
            lines = config.getStringList(
                    "lines",
                    CONTENT,
                    new String[] { "&7&m--------------------", "&e玩家: &f{player}", "&e在线: &a{online}&7/&f{max_players}",
                            "&e世界: &f{world}", "&e坐标: &f{x}, {y}, {z}", "&e延迟: &f{ping}ms", "&b欢迎游玩!||&d祝你游戏愉快!",
                            "&7&m--------------------" },
                    "Scoreboard lines. Use || between frames to animate one line. Supported placeholders: "
                            + "{player}, {display_name}, {online}, {max_players}, {ping}, {x}, {y}, {z}, "
                            + "{dimension}, {world}, {health}, {max_health}, {food}, {xp_level}, {team}, "
                            + "{real_time}, {date}, {year}, {month}, {day}, {hour}, {minute}, {second}, "
                            + "{world_time}, {fps}.");

            alignment = config.getString("alignment", APPEARANCE, "RIGHT", "Horizontal alignment: RIGHT or LEFT.")
                    .toUpperCase();
            if (!alignment.equals("LEFT")) alignment = "RIGHT";
            xOffset = config.getInt("xOffset", APPEARANCE, 5, 0, 10_000, "Distance from the selected screen edge.");
            yOffset = config.getInt("yOffset", APPEARANCE, 30, 0, 10_000, "Distance from the top of the screen.");
            scale = config.getFloat("scale", APPEARANCE, 1.0F, 0.25F, 4.0F, "Scale of the whole scoreboard.");
            lineSpacing = config.getInt("lineSpacing", APPEARANCE, 1, 0, 20, "Extra pixels between text lines.");
            horizontalPadding = config
                    .getInt("horizontalPadding", APPEARANCE, 4, 0, 40, "Horizontal padding around text.");
            textShadow = config.getBoolean("textShadow", APPEARANCE, true, "Draw a shadow behind scoreboard text.");
            backgroundColor = parseColor(
                    config.getString(
                            "backgroundColor",
                            APPEARANCE,
                            "90000000",
                            "ARGB hexadecimal color for line backgrounds."),
                    0x90000000);
            titleBackgroundColor = parseColor(
                    config.getString(
                            "titleBackgroundColor",
                            APPEARANCE,
                            "B0000000",
                            "ARGB hexadecimal color for the title background."),
                    0xB0000000);
        } catch (RuntimeException e) {
            ServerUtilities.LOGGER.error("Failed to load animated scoreboard config {}", file, e);
        } finally {
            if (config.hasChanged()) config.save();
            lastModified = file.lastModified();
        }
    }

    static File resolveConfigFile(File configDirectory) {
        File absoluteConfigDirectory = configDirectory.getAbsoluteFile();
        return new File(new File(absoluteConfigDirectory.getParentFile(), ServerUtilities.MOD_ID), "scoreboard.cfg");
    }

    static int parseColor(String value, int fallback) {
        try {
            String color = value.trim();
            if (color.startsWith("#")) color = color.substring(1);
            if (color.startsWith("0x") || color.startsWith("0X")) color = color.substring(2);
            return (int) Long.parseLong(color, 16);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
