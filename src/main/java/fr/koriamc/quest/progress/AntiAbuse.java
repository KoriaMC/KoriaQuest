package fr.koriamc.quest.progress;

import fr.koriamc.factions.manager.UpgradeManager;
import fr.koriamc.quest.KoriaQuest;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protections contre la triche aux quêtes : blocs posés puis recassés, mobs de
 * spawner, kills répétés sur la même victime, joueurs AFK. Garde aussi l'état
 * nécessaire à certains objectifs (éleveur, alambic, distance parcourue).
 */
public final class AntiAbuse {

    private static final String MOB_META = "koriaquest-ignore";
    private static final long FEED_MS = 60000L;

    private final KoriaQuest plugin;
    /** Blocs posés par des joueurs (monde, x, y, z compactés), du plus ancien au plus récent. */
    private final LinkedHashSet<Long> placed = new LinkedHashSet<Long>();
    private final List<String> worlds = new ArrayList<String>();
    private final Map<String, Long> kills = new HashMap<String, Long>();
    private final Map<UUID, UUID> feeders = new HashMap<UUID, UUID>();
    private final Map<UUID, Long> feedTimes = new HashMap<UUID, Long>();
    private final Map<String, UUID> brewers = new HashMap<String, UUID>();
    /** Modifié aussi depuis le chat (thread asynchrone). */
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Double> distances = new HashMap<UUID, Double>();

    public AntiAbuse(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    // ========================================================= Blocs posés

    private long key(Block block) {
        String world = block.getWorld().getName();
        int index = worlds.indexOf(world);
        if (index < 0) {
            worlds.add(world);
            index = worlds.size() - 1;
        }
        return ((long) (index & 0xF) << 60) | (((long) block.getX() & 0x3FFFFFFL) << 34)
                | (((long) block.getZ() & 0x3FFFFFFL) << 8) | (block.getY() & 0xFF);
    }

    public void markPlaced(Block block) {
        if (!plugin.settings().ignorePlacedBlocks || worlds.size() >= 16 && !worlds.contains(block.getWorld().getName())) {
            return;
        }
        placed.add(key(block));
        int max = plugin.settings().placedBlocksMax;
        if (placed.size() > max) {
            Iterator<Long> it = placed.iterator();
            while (placed.size() > max && it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    /** @return true si le bloc avait été posé par un joueur (il est alors oublié) */
    public boolean consumePlaced(Block block) {
        return plugin.settings().ignorePlacedBlocks && worlds.contains(block.getWorld().getName()) && placed.remove(key(block));
    }

    public boolean isPlaced(Block block) {
        return plugin.settings().ignorePlacedBlocks && worlds.contains(block.getWorld().getName()) && placed.contains(key(block));
    }

    /** Recharge les blocs posés enregistrés à l'arrêt précédent. */
    public void loadPlaced(File file) {
        if (!file.exists()) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int worldCount = in.readInt();
            for (int i = 0; i < worldCount; i++) {
                worlds.add(in.readUTF());
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                placed.add(in.readLong());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Blocs posés illisibles (" + file.getName() + ") : " + e.getMessage());
        }
    }

    public void savePlaced(File file) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(worlds.size());
            for (String world : worlds) {
                out.writeUTF(world);
            }
            out.writeInt(placed.size());
            for (Long key : placed) {
                out.writeLong(key);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer les blocs posés : " + e.getMessage());
        }
    }

    // ================================================================= Mobs

    /** Marque un mob qui ne doit pas compter (spawner, œuf). */
    public void ignoreMob(Entity entity) {
        entity.setMetadata(MOB_META, new FixedMetadataValue(plugin, true));
    }

    /** Mob marqué, ou mob supplémentaire de l'amélioration SPAWNERS de KoriaFactions (apparu sans raison SPAWNER). */
    public boolean isIgnoredMob(Entity entity) {
        return entity.hasMetadata(MOB_META)
                || (plugin.settings().ignoreSpawnerMobs && entity.hasMetadata(UpgradeManager.SPAWNER_METADATA));
    }

    // ================================================================ Kills

    /** Un kill compte-t-il ? (même IP, même victime trop récente) */
    public boolean allowKill(Player killer, Player victim) {
        if (killer.equals(victim)) {
            return false;
        }
        if (plugin.settings().ignoreSameIp && sameIp(killer, victim)) {
            return false;
        }
        int minutes = plugin.settings().sameVictimMinutes;
        if (minutes <= 0) {
            return true;
        }
        String key = killer.getUniqueId() + ":" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = kills.get(key);
        if (last != null && now - last < minutes * 60000L) {
            return false;
        }
        kills.put(key, now);
        if (kills.size() > 5000) {
            kills.values().removeIf(t -> now - t > minutes * 60000L);
        }
        return true;
    }

    private static boolean sameIp(Player a, Player b) {
        InetSocketAddress ia = a.getAddress();
        InetSocketAddress ib = b.getAddress();
        return ia != null && ib != null && ia.getAddress() != null && ia.getAddress().equals(ib.getAddress());
    }

    // ============================================================== Élevage

    public void recordFeed(Entity animal, Player player) {
        feeders.put(animal.getUniqueId(), player.getUniqueId());
        feedTimes.put(animal.getUniqueId(), System.currentTimeMillis());
    }

    /** Joueur ayant nourri récemment un animal proche du nouveau-né. */
    public UUID findBreeder(Entity baby) {
        long now = System.currentTimeMillis();
        feedTimes.values().removeIf(t -> now - t > FEED_MS);
        feeders.keySet().retainAll(feedTimes.keySet());
        for (Entity nearby : baby.getNearbyEntities(4, 2, 4)) {
            if (nearby.getType() == baby.getType() && feeders.containsKey(nearby.getUniqueId())) {
                UUID player = feeders.remove(nearby.getUniqueId());
                feedTimes.remove(nearby.getUniqueId());
                return player;
            }
        }
        return null;
    }

    // ============================================================= Alambics

    private static String locationKey(Location l) {
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    public void recordBrewer(Block stand, Player player) {
        brewers.put(locationKey(stand.getLocation()), player.getUniqueId());
    }

    public UUID getBrewer(Block stand) {
        return brewers.get(locationKey(stand.getLocation()));
    }

    // ======================================================== Activité / AFK

    public void activity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isAfk(Player player) {
        int minutes = plugin.settings().afkMinutes;
        if (minutes <= 0) {
            return false;
        }
        Long last = lastActivity.get(player.getUniqueId());
        return last == null || System.currentTimeMillis() - last > minutes * 60000L;
    }

    /** Ajoute une distance et renvoie le nombre de blocs entiers à compter. */
    public long addDistance(Player player, double distance) {
        Double current = distances.get(player.getUniqueId());
        double total = (current == null ? 0 : current) + distance;
        long whole = (long) Math.floor(total);
        distances.put(player.getUniqueId(), total - whole);
        return whole;
    }

    public void forget(UUID uuid) {
        lastActivity.remove(uuid);
        distances.remove(uuid);
    }
}
