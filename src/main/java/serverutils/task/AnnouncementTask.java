package serverutils.task;

import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;

import static serverutils.ServerUtilitiesConfig.tasks;
import static serverutils.ServerUtilitiesNotifications.ANNOUNCEMENT;

public class AnnouncementTask extends Task{

    private int announcementIndex;

    public AnnouncementTask(){
        super(Ticks.MINUTE.x(tasks.announcement.interval));
    }

    @Override
    public void execute(Universe universe) {
        String[] announcements = tasks.announcement.announcements;

        if (!tasks.announcement.enabled || announcements == null|| announcements.length == 0) {
            return;
        }

        ANNOUNCEMENT.createNotification(announcements[announcementIndex]).sendToAll();

        announcementIndex++;

        if (announcementIndex >= announcements.length) {
            announcementIndex = 0;
        }
    }
}