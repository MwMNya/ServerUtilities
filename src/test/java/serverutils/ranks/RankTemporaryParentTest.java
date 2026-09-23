package serverutils.ranks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import serverutils.lib.data.Universe;

public class RankTemporaryParentTest {

    @Test
    public void activeTemporaryParentGrantsInheritedPermissions() {
        Ranks ranks = new Ranks(mock(Universe.class));
        Rank vip = new Rank(ranks, "vip");
        Rank target = new Rank(ranks, "event_member");
        vip.add();
        target.add();
        vip.setPermission("example.permission", true);

        long now = System.currentTimeMillis();
        assertTrue(target.addTemporaryParent(vip, now - 1_000L, now + 60_000L));
        assertEquals("true", target.getPermission("example.permission"));
    }

    @Test
    public void expiredTemporaryParentDoesNotGrantPermissions() {
        Ranks ranks = new Ranks(mock(Universe.class));
        Rank vip = new Rank(ranks, "vip");
        Rank target = new Rank(ranks, "event_member");
        vip.add();
        target.add();
        vip.setPermission("example.permission", true);

        long now = System.currentTimeMillis();
        assertTrue(target.addTemporaryParent(vip, now - 60_000L, now - 1L));
        assertEquals("", target.getPermission("example.permission"));
    }

    @Test
    public void permanentGrantReplacesTemporaryGrantAndRemoveClearsBothKinds() {
        Ranks ranks = new Ranks(mock(Universe.class));
        Rank vip = new Rank(ranks, "vip");
        Rank target = new Rank(ranks, "event_member");
        vip.add();
        target.add();

        long now = System.currentTimeMillis();
        assertTrue(target.addTemporaryParent(vip, now, now + 60_000L));
        assertTrue(target.addParent(vip));
        assertTrue(target.temporaryParents.isEmpty());
        assertTrue(target.removeParent(vip));
        assertFalse(target.getParents().contains(vip));
    }
}
