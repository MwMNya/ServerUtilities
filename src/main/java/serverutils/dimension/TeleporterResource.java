package serverutils.dimension;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class TeleporterResource extends Teleporter {

    public TeleporterResource(WorldServer worldServer) {
        super(worldServer);
    }

    @Override
    public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {

        entity.setPosition(x, y, z);

    }
}
