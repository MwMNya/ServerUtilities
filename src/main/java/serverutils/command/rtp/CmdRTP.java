package serverutils.command.rtp;

import cpw.mods.fml.common.Loader;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;

public class CmdRTP extends CmdTreeBase {

    public CmdRTP() {
        super("rtp");
        addSubcommand(new CmdOverworld());
        addSubcommand(new CmdNether());
        if (ServerUtilitiesConfig.dimension.enableMiningDimension) {
            addSubcommand(new CmdRes());
            addSubcommand(new CmdCave());
        }
        if (Loader.isModLoaded("TwilightForest")) addSubcommand(new CmdTF());
        addSubcommand(new CmdTreeHelp(this));
    }
}
