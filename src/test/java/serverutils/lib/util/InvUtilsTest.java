package serverutils.lib.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import org.junit.Test;

public class InvUtilsTest {

    @Test
    public void forceUpdateSkipsFakePlayersWithoutAClientConnection() {
        EntityPlayerMP fakePlayer = mock(EntityPlayerMP.class);
        IInventory incompatibleInventory = mock(IInventory.class);
        fakePlayer.inventoryContainer = new Container() {

            {
                addSlotToContainer(new Slot(incompatibleInventory, 0, 0, 0));
            }

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        };

        InvUtils.forceUpdate(fakePlayer);

        verifyNoInteractions(incompatibleInventory);
    }
}
