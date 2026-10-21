package fr.koriamc.quest.hook;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.api.KoriaFactionsAPI;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.model.Relation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Seule classe qui interroge KoriaFactions : faction d'un joueur, membres,
 * territoire et points de faction. Les factions sont désignées par leur identifiant
 * (stable, contrairement au nom).
 */
public final class FactionHook {

    /** Relation d'une victime sans faction (cible de TUER_JOUEUR). */
    public static final String NO_FACTION = "NO_FACTION";

    public String getFactionId(Player player) {
        Faction f = KoriaFactionsAPI.getFaction(player);
        return f == null || !f.isNormal() ? null : f.getId();
    }

    /** Faction d'un joueur, connecté ou non. */
    public String getFactionId(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return getFactionId(online);
        }
        KoriaFactions kf = KoriaFactions.get();
        FPlayer fp = kf == null ? null : kf.players().get(uuid);
        Faction f = fp == null ? null : kf.factions().getFaction(fp);
        return f == null || !f.isNormal() ? null : f.getId();
    }

    public Faction getFaction(String id) {
        Faction f = id == null ? null : KoriaFactionsAPI.getFactionById(id);
        return f == null || !f.isNormal() ? null : f;
    }

    public Faction getByTag(String tag) {
        Faction f = KoriaFactionsAPI.getFactionByTag(tag);
        return f == null || !f.isNormal() ? null : f;
    }

    public String getTag(String id) {
        Faction f = getFaction(id);
        return f == null ? "?" : f.getTag();
    }

    public List<String> getTags() {
        List<String> tags = new ArrayList<String>();
        for (Faction f : KoriaFactionsAPI.getFactions()) {
            tags.add(f.getTag());
        }
        return tags;
    }

    public int getSize(String id) {
        Faction f = getFaction(id);
        return f == null ? 0 : f.getSize();
    }

    public Set<UUID> getMembers(String id) {
        Faction f = getFaction(id);
        return f == null ? Collections.<UUID>emptySet() : f.getMemberIds();
    }

    public List<Player> getOnlineMembers(String id) {
        List<Player> list = new ArrayList<Player>();
        if (id == null) {
            return list;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (id.equals(getFactionId(p))) {
                list.add(p);
            }
        }
        return list;
    }

    public long getPoints(String id) {
        return KoriaFactionsAPI.getPoints(getFaction(id));
    }

    /** @return les points réellement crédités */
    public long addPoints(String id, long amount) {
        Faction f = getFaction(id);
        return f == null || amount == 0 ? 0 : KoriaFactionsAPI.addPoints(f, amount, "koriaquest");
    }

    /** Type de territoire d'une position vue par le joueur (WILDERNESS, SAFEZONE, WARZONE, OWN, ALLY, ENEMY). */
    public String getZone(Player player, Location location) {
        return KoriaFactionsAPI.getTerritoryType(player, location).name();
    }

    /** Relation du tueur envers sa victime : ENEMY, ALLY, MEMBER ou NO_FACTION. */
    public String getVictimRelation(Player killer, Player victim) {
        if (getFactionId(victim) == null) {
            return NO_FACTION;
        }
        Relation relation = KoriaFactionsAPI.getRelation(killer, victim);
        return relation == null ? Relation.ENEMY.name() : relation.name();
    }
}
