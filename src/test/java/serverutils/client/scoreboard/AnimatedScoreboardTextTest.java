package serverutils.client.scoreboard;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Calendar;

import org.junit.Test;

public class AnimatedScoreboardTextTest {

    @Test
    public void selectsDelimitedLineFramesAtConfiguredInterval() {
        assertEquals("first", AnimatedScoreboardText.selectFrame("first||second||third", 0, 10));
        assertEquals("first", AnimatedScoreboardText.selectFrame("first||second||third", 9, 10));
        assertEquals("second", AnimatedScoreboardText.selectFrame("first||second||third", 10, 10));
        assertEquals("third", AnimatedScoreboardText.selectFrame("first||second||third", 20, 10));
        assertEquals("first", AnimatedScoreboardText.selectFrame("first||second||third", 30, 10));
    }

    @Test
    public void selectsTitleFramesAndHandlesInvalidInterval() {
        String[] frames = { "one", "two" };
        assertEquals("one", AnimatedScoreboardText.selectFrame(frames, 0, 0));
        assertEquals("two", AnimatedScoreboardText.selectFrame(frames, 1, 0));
        assertEquals("", AnimatedScoreboardText.selectFrame(new String[0], 1, 10));
    }

    @Test
    public void convertsAmpersandFormattingCodes() {
        assertEquals("\u00a7aHello \u00a7lWorld", AnimatedScoreboardText.formatColors("&aHello &lWorld"));
    }

    @Test
    public void parsesArgbColors() {
        assertEquals(0x90000000, AnimatedScoreboardConfig.parseColor("#90000000", 0));
        assertEquals(0xFFFFFFFF, AnimatedScoreboardConfig.parseColor("0xFFFFFFFF", 0));
        assertEquals(123, AnimatedScoreboardConfig.parseColor("invalid", 123));
    }

    @Test
    public void replacesCombinedAndSeparateDateTimePlaceholders() {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.SEPTEMBER, 6, 7, 8, 9);

        assertEquals(
                "2026-09-06 07:08:09 | 2026 09 06 07 08 09",
                AnimatedScoreboardText.replaceDateTimePlaceholders(
                        "{date} {real_time} | {year} {month} {day} {hour} {minute} {second}",
                        calendar.getTime()));
    }

    @Test
    public void placesScoreboardConfigBesideTheMainServerUtilitiesConfig() {
        File gameDirectory = new File("test-instance").getAbsoluteFile();
        File forgeConfigDirectory = new File(gameDirectory, "config");

        assertEquals(
                new File(new File(gameDirectory, "serverutilities"), "scoreboard.cfg"),
                AnimatedScoreboardConfig.resolveConfigFile(forgeConfigDirectory));
    }
}
