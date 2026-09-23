package serverutils.task;

import static serverutils.ServerUtilitiesConfig.tasks;
import static serverutils.ServerUtilitiesConfig.world;
import static serverutils.ServerUtilitiesNotifications.RESOURCE_WORLD_CLEANUP;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesLoadedChunkManager;
import serverutils.lib.data.Universe;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.math.Ticks;
import serverutils.pregenerator.RTPPreGenManager;

public class ResourceWorldCleanupTask extends Task {

    private static final long FINALIZE_DELAY_MILLIS = Ticks.SECOND.x(2).millis();
    private static final int MAX_UNLOAD_ATTEMPTS = 30;

    public ResourceWorldCleanupTask() {
        super(Ticks.HOUR.x(Math.max(1D, tasks.resource_world_cleanup.interval_hours)));
    }

    @Override
    public void execute(Universe universe) {
        Set<Integer> dimensions = configuredDimensions();
        try {
            for (int dimensionId : dimensions) {
                validateDimension(dimensionId);
            }
            evacuatePlayers(universe.server, dimensions);

            for (int dimensionId : dimensions) {
                ServerUtilitiesLoadedChunkManager.INSTANCE.releaseDimension(dimensionId);
                RTPPreGenManager.clearDimensionState(dimensionId);
                if (DimensionManager.getWorld(dimensionId) != null) {
                    DimensionManager.unloadWorld(dimensionId);
                }
            }

            universe.scheduleTask(
                    new FinalizeResetTask(System.currentTimeMillis() + FINALIZE_DELAY_MILLIS, dimensions, 1));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Override
    protected List<NotifyTask> getNotifications() {
        List<NotifyTask> notifications = new ArrayList<>();
        if (!tasks.resource_world_cleanup.notifications) return notifications;

        addWarning(notifications, 300);
        addWarning(notifications, 60);
        addWarning(notifications, 30);
        addWarning(notifications, 10);
        addWarning(notifications, 5);
        addWarning(notifications, 4);
        addWarning(notifications, 3);
        addWarning(notifications, 2);
        addWarning(notifications, 1);
        return notifications;
    }

    private void addWarning(List<NotifyTask> notifications, int seconds) {
        long warningTime = nextTime - Ticks.SECOND.x(seconds).millis();
        if (warningTime > System.currentTimeMillis()) {
            notifications.add(
                    new NotifyTask(
                            warningTime,
                            RESOURCE_WORLD_CLEANUP.createNotification(
                                    "serverutilities.task.resource_world_cleanup_warning",
                                    seconds)));
        }
    }

    private static Set<Integer> configuredDimensions() {
        Set<Integer> dimensions = new LinkedHashSet<>();
        for (int dimensionId : tasks.resource_world_cleanup.dimension_ids) {
            dimensions.add(dimensionId);
        }
        return dimensions;
    }

    private static void validateDimension(int dimensionId) {
        if (dimensionId == world.spawn_dimension) {
            throw new IllegalStateException("Refusing to reset protected dimension " + dimensionId);
        }
        if (!DimensionManager.isDimensionRegistered(dimensionId)) {
            throw new IllegalStateException("Resource dimension " + dimensionId + " is not registered");
        }
    }

    private static void evacuatePlayers(MinecraftServer server, Set<Integer> dimensions) {
        WorldServer spawnWorld = server.worldServerForDimension(world.spawn_dimension);
        ChunkCoordinates spawn = spawnWorld.getSpawnPoint();
        int y = spawn.posY;
        while (y < 254 && (!spawnWorld.isAirBlock(spawn.posX, y, spawn.posZ)
                || !spawnWorld.isAirBlock(spawn.posX, y + 1, spawn.posZ))) {
            y++;
        }

        TeleporterDimPos destination = TeleporterDimPos
                .of(spawn.posX + 0.5D, y + 0.1D, spawn.posZ + 0.5D, world.spawn_dimension);
        for (EntityPlayerMP player : new ArrayList<EntityPlayerMP>(server.getConfigurationManager().playerEntityList)) {
            if (dimensions.contains(player.dimension)) {
                destination.teleport(player);
            }
        }
    }

    private static Path getRegionDirectory(int dimensionId) throws IOException {
        File saveRootFile = DimensionManager.getCurrentSaveRootDirectory();
        if (saveRootFile == null) throw new IOException("World save directory is unavailable");

        WorldProvider provider = DimensionManager.createProviderFor(dimensionId);
        String saveFolder = provider.getSaveFolder();
        if (saveFolder == null || saveFolder.trim().isEmpty()) {
            throw new IOException("Dimension " + dimensionId + " has no separate save folder");
        }

        Path saveRoot = saveRootFile.toPath().toAbsolutePath().normalize();
        Path dimensionFolder = saveRoot.resolve(saveFolder).normalize();
        Path regionFolder = dimensionFolder.resolve("region").normalize();
        if (!dimensionFolder.startsWith(saveRoot) || dimensionFolder.equals(saveRoot)
                || !regionFolder.startsWith(dimensionFolder)) {
            throw new IOException("Unsafe resource-world path: " + regionFolder);
        }
        return regionFolder;
    }

    private static void resetRegionDirectory(int dimensionId) throws IOException {
        Path regionFolder = getRegionDirectory(dimensionId);
        if (!Files.exists(regionFolder)) return;

        Path retired = regionFolder.resolveSibling("region.serverutilities-reset-" + System.currentTimeMillis());
        try {
            Files.move(regionFolder, retired, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(regionFolder, retired);
        }

        Thread deleteThread = new Thread(() -> deleteTree(retired), "ServerUtilities resource-world cleanup");
        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    private static void deleteTree(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) throw exc;
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            ServerUtilities.LOGGER.error("Failed to delete retired resource-world region directory {}", root, e);
        }
    }

    private static void fail(Exception e) {
        ServerUtilities.LOGGER.error("Resource-world cleanup failed", e);
        if (tasks.resource_world_cleanup.notifications) {
            RESOURCE_WORLD_CLEANUP.sendAll("serverutilities.task.resource_world_cleanup_failed", e.getMessage());
        }
    }

    private static class FinalizeResetTask extends Task {

        private final Set<Integer> dimensions;
        private final int attempt;

        private FinalizeResetTask(long whenToRun, Set<Integer> dimensions, int attempt) {
            super(whenToRun);
            this.dimensions = new LinkedHashSet<>(dimensions);
            this.attempt = attempt;
        }

        @Override
        public void execute(Universe universe) {
            try {
                evacuatePlayers(universe.server, dimensions);
                for (int dimensionId : dimensions) {
                    if (DimensionManager.getWorld(dimensionId) != null) {
                        if (attempt >= MAX_UNLOAD_ATTEMPTS) {
                            throw new IOException("Dimension " + dimensionId + " did not unload within 60 seconds");
                        }
                        DimensionManager.unloadWorld(dimensionId);
                        universe.scheduleTask(
                                new FinalizeResetTask(
                                        System.currentTimeMillis() + FINALIZE_DELAY_MILLIS,
                                        dimensions,
                                        attempt + 1));
                        return;
                    }
                }

                for (int dimensionId : dimensions) {
                    resetRegionDirectory(dimensionId);
                }
                if (tasks.resource_world_cleanup.notifications) {
                    RESOURCE_WORLD_CLEANUP.sendAll("serverutilities.task.resource_world_cleanup_complete");
                }
                ServerUtilities.LOGGER.info("Reset resource-world chunks for dimensions {}", dimensions);
            } catch (Exception e) {
                fail(e);
            }
        }
    }
}
