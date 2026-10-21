package fr.koriamc.quest.objective;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Une action d'un joueur susceptible de faire avancer des quêtes :
 * type d'objectif, quantité, lieu et élément concerné (matériau ou nom).
 */
public final class ProgressContext {

    private final Player player;
    private final String type;
    private final long amount;
    private final Location location;
    private int materialId = -1;
    private int materialData = -1;
    private String key;

    private ProgressContext(Player player, String type, long amount, Location location) {
        this.player = player;
        this.type = type.toUpperCase(Locale.ROOT);
        this.amount = amount;
        this.location = location;
    }

    /** Action à la position du joueur. */
    public static ProgressContext of(Player player, String type, long amount) {
        return new ProgressContext(player, type, amount, player.getLocation());
    }

    /** Action à une position précise (bloc cassé, entité tuée...). */
    public static ProgressContext at(Player player, String type, long amount, Location location) {
        return new ProgressContext(player, type, amount, location == null ? player.getLocation() : location);
    }

    public ProgressContext material(int id, int data) {
        this.materialId = id;
        this.materialData = data;
        return this;
    }

    @SuppressWarnings("deprecation")
    public ProgressContext item(ItemStack item) {
        if (item != null) {
            material(item.getTypeId(), item.getDurability());
        }
        return this;
    }

    public ProgressContext key(String key) {
        this.key = key;
        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public String getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public Location getLocation() {
        return location;
    }

    public int getMaterialId() {
        return materialId;
    }

    public int getMaterialData() {
        return materialData;
    }

    public String getKey() {
        return key;
    }
}
