package serverutils.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.client.ClientUtils;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.TextBox;
import serverutils.lib.gui.TextField;
import serverutils.lib.gui.Widget;
import serverutils.lib.gui.WidgetType;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.misc.MouseButton;

public class GuiConvenientCommands extends GuiButtonListBase {

    private static final class Argument {

        private final String labelKey;
        private final boolean required;
        private final boolean player;

        private Argument(String id, boolean isRequired, boolean isPlayer) {
            labelKey = "serverutilities.convenient_commands.argument." + id;
            required = isRequired;
            player = isPlayer;
        }
    }

    private static final class CommandEntry {

        private final String titleKey;
        private final String command;
        private final String usage;
        private final boolean executeImmediately;
        private final Icon icon;
        private final Argument[] arguments;

        private CommandEntry(String title, String cmd, String commandUsage, boolean execute, Icon commandIcon,
                Argument... commandArguments) {
            titleKey = title;
            command = cmd;
            usage = commandUsage;
            executeImmediately = execute;
            icon = commandIcon;
            arguments = commandArguments;
        }
    }

    private static final CommandEntry[] COMMANDS = {
            entry("tpa", "/tpa", "/tpa <玩家>", false, GuiIcons.PLAYER, playerArg("target_player")),
            entry("tpaccept", "/tpaccept", "/tpaccept <玩家>", false, GuiIcons.ACCEPT, playerArg("request_player")),
            entry("spawn", "/spawn", "/spawn", true, GuiIcons.MARKER),
            entry("back", "/back", "/back", true, GuiIcons.BACK), entry("end", "/end", "/end", true, GuiIcons.GLOBE),
            entry("setspawn", "/setspawn", "/setspawn", true, GuiIcons.BED),
            entry("claim_tp", "/chunks tp", "/chunks tp", true, GuiIcons.MAP),
            entry("home_default", "/home", "/home", true, GuiIcons.MARKER),
            entry("home_named", "/home", "/home <家名称>", false, GuiIcons.MARKER, arg("home_name")),
            entry(
                    "home_other",
                    "/home",
                    "/home <家名称> <玩家>",
                    false,
                    GuiIcons.MARKER,
                    arg("home_name"),
                    playerArg("player")),
            entry("home_list", "/home list", "/home list", true, GuiIcons.NOTES),
            entry("home_list_other", "/home list", "/home list <玩家>", false, GuiIcons.NOTES, playerArg("player")),
            entry("home_list_all", "/home list_all", "/home list_all", true, GuiIcons.NOTES),
            entry("sethome_default", "/sethome", "/sethome", true, GuiIcons.ADD),
            entry("sethome_named", "/sethome", "/sethome <家名称>", false, GuiIcons.ADD, arg("home_name")),
            entry("delhome_default", "/delhome", "/delhome", false, GuiIcons.REMOVE),
            entry("delhome_named", "/delhome", "/delhome <家名称>", false, GuiIcons.REMOVE, arg("home_name")),
            entry("rtp_overworld", "/rtp overworld", "/rtp overworld", true, GuiIcons.GLOBE),
            entry("rtp_nether", "/rtp nether", "/rtp nether", true, GuiIcons.GLOBE),
            entry("rtp_res", "/rtp res", "/rtp res", true, GuiIcons.DIAMOND),
            entry("rtp_cave", "/rtp cave", "/rtp cave", true, GuiIcons.DIAMOND),
            entry("rtp_tf", "/rtp tf", "/rtp tf", true, GuiIcons.GLOBE) };

    public GuiConvenientCommands() {
        setTitle(I18n.format("serverutilities.convenient_commands.title"));
        setHasSearchBox(true);
    }

    private static Argument arg(String id) {
        return new Argument(id, true, false);
    }

    private static Argument playerArg(String id) {
        return new Argument(id, true, true);
    }

    private static CommandEntry entry(String id, String command, String usage, boolean execute, Icon icon,
            Argument... arguments) {
        return new CommandEntry("serverutilities.convenient_commands." + id, command, usage, execute, icon, arguments);
    }

    @Override
    public void addButtons(Panel panel) {
        for (CommandEntry entry : COMMANDS) {
            panel.add(new CommandButton(panel, entry));
        }
    }

    private static final class CommandButton extends SimpleTextButton {

        private final CommandEntry entry;

        private CommandButton(Panel panel, CommandEntry commandEntry) {
            super(
                    panel,
                    I18n.format(commandEntry.titleKey) + EnumChatFormatting.GRAY + "  " + commandEntry.usage,
                    commandEntry.icon);
            entry = commandEntry;
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            if (entry.executeImmediately) {
                getGui().closeGui();
                ClientUtils.execClientCommand(entry.command, true);
            } else {
                new GuiCommandArguments(entry).openGui();
            }
        }

        @Override
        public void addMouseOverText(List<String> list) {
            super.addMouseOverText(list);
            list.add(EnumChatFormatting.YELLOW + I18n.format("serverutilities.convenient_commands.usage", entry.usage));
            list.add(
                    EnumChatFormatting.DARK_GRAY + I18n.format(
                            entry.executeImmediately ? "serverutilities.convenient_commands.click_execute"
                                    : "serverutilities.convenient_commands.click_configure"));
        }
    }

    private static final class GuiCommandArguments extends GuiBase {

        private final CommandEntry entry;
        private final TextBox[] inputs;
        private final List<Widget> widgets;
        private final Button executeButton;
        private final Button cancelButton;

        private GuiCommandArguments(CommandEntry commandEntry) {
            entry = commandEntry;
            inputs = new TextBox[entry.arguments.length];
            widgets = new ArrayList<>();
            setSize(300, 66 + entry.arguments.length * 20);

            TextField title = new TextField(this).setMaxWidth(width - 16).setText(I18n.format(entry.titleKey));
            title.setPos(8, 7);
            widgets.add(title);

            TextField usage = new TextField(this).setMaxWidth(width - 16).setText(
                    EnumChatFormatting.GRAY + I18n.format("serverutilities.convenient_commands.usage", entry.usage));
            usage.setPos(8, 19);
            widgets.add(usage);

            for (int i = 0; i < entry.arguments.length; i++) {
                Argument argument = entry.arguments[i];
                int y = 34 + i * 20;
                TextField label = new TextField(this).setText(I18n.format(argument.labelKey));
                label.setPos(8, y + 4);
                widgets.add(label);

                TextBox input = new TextBox(this) {

                    @Override
                    public void onEnterPressed() {
                        execute();
                    }
                };
                input.setText("", false);
                input.ghostText = I18n.format(argument.labelKey);
                input.setPosAndSize(92, y, argument.player ? 142 : 200, 16);
                inputs[i] = input;
                widgets.add(input);

                if (argument.player) {
                    Button selectPlayer = new SimpleTextButton(
                            this,
                            I18n.format("serverutilities.convenient_commands.select_player"),
                            GuiIcons.PLAYER) {

                        @Override
                        public void onClicked(MouseButton button) {
                            GuiHelper.playClickSound();
                            new GuiCommandPlayerSelector(input).openGui();
                        }

                        @Override
                        public boolean renderTitleInCenter() {
                            return true;
                        }
                    };
                    selectPlayer.setPosAndSize(238, y, 54, 16);
                    widgets.add(selectPlayer);
                }
            }

            int buttonWidth = width / 2 - 10;
            cancelButton = new SimpleTextButton(this, I18n.format("gui.cancel"), Icon.EMPTY) {

                @Override
                public void onClicked(MouseButton button) {
                    GuiHelper.playClickSound();
                    closeGui(true);
                }

                @Override
                public boolean renderTitleInCenter() {
                    return true;
                }
            };
            cancelButton.setPosAndSize(8, height - 24, buttonWidth, 16);

            executeButton = new SimpleTextButton(
                    this,
                    I18n.format("serverutilities.convenient_commands.execute"),
                    GuiIcons.ACCEPT) {

                @Override
                public void onClicked(MouseButton button) {
                    GuiHelper.playClickSound();
                    execute();
                }

                @Override
                public WidgetType getWidgetType() {
                    return buildCommand() == null ? WidgetType.DISABLED : super.getWidgetType();
                }

                @Override
                public boolean renderTitleInCenter() {
                    return true;
                }
            };
            executeButton.setPosAndSize(width - buttonWidth - 8, height - 24, buttonWidth, 16);
        }

        private String buildCommand() {
            StringBuilder command = new StringBuilder(entry.command);
            boolean skippedOptional = false;

            for (int i = 0; i < entry.arguments.length; i++) {
                String value = inputs[i].getText().trim();
                if (value.isEmpty()) {
                    if (entry.arguments[i].required) return null;
                    skippedOptional = true;
                } else {
                    if (skippedOptional) return null;
                    command.append(' ').append(value);
                }
            }

            return command.toString();
        }

        private void execute() {
            String command = buildCommand();
            if (command != null) {
                closeGui(false);
                ClientUtils.execClientCommand(command, true);
            }
        }

        @Override
        public void addWidgets() {
            addAll(widgets);
            add(cancelButton);
            add(executeButton);
        }
    }

    private static final class GuiCommandPlayerSelector extends GuiButtonListBase {

        private final TextBox target;

        private GuiCommandPlayerSelector(TextBox targetBox) {
            target = targetBox;
            setTitle(I18n.format("serverutilities.convenient_commands.select_player_title"));
            setHasSearchBox(true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void addButtons(Panel panel) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.thePlayer == null || minecraft.thePlayer.sendQueue == null) return;

            List<GuiPlayerInfo> players = new ArrayList<>(minecraft.thePlayer.sendQueue.playerInfoList);
            players.sort(Comparator.comparing(player -> player.name.toLowerCase(Locale.ROOT)));
            for (GuiPlayerInfo player : players) {
                panel.add(new SimpleTextButton(panel, player.name, GuiIcons.PLAYER) {

                    @Override
                    public void onClicked(MouseButton button) {
                        GuiHelper.playClickSound();
                        target.setText(player.name);
                        target.setCursorPosition(player.name.length());
                        closeGui(true);
                    }
                });
            }
        }
    }
}
