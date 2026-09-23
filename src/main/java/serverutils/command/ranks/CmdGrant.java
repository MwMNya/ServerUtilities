package serverutils.command.ranks;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;
import serverutils.ranks.TemporaryGrantTime;

public class CmdGrant extends CmdBase {

    public CmdGrant() {
        super("grant", Level.OP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (!Ranks.isActive()) return super.addTabCompletionOptions(sender, args);

        if (args.length == 1 || args.length == 2) {
            List<String> list = new ArrayList<>();
            if (args.length == 1 && sender instanceof EntityPlayerMP player) {
                list.addAll(Arrays.asList(player.mcServer.getConfigurationManager().getAllUsernames()));
            }
            list.addAll(Ranks.INSTANCE.getRankNames(false));
            return getListOfStringsFromIterableMatchingLastWord(args, list);
        }

        if (args.length == 3) {
            return getListOfStringsMatchingLastWord(args, "30d", "month", "2026-09-01..2026-10-01");
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!Ranks.isActive()) throw ServerUtilities.errorFeatureDisabledServer(sender);

        checkArgs(sender, args, 3);
        Rank target = Ranks.INSTANCE.getRank(sender, args[0]);
        Rank parent = Ranks.INSTANCE.getRank(sender, args[1]);
        if (parent.isPlayer()) throw ServerUtilities.error(sender, "commands.ranks.grant.parent_is_player", args[1]);

        long now = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        Rank.TemporaryParent existingGrant = target.temporaryParents.get(parent.getId());
        TemporaryGrantTime.Range existingRange = existingGrant == null ? null
                : new TemporaryGrantTime.Range(existingGrant.validFrom, existingGrant.validUntil);
        TemporaryGrantTime.Range range = TemporaryGrantTime.parseForGrant(args[2], now, zone, existingRange);
        if (range == null) throw ServerUtilities.error(sender, "commands.ranks.grant.invalid_time", args[2]);

        if (!target.addTemporaryParent(parent, range.validFrom(), range.validUntil())) {
            sender.addChatMessage(ServerUtilities.lang(sender, "nothing_changed"));
            return;
        }

        target.ranks.temporaryGrantsChanged();
        target.ranks.save();
        sender.addChatMessage(
                ServerUtilities.lang(
                        sender,
                        "commands.ranks.grant.text",
                        parent.getDisplayName(),
                        target.getDisplayName(),
                        TemporaryGrantTime.format(range.validFrom(), zone),
                        TemporaryGrantTime.format(range.validUntil(), zone)));
    }
}
