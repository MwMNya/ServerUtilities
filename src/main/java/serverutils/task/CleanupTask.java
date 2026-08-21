package serverutils.task;

import static serverutils.ServerUtilitiesConfig.tasks;
import static serverutils.ServerUtilitiesNotifications.CLEANUP;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.INpc;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;

public class CleanupTask extends Task {

    public CleanupTask() {
        super(Ticks.MINUTE.x(tasks.cleanup.interval));
    }

    @Override
    public void execute(Universe universe) {
        int removed = 0;
        for (World world : universe.server.worldServers) {
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (shouldDespawn(entity)) {
                    entity.setDead();
                    removed++;
                }
            }
        }

        CLEANUP.sendAll("serverutilities.task.cleanup_removed", removed);
    }

    @Override
    public List<NotifyTask> getNotifications() {
        List<NotifyTask> notifications = new ArrayList<>();
        if (tasks.cleanup.silent) return notifications;
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(1).millis(),
                        CLEANUP.createNotification(getNotificationString(1))));
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(2).millis(),
                        CLEANUP.createNotification(getNotificationString(2))));
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(3).millis(),
                        CLEANUP.createNotification(getNotificationString(3))));
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(4).millis(),
                        CLEANUP.createNotification(getNotificationString(4))));
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(5).millis(),
                        CLEANUP.createNotification(getNotificationString(5))));
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(30).millis(),
                        CLEANUP.createNotification(getNotificationString(30))));
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(60).millis(),
                        CLEANUP.createNotification(getNotificationString(60))));
        return notifications;
    }

    private IChatComponent getNotificationString(int seconds) {
        ServerUtilitiesConfig.Tasks.Cleanup config = tasks.cleanup;

        List<IChatComponent> components = new ArrayList<>();

        if (config.hostiles) {
            components.add(new ChatComponentTranslation("serverutilities.task.cleanup_hostiles"));
        }

        if (config.passives) {
            components.add(new ChatComponentTranslation("serverutilities.task.cleanup_passives"));
        }

        if (config.items) {
            components.add(new ChatComponentTranslation("serverutilities.task.cleanup_items"));
        }

        if (config.experience) {
            components.add(new ChatComponentTranslation("serverutilities.task.cleanup_experience"));
        }

        ChatComponentText entityText = new ChatComponentText("");

        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                if (i == components.size() - 1) {
                    entityText.appendText(" & ");
                } else {
                    entityText.appendText(", ");
                }
            }

            entityText.appendSibling(components.get(i));
        }

        return StringUtils.color(
                new ChatComponentTranslation("serverutilities.task.cleanup_entity", entityText, seconds),
                EnumChatFormatting.LIGHT_PURPLE);
    }

    private static boolean shouldDespawn(Entity entity) {
        ServerUtilitiesConfig.Tasks.Cleanup config = tasks.cleanup;
        if (entity instanceof EntityLiving living && living.isNoDespawnRequired()) {
            return false;
        }

        if ((entity instanceof IAnimals && !(entity instanceof IMob)) || entity instanceof INpc) {
            return config.passives;
        }
        if (entity instanceof IMob) {
            return config.hostiles;
        }
        if (entity instanceof EntityItem) {
            return config.items;
        }
        return config.experience && entity instanceof EntityXPOrb;
    }
}
