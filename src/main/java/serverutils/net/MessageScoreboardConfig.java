package serverutils.net;

import java.util.Arrays;
import java.util.Collection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.scoreboard.AnimatedScoreboardConfig;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageScoreboardConfig extends MessageToClient {

    private boolean enabled;
    private boolean hideVanillaSidebar;
    private boolean hideInDebug;
    private int animationIntervalTicks;
    private String[] titleFrames;
    private String[] lines;
    private String alignment;
    private int xOffset;
    private int yOffset;
    private double scale;
    private int lineSpacing;
    private int horizontalPadding;
    private boolean textShadow;
    private int backgroundColor;
    private int titleBackgroundColor;

    public MessageScoreboardConfig() {}

    public static MessageScoreboardConfig fromCurrentConfig() {
        MessageScoreboardConfig message = new MessageScoreboardConfig();
        message.enabled = AnimatedScoreboardConfig.enabled;
        message.hideVanillaSidebar = AnimatedScoreboardConfig.hideVanillaSidebar;
        message.hideInDebug = AnimatedScoreboardConfig.hideInDebug;
        message.animationIntervalTicks = AnimatedScoreboardConfig.animationIntervalTicks;
        message.titleFrames = AnimatedScoreboardConfig.titleFrames.clone();
        message.lines = AnimatedScoreboardConfig.lines.clone();
        message.alignment = AnimatedScoreboardConfig.alignment;
        message.xOffset = AnimatedScoreboardConfig.xOffset;
        message.yOffset = AnimatedScoreboardConfig.yOffset;
        message.scale = AnimatedScoreboardConfig.scale;
        message.lineSpacing = AnimatedScoreboardConfig.lineSpacing;
        message.horizontalPadding = AnimatedScoreboardConfig.horizontalPadding;
        message.textShadow = AnimatedScoreboardConfig.textShadow;
        message.backgroundColor = AnimatedScoreboardConfig.backgroundColor;
        message.titleBackgroundColor = AnimatedScoreboardConfig.titleBackgroundColor;
        return message;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeBoolean(enabled);
        data.writeBoolean(hideVanillaSidebar);
        data.writeBoolean(hideInDebug);
        data.writeVarInt(animationIntervalTicks);
        data.writeCollection(Arrays.asList(titleFrames), DataOut.STRING);
        data.writeCollection(Arrays.asList(lines), DataOut.STRING);
        data.writeString(alignment);
        data.writeVarInt(xOffset);
        data.writeVarInt(yOffset);
        data.writeDouble(scale);
        data.writeVarInt(lineSpacing);
        data.writeVarInt(horizontalPadding);
        data.writeBoolean(textShadow);
        data.writeInt(backgroundColor);
        data.writeInt(titleBackgroundColor);
    }

    @Override
    public void readData(DataIn data) {
        enabled = data.readBoolean();
        hideVanillaSidebar = data.readBoolean();
        hideInDebug = data.readBoolean();
        animationIntervalTicks = data.readVarInt();
        titleFrames = toArray(data.readCollection(DataIn.STRING));
        lines = toArray(data.readCollection(DataIn.STRING));
        alignment = data.readString();
        xOffset = data.readVarInt();
        yOffset = data.readVarInt();
        scale = data.readDouble();
        lineSpacing = data.readVarInt();
        horizontalPadding = data.readVarInt();
        textShadow = data.readBoolean();
        backgroundColor = data.readInt();
        titleBackgroundColor = data.readInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        AnimatedScoreboardConfig.applyServerConfig(
                enabled,
                hideVanillaSidebar,
                hideInDebug,
                animationIntervalTicks,
                titleFrames,
                lines,
                alignment,
                xOffset,
                yOffset,
                scale,
                lineSpacing,
                horizontalPadding,
                textShadow,
                backgroundColor,
                titleBackgroundColor);
    }

    private static String[] toArray(Collection<String> values) {
        return values.toArray(new String[0]);
    }
}
