package serverutils.ranks;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.mojang.authlib.GameProfile;

import serverutils.lib.config.ConfigValue;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;

public class PlayerRank extends Rank {

    public final UUID uuid;
    public final GameProfile profile;
    private final Map<String, String> stringCache;
    private final Map<String, ConfigValue> valueCache;

    PlayerRank(Ranks r, UUID id, String name) {
        super(r, StringUtils.fromUUID(id));
        displayName = new ChatComponentText(name.isEmpty() ? getId() : name);
        displayName.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        uuid = id;
        comment = name;
        profile = new GameProfile(id, name.isEmpty() ? null : name);
        stringCache = new HashMap<>();
        valueCache = new HashMap<>();
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public boolean add() {
        return ranks.playerRanks.put(uuid, this) != this;
    }

    @Override
    public boolean remove() {
        if (!permissions.isEmpty() || !getParents().isEmpty()) {
            permissions.clear();
            clearParents();
            return true;
        }

        return false;
    }

    @Override
    public boolean isDefaultPlayerRank() {
        return false;
    }

    @Override
    public boolean isDefaultOPRank() {
        return false;
    }

    @Override
    public Set<Rank> getActualParents() {
        Set<Rank> parents = new LinkedHashSet<>(super.getActualParents());

        if (ServerUtils.isOP(ranks.universe.server, profile)) {
            Rank r = ranks.getDefaultOPRank();

            if (r != null) {
                parents.add(r);
            }
        }

        Rank r = ranks.getDefaultPlayerRank();

        if (r != null) {
            parents.add(r);
        }

        java.util.List<Rank> list = new java.util.ArrayList<>(parents);
        list.sort(null);
        return new LinkedHashSet<>(list);
    }

    @Override
    public String getPermission(String originalNode, String node, boolean recursive) {
        ranks.refreshTemporaryGrants();
        String s = stringCache.get(node);

        if (s != null) {
            return s;
        }

        s = super.getPermission(originalNode, node, recursive);
        stringCache.put(node, s);
        return s;
    }

    @Override
    public ConfigValue getPermissionValue(String originalNode, String node, boolean recursive) {
        ranks.refreshTemporaryGrants();
        ConfigValue v = valueCache.get(node);

        if (v == null) {
            v = super.getPermissionValue(originalNode, node, recursive);
            valueCache.put(node, v);
        }

        return v;
    }

    @Override
    public void clearCache() {
        super.clearCache();
        stringCache.clear();
        valueCache.clear();
    }
}
