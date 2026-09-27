package serverutils.client.scoreboard;

import java.util.Calendar;
import java.util.Date;

import serverutils.lib.util.StringUtils;

public final class AnimatedScoreboardText {

    private AnimatedScoreboardText() {}

    public static String selectFrame(String value, long worldTicks, int intervalTicks) {
        if (value == null || value.isEmpty()) return "";
        String[] frames = value.split("\\|\\|", -1);
        int interval = Math.max(1, intervalTicks);
        int index = (int) Math.floorMod(worldTicks / interval, frames.length);
        return frames[index];
    }

    public static String selectFrame(String[] frames, long worldTicks, int intervalTicks) {
        if (frames == null || frames.length == 0) return "";
        int interval = Math.max(1, intervalTicks);
        int index = (int) Math.floorMod(worldTicks / interval, frames.length);
        return frames[index] == null ? "" : frames[index];
    }

    public static String formatColors(String value) {
        return StringUtils.addFormatting(value == null ? "" : value);
    }

    public static String replaceDateTimePlaceholders(String value, Date now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        String year = Integer.toString(calendar.get(Calendar.YEAR));
        String month = twoDigits(calendar.get(Calendar.MONTH) + 1);
        String day = twoDigits(calendar.get(Calendar.DAY_OF_MONTH));
        String hour = twoDigits(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = twoDigits(calendar.get(Calendar.MINUTE));
        String second = twoDigits(calendar.get(Calendar.SECOND));
        return value.replace("{real_time}", hour + ":" + minute + ":" + second)
                .replace("{date}", year + "-" + month + "-" + day).replace("{year}", year).replace("{month}", month)
                .replace("{day}", day).replace("{hour}", hour).replace("{minute}", minute).replace("{second}", second);
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
