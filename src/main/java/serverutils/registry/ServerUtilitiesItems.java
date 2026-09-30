package serverutils.registry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import serverutils.ServerUtilities;

public final class ServerUtilitiesItems {

    public static final Item PLAYER_MENU = new Item() {

        @Override
        public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
            if (world.isRemote) {
                ServerUtilities.PROXY.openPlayerMenu();
            }
            return stack;
        }
    }.setUnlocalizedName("serverutilities.player_menu").setTextureName("minecraft:compass")
            .setCreativeTab(CreativeTabs.tabTools).setMaxStackSize(1);

    private ServerUtilitiesItems() {}

    public static void init() {
        GameRegistry.registerItem(PLAYER_MENU, "player_menu");
    }
}
