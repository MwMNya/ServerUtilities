package serverutils.client.scoreboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

@EventBusSubscriber(side = Side.CLIENT)
public final class AnimatedScoreboardRenderer {

    private static boolean hiddenByPlayer;

    private AnimatedScoreboardRenderer() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!AnimatedScoreboardConfig.enabled || hiddenByPlayer || mc.theWorld == null || mc.thePlayer == null) return;
        if (AnimatedScoreboardConfig.hideInDebug && mc.gameSettings.showDebugInfo) return;

        render(event.resolution, mc);
    }

    public static boolean shouldHideVanillaSidebar() {
        return AnimatedScoreboardConfig.enabled && AnimatedScoreboardConfig.hideVanillaSidebar;
    }

    public static boolean togglePlayerVisibility() {
        hiddenByPlayer = !hiddenByPlayer;
        return !hiddenByPlayer;
    }

    private static void render(ScaledResolution resolution, Minecraft mc) {
        FontRenderer font = mc.fontRenderer;
        long ticks = mc.theWorld.getTotalWorldTime();
        int interval = AnimatedScoreboardConfig.animationIntervalTicks;
        String title = resolve(
                AnimatedScoreboardText.selectFrame(AnimatedScoreboardConfig.titleFrames, ticks, interval),
                mc);
        List<String> renderedLines = new ArrayList<>();
        for (String configuredLine : AnimatedScoreboardConfig.lines) {
            renderedLines.add(resolve(AnimatedScoreboardText.selectFrame(configuredLine, ticks, interval), mc));
        }

        int contentWidth = font.getStringWidth(title);
        for (String line : renderedLines) contentWidth = Math.max(contentWidth, font.getStringWidth(line));
        int width = contentWidth + AnimatedScoreboardConfig.horizontalPadding * 2;
        int lineHeight = font.FONT_HEIGHT + AnimatedScoreboardConfig.lineSpacing;
        int scaledScreenWidth = (int) (resolution.getScaledWidth() / AnimatedScoreboardConfig.scale);
        int x = AnimatedScoreboardConfig.alignment.equals("LEFT") ? AnimatedScoreboardConfig.xOffset
                : scaledScreenWidth - AnimatedScoreboardConfig.xOffset - width;
        int y = AnimatedScoreboardConfig.yOffset;

        GL11.glPushMatrix();
        GL11.glScalef(
                (float) AnimatedScoreboardConfig.scale,
                (float) AnimatedScoreboardConfig.scale,
                (float) AnimatedScoreboardConfig.scale);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);

        if (!title.isEmpty()) {
            Gui.drawRect(x, y, x + width, y + lineHeight, AnimatedScoreboardConfig.titleBackgroundColor);
            drawString(font, title, x + (width - font.getStringWidth(title)) / 2, y + 1);
            y += lineHeight;
        }

        for (String line : renderedLines) {
            Gui.drawRect(x, y, x + width, y + lineHeight, AnimatedScoreboardConfig.backgroundColor);
            drawString(font, line, x + AnimatedScoreboardConfig.horizontalPadding, y + 1);
            y += lineHeight;
        }

        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    private static void drawString(FontRenderer font, String text, int x, int y) {
        if (AnimatedScoreboardConfig.textShadow) {
            font.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
        } else {
            font.drawString(text, x, y, 0xFFFFFFFF);
        }
    }

    private static String resolve(String text, Minecraft mc) {
        int online = mc.thePlayer.sendQueue.playerInfoList.size();
        int maxPlayers = mc.thePlayer.sendQueue.currentServerMaxPlayers;
        int ping = findPing(mc);
        int x = MathHelper.floor_double(mc.thePlayer.posX);
        int y = MathHelper.floor_double(mc.thePlayer.posY);
        int z = MathHelper.floor_double(mc.thePlayer.posZ);
        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(mc.thePlayer.getCommandSenderName());
        Date now = new Date();

        String result = text.replace("{player}", mc.thePlayer.getCommandSenderName())
                .replace("{display_name}", mc.thePlayer.getDisplayName()).replace("{online}", Integer.toString(online))
                .replace("{max_players}", Integer.toString(maxPlayers))
                .replace("{ping}", ping < 0 ? "?" : Integer.toString(ping)).replace("{x}", Integer.toString(x))
                .replace("{y}", Integer.toString(y)).replace("{z}", Integer.toString(z))
                .replace("{dimension}", mc.theWorld.provider.getDimensionName())
                .replace("{world}", mc.theWorld.getWorldInfo().getWorldName())
                .replace("{health}", trimFloat(mc.thePlayer.getHealth()))
                .replace("{max_health}", trimFloat(mc.thePlayer.getMaxHealth()))
                .replace("{food}", Integer.toString(mc.thePlayer.getFoodStats().getFoodLevel()))
                .replace("{xp_level}", Integer.toString(mc.thePlayer.experienceLevel))
                .replace("{team}", team == null ? "" : team.func_96669_c())
                .replace("{world_time}", Long.toString(mc.theWorld.getWorldTime())).replace("{fps}", getDebugFps(mc));
        return AnimatedScoreboardText.formatColors(AnimatedScoreboardText.replaceDateTimePlaceholders(result, now));
    }

    private static int findPing(Minecraft mc) {
        @SuppressWarnings("unchecked")
        List<GuiPlayerInfo> players = mc.thePlayer.sendQueue.playerInfoList;
        for (GuiPlayerInfo player : players) {
            if (player.name.equals(mc.thePlayer.getCommandSenderName())) return player.responseTime;
        }
        return -1;
    }

    private static String trimFloat(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format("%.1f", value);
    }

    private static String getDebugFps(Minecraft mc) {
        if (mc.debug == null) return "?";
        int separator = mc.debug.indexOf(' ');
        return separator <= 0 ? "?" : mc.debug.substring(0, separator);
    }
}
