package serverutils.client.integration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentTranslation;

import cpw.mods.fml.common.Loader;
import serverutils.ServerUtilities;

/** Opens optional client GUIs without making either mod a hard dependency. */
public final class PlayerMenuIntegration {

    private PlayerMenuIntegration() {}

    public static void openBetterQuesting() {
        if (!Loader.isModLoaded("betterquesting")) {
            showUnavailable("serverutilities.player_menu.quests_unavailable");
            return;
        }

        try {
            Class<?> settingsClass = Class.forName("betterquesting.api.storage.BQ_Settings");
            Class<?> homeClass = Class.forName("betterquesting.client.gui2.GuiHome");
            boolean useBookmark = settingsClass.getField("useBookmark").getBoolean(null);
            GuiScreen screen = useBookmark ? (GuiScreen) homeClass.getField("bookmark").get(null) : null;

            if (screen == null) {
                Class<?> registryClass = Class.forName("betterquesting.client.themes.ThemeRegistry");
                Class<?> presetClass = Class.forName("betterquesting.api2.client.gui.themes.presets.PresetGUIs");
                Class<?> argsClass = Class.forName("betterquesting.api2.client.gui.themes.gui_args.GArgsNone");
                Object registry = registryClass.getField("INSTANCE").get(null);
                Object home = presetClass.getField("HOME").get(null);
                Object noArgs = argsClass.getField("NONE").get(null);
                Method getGui = findMethod(registryClass, "getGui", 2);
                screen = (GuiScreen) getGui.invoke(registry, home, noArgs);

                if (useBookmark && settingsClass.getField("skipHome").getBoolean(null)) {
                    Class<?> questLinesClass = Class.forName("betterquesting.client.gui2.GuiQuestLines");
                    Constructor<?> constructor = questLinesClass.getConstructor(GuiScreen.class);
                    screen = (GuiScreen) constructor.newInstance(screen);
                }
            }

            Minecraft.getMinecraft().displayGuiScreen(screen);
        } catch (ReflectiveOperationException | LinkageError e) {
            ServerUtilities.LOGGER.error("Failed to open the installed BetterQuesting GUI", e);
            showUnavailable("serverutilities.player_menu.quests_failed");
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + '.' + name);
    }

    private static void showUnavailable(String key) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentTranslation(key));
        }
    }
}
