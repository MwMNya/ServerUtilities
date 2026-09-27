package serverutils.client.scoreboard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AnimatedScoreboardRendererTest {

    @Test
    public void playerVisibilityToggleHidesAndRestoresTheScoreboard() {
        assertFalse(AnimatedScoreboardRenderer.togglePlayerVisibility());
        assertTrue(AnimatedScoreboardRenderer.togglePlayerVisibility());
    }
}
