package serverutils.client.gui;

import net.minecraft.client.resources.I18n;

import serverutils.client.integration.PlayerMenuIntegration;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageMyTeamGui;

public class GuiPlayerMenu extends GuiButtonListBase {

    public GuiPlayerMenu() {
        setTitle(I18n.format("serverutilities.player_menu.title"));
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new SimpleTextButton(panel, I18n.format("serverutilities.player_menu.my_team"), GuiIcons.FRIENDS) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                new MessageMyTeamGui().sendToServer();
            }
        });

        panel.add(new SimpleTextButton(panel, I18n.format("serverutilities.player_menu.claims"), GuiIcons.MAP) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                GuiClaimedChunks.instance = new GuiClaimedChunks();
                GuiClaimedChunks.instance.openGui();
            }
        });

        panel.add(
                new SimpleTextButton(
                        panel,
                        I18n.format("serverutilities.player_menu.convenient_commands"),
                        GuiIcons.CONTROLLER) {

                    @Override
                    public void onClicked(MouseButton button) {
                        GuiHelper.playClickSound();
                        new GuiConvenientCommands().openGui();
                    }
                });

        panel.add(new SimpleTextButton(panel, I18n.format("serverutilities.player_menu.quests"), GuiIcons.BOOK_RED) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                PlayerMenuIntegration.openBetterQuesting();
            }
        });
    }
}
