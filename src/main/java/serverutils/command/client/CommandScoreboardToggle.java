package serverutils.command.client;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.client.scoreboard.AnimatedScoreboardRenderer;
import serverutils.lib.command.CmdBase;

public class CommandScoreboardToggle extends CmdBase {

    public CommandScoreboardToggle() {
        super("bc", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        boolean visible = AnimatedScoreboardRenderer.togglePlayerVisibility();
        sender.addChatMessage(ServerUtilities.lang(sender, visible ? "commands.bc.shown" : "commands.bc.hidden"));
    }
}
