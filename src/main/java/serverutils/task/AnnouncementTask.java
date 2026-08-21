package serverutils.task;

import static serverutils.ServerUtilitiesConfig.tasks;
import static serverutils.ServerUtilitiesNotifications.ANNOUNCEMENT;

import java.util.ArrayList;
import java.util.List;

import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;

public class AnnouncementTask extends Task {

    private int announcementIndex;

    public AnnouncementTask() {
        super(Ticks.MINUTE.x(tasks.announcement.interval));
    }

    @Override
    public void execute(Universe universe) {
        if (!tasks.announcement.enabled) {
            return;
        }

        List<String[]> announcements = new ArrayList<>();

        announcements.add(tasks.announcement.announcement1);
        announcements.add(tasks.announcement.announcement2);
        announcements.add(tasks.announcement.announcement3);
        announcements.add(tasks.announcement.announcement4);
        announcements.add(tasks.announcement.announcement5);
        announcements.add(tasks.announcement.announcement6);
        announcements.add(tasks.announcement.announcement7);

        String[] announcement = announcements.get(announcementIndex);

        if (announcement != null) {
            for (String text : announcement) {
                if (text == null || text.isEmpty()) {
                    continue;
                }

                ANNOUNCEMENT.createNotification(text).sendToAll();
            }
        }

        announcementIndex++;

        if (announcementIndex >= announcements.size()) {
            announcementIndex = 0;
        }
    }
}
