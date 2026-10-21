package fr.koriamc.quest.listener;

import fr.koriamc.factions.api.event.LandClaimEvent;
import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.objective.ObjectiveRegistry;
import fr.koriamc.quest.objective.ProgressContext;
import fr.koriamc.quest.progress.AntiAbuse;
import fr.koriamc.quest.progress.ProgressService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Traduit les évènements Bukkit en actions de quête. Chaque gestionnaire ne fait
 * que construire un {@link ProgressContext} : c'est ProgressService qui décide
 * des quêtes concernées. Les évènements annulés (protection, claims) ne comptent pas.
 */
@SuppressWarnings("deprecation")
public final class ObjectiveListener implements Listener {

    /** Plantes : posées en graine, elles doivent pouvoir compter à la récolte. */
    private static final Set<Material> PLANTS = EnumSet.of(Material.CROPS, Material.CARROT, Material.POTATO,
            Material.NETHER_WARTS, Material.COCOA, Material.SUGAR_CANE_BLOCK, Material.CACTUS, Material.MELON_STEM,
            Material.PUMPKIN_STEM, Material.SAPLING);

    private final KoriaQuest plugin;
    private final AntiAbuse anti;
    private BukkitTask playtimeTask;

    public ObjectiveListener(KoriaQuest plugin) {
        this.plugin = plugin;
        this.anti = plugin.antiAbuse();
    }

    private ProgressService progress() {
        return plugin.progress();
    }

    private boolean tracked(String type) {
        return plugin.catalog().usesType(type);
    }

    /** Temps de jeu : une minute comptée à chaque joueur actif. */
    public void start() {
        stop();
        playtimeTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!tracked(ObjectiveRegistry.PLAYTIME)) {
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!anti.isAfk(p)) {
                        progress().handle(ProgressContext.of(p, ObjectiveRegistry.PLAYTIME, 1));
                    }
                }
            }
        }, 1200L, 1200L);
    }

    public void stop() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
            playtimeTask = null;
        }
    }

    // ================================================================= Blocs

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        boolean wasPlaced = anti.consumePlaced(block);
        if (wasPlaced) {
            return;
        }
        progress().handle(ProgressContext.at(player, ObjectiveRegistry.BREAK_BLOCK, 1, block.getLocation())
                .material(block.getTypeId(), block.getData()));
        if (isHarvest(block)) {
            progress().handle(ProgressContext.at(player, ObjectiveRegistry.HARVEST, 1, block.getLocation())
                    .material(block.getTypeId(), block.getData()));
        }
    }

    /** Culture mûre, melon / citrouille poussé, canne ou cactus au-dessus de sa base. */
    private boolean isHarvest(Block block) {
        int data = block.getData();
        switch (block.getType()) {
            case CROPS:
            case CARROT:
            case POTATO:
                return data >= 7;
            case NETHER_WARTS:
                return data >= 3;
            case COCOA:
                return (data >> 2) >= 2;
            case MELON_BLOCK:
            case PUMPKIN:
                return true;
            case SUGAR_CANE_BLOCK:
            case CACTUS:
                return block.getRelative(BlockFace.DOWN).getType() == block.getType();
            default:
                return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!PLANTS.contains(block.getType())) {
            anti.markPlaced(block);
        }
        progress().handle(ProgressContext.at(event.getPlayer(), ObjectiveRegistry.PLACE_BLOCK, 1, block.getLocation())
                .material(block.getTypeId(), block.getData()));
    }

    // =============================================================== Combat

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        if (dead instanceof Player) {
            Player victim = (Player) dead;
            if (tracked(ObjectiveRegistry.KILL_PLAYER) && anti.allowKill(killer, victim)) {
                progress().handle(ProgressContext.at(killer, ObjectiveRegistry.KILL_PLAYER, 1, victim.getLocation())
                        .key(plugin.factionHook().getVictimRelation(killer, victim)));
            }
            return;
        }
        if (anti.isIgnoredMob(dead)) {
            return;
        }
        progress().handle(ProgressContext.at(killer, ObjectiveRegistry.KILL_MOB, 1, dead.getLocation())
                .key(dead.getType().name()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!tracked(ObjectiveRegistry.DEAL_DAMAGE) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            attacker = (Player) ((Projectile) event.getDamager()).getShooter();
        }
        Entity victim = event.getEntity();
        if (attacker == null || attacker.equals(victim) || anti.isIgnoredMob(victim)) {
            return;
        }
        double health = ((LivingEntity) victim).getHealth();
        long amount = Math.round(Math.min(event.getFinalDamage(), health));
        if (amount > 0) {
            progress().handle(ProgressContext.at(attacker, ObjectiveRegistry.DEAL_DAMAGE, amount, victim.getLocation())
                    .key(victim.getType().name()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        switch (event.getSpawnReason()) {
            case SPAWNER:
                if (plugin.settings().ignoreSpawnerMobs) {
                    anti.ignoreMob(event.getEntity());
                }
                break;
            case SPAWNER_EGG:
                if (plugin.settings().ignoreEggMobs) {
                    anti.ignoreMob(event.getEntity());
                }
                break;
            case BREEDING:
                if (!tracked(ObjectiveRegistry.BREED)) {
                    return;
                }
                UUID breeder = anti.findBreeder(event.getEntity());
                Player player = breeder == null ? null : Bukkit.getPlayer(breeder);
                if (player != null) {
                    progress().handle(ProgressContext.at(player, ObjectiveRegistry.BREED, 1, event.getLocation())
                            .key(event.getEntityType().name()));
                }
                break;
            default:
                break;
        }
    }

    // ============================================================= Animaux

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFeed(PlayerInteractEntityEvent event) {
        ItemStack hand = event.getPlayer().getItemInHand();
        if (event.getRightClicked() instanceof Animals && hand != null && hand.getType() != Material.AIR
                && tracked(ObjectiveRegistry.BREED)) {
            anti.recordFeed(event.getRightClicked(), event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player) {
            progress().handle(ProgressContext.at((Player) event.getOwner(), ObjectiveRegistry.TAME, 1,
                    event.getEntity().getLocation()).key(event.getEntityType().name()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        progress().handle(ProgressContext.at(event.getPlayer(), ObjectiveRegistry.SHEAR, 1, event.getEntity().getLocation())
                .key(event.getEntity().getType().name()));
    }

    // =========================================================== Artisanat

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        ProgressContext ctx = ProgressContext.of(event.getPlayer(), ObjectiveRegistry.FISH, 1);
        if (event.getCaught() instanceof Item) {
            ctx.item(((Item) event.getCaught()).getItemStack());
        }
        progress().handle(ctx);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getAction() == InventoryAction.NOTHING
                || !tracked(ObjectiveRegistry.CRAFT)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        int times = 1;
        if (event.isShiftClick()) {
            times = Integer.MAX_VALUE;
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    times = Math.min(times, ingredient.getAmount());
                }
            }
            if (times == Integer.MAX_VALUE) {
                times = 1;
            }
        }
        progress().handle(ProgressContext.of((Player) event.getWhoClicked(), ObjectiveRegistry.CRAFT,
                (long) result.getAmount() * times).item(result));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSmelt(FurnaceExtractEvent event) {
        if (event.getItemAmount() > 0) {
            progress().handle(ProgressContext.at(event.getPlayer(), ObjectiveRegistry.SMELT, event.getItemAmount(),
                    event.getBlock().getLocation()).material(event.getItemType().getId(), 0));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        progress().handle(ProgressContext.at(event.getEnchanter(), ObjectiveRegistry.ENCHANT, 1,
                event.getEnchantBlock().getLocation()).item(event.getItem()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpenStand(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof BrewingStand && event.getPlayer() instanceof Player
                && tracked(ObjectiveRegistry.BREW)) {
            anti.recordBrewer(((BrewingStand) event.getInventory().getHolder()).getBlock(), (Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!tracked(ObjectiveRegistry.BREW)) {
            return;
        }
        UUID brewer = anti.getBrewer(event.getBlock());
        final Player player = brewer == null ? null : Bukkit.getPlayer(brewer);
        if (player == null) {
            return;
        }
        final BrewerInventory inventory = event.getContents();
        final Location location = event.getBlock().getLocation();
        // les potions obtenues ne sont dans l'alambic qu'après l'évènement
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (int slot = 0; slot < 3; slot++) {
                    ItemStack potion = inventory.getItem(slot);
                    if (potion != null && potion.getType() == Material.POTION) {
                        progress().handle(ProgressContext.at(player, ObjectiveRegistry.BREW, 1, location).item(potion));
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        progress().handle(ProgressContext.of(event.getPlayer(), ObjectiveRegistry.CONSUME, 1).item(event.getItem()));
    }

    // ========================================================== Expérience

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExp(PlayerExpChangeEvent event) {
        if (event.getAmount() > 0) {
            progress().handle(ProgressContext.of(event.getPlayer(), ObjectiveRegistry.GAIN_XP, event.getAmount()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent event) {
        int gained = event.getNewLevel() - event.getOldLevel();
        if (gained > 0) {
            progress().handle(ProgressContext.of(event.getPlayer(), ObjectiveRegistry.LEVEL_UP, gained));
        }
    }

    // ===================================================== Déplacement / AFK

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        Player player = event.getPlayer();
        if (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {
            anti.activity(player);
        }
        if (!tracked(ObjectiveRegistry.TRAVEL) || from.getWorld() != to.getWorld() || player.isFlying()
                || (player.isInsideVehicle() && !plugin.settings().travelInVehicle)) {
            return;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double distance = Math.min(5, Math.sqrt(dx * dx + dz * dz));
        if (distance <= 0) {
            return;
        }
        long blocks = anti.addDistance(player, distance);
        if (blocks > 0) {
            progress().handle(ProgressContext.of(player, ObjectiveRegistry.TRAVEL, blocks));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        anti.activity(event.getPlayer());
    }

    // ============================================================ Factions

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaim(LandClaimEvent event) {
        if (event.getPlayer() != null) {
            progress().handle(ProgressContext.of(event.getPlayer(), ObjectiveRegistry.CLAIM, 1));
        }
    }
}
