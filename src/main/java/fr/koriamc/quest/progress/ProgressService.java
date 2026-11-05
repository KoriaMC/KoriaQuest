package fr.koriamc.quest.progress;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.api.event.BonusEarnedEvent;
import fr.koriamc.quest.api.event.QuestCompleteEvent;
import fr.koriamc.quest.api.event.QuestProgressEvent;
import fr.koriamc.quest.config.RewardSettings;
import fr.koriamc.quest.config.Settings;
import fr.koriamc.quest.hook.FactionHook;
import fr.koriamc.quest.model.FactionData;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestProgress;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.objective.ObjectiveType;
import fr.koriamc.quest.objective.ProgressContext;
import fr.koriamc.quest.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fait avancer les quêtes du joueur et de sa faction à partir des actions
 * signalées ({@link ProgressContext}), puis gère réussites, récompenses et paliers.
 */
public final class ProgressService {

    private final KoriaQuest plugin;

    public ProgressService(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    // ============================================================ Actions

    /** Signale une action ; ne fait rien si aucune quête ne l'utilise. */
    public void handle(ProgressContext ctx) {
        if (ctx.getAmount() <= 0 || !plugin.catalog().usesType(ctx.getType())) {
            return;
        }
        Player player = ctx.getPlayer();
        Settings s = plugin.settings();
        if (player == null || !player.isOnline() || s.isWorldDisabled(player.getWorld().getName())
                || (s.ignoreCreative && player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }
        ObjectiveType type = plugin.objectives().get(ctx.getType());
        if (type == null || type.isDelivery()) {
            return;
        }
        String[] zone = new String[1];
        if (s.playerEnabled) {
            PlayerData pd = plugin.quests().player(player);
            for (QuestProgress q : pd.getQuests()) {
                QuestDefinition def = plugin.catalog().get(QuestScope.JOUEUR, q.getQuest());
                if (def != null && !q.isDone() && def.type.equals(ctx.getType()) && accepts(def, type, ctx, zone)) {
                    advancePlayer(player, pd, q, def, ctx.getAmount());
                }
            }
        }
        String factionId = plugin.factionHook().getFactionId(player);
        if (plugin.quests().isEligible(factionId)) {
            FactionData fd = plugin.quests().faction(factionId);
            for (QuestProgress q : fd.getQuests()) {
                QuestDefinition def = plugin.catalog().get(QuestScope.FACTION, q.getQuest());
                if (def != null && !q.isDone() && def.type.equals(ctx.getType()) && accepts(def, type, ctx, zone)) {
                    advanceFaction(player, factionId, fd, q, def, ctx.getAmount());
                }
            }
        }
    }

    /** Filtres communs (mondes, zones) puis cibles du type. */
    private boolean accepts(QuestDefinition def, ObjectiveType type, ProgressContext ctx, String[] zone) {
        if (!def.worlds.isEmpty() && (ctx.getLocation() == null
                || !def.worlds.contains(ctx.getLocation().getWorld().getName().toLowerCase(Locale.ROOT)))) {
            return false;
        }
        if (!def.tools.isEmpty()) {
            org.bukkit.inventory.ItemStack hand = ctx.getPlayer() == null ? null : ctx.getPlayer().getItemInHand();
            if (hand == null || !def.tools.contains(hand.getTypeId(), Math.max(0, (int) hand.getDurability()))) {
                return false;
            }
        }
        if (!def.zones.isEmpty()) {
            if (zone[0] == null) {
                zone[0] = plugin.factionHook().getZone(ctx.getPlayer(), ctx.getLocation());
            }
            if (!def.zones.contains(zone[0])) {
                return false;
            }
        }
        return type.matches(def, ctx);
    }

    private void advancePlayer(Player player, PlayerData pd, QuestProgress q, QuestDefinition def, long amount) {
        QuestProgressEvent event = new QuestProgressEvent(player, def, null, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getAmount() <= 0) {
            return;
        }
        long before = q.getProgress();
        if (q.add(event.getAmount()) <= 0) {
            return;
        }
        if (q.isDone()) {
            completePlayer(player, pd, q, def);
        } else {
            notifyProgress(Collections.singletonList(player), player, def, q, before);
        }
    }

    private void advanceFaction(Player player, String factionId, FactionData fd, QuestProgress q, QuestDefinition def, long amount) {
        QuestProgressEvent event = new QuestProgressEvent(player, def, factionId, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getAmount() <= 0) {
            return;
        }
        long before = q.getProgress();
        long added = q.add(event.getAmount());
        if (added <= 0) {
            return;
        }
        q.contribute(player.getUniqueId().toString(), added);
        if (q.isDone()) {
            completeFaction(player, factionId, fd, q, def);
        } else {
            notifyProgress(plugin.factionHook().getOnlineMembers(factionId), player, def, q, before);
        }
    }

    /** Barre d'action pour l'auteur, message aux paliers de pourcentage pour les concernés. */
    private void notifyProgress(List<Player> audience, Player actor, QuestDefinition def, QuestProgress q, long before) {
        Settings s = plugin.settings();
        Object[] vars = plugin.view().vars(def, q);
        if (s.progressActionBar) {
            plugin.msg().send(actor, "quest.progress", vars);
        }
        Sounds.play(actor, s.sound("progression"));
        for (int pct : s.milestones) {
            long threshold = (long) Math.ceil(q.getTarget() * pct / 100.0);
            if (before < threshold && q.getProgress() >= threshold) {
                Object[] milestone = with(vars, "pourcentage", pct);
                for (Player p : audience) {
                    plugin.msg().send(p, "quest.milestone", milestone);
                    Sounds.play(p, s.sound("palier"));
                }
                break;
            }
        }
    }

    // =========================================================== Livraison

    /**
     * Livraison des objets de l'inventaire pour une quête LIVRER (clic dans le menu).
     *
     * @return la quantité livrée
     */
    @SuppressWarnings("deprecation")
    public long deliver(Player player, QuestScope scope, QuestProgress q) {
        QuestDefinition def = plugin.catalog().get(scope, q.getQuest());
        ObjectiveType type = def == null ? null : plugin.objectives().get(def.type);
        if (type == null || !type.isDelivery() || q.isDone()) {
            return 0;
        }
        String factionId = plugin.factionHook().getFactionId(player);
        if (scope == QuestScope.FACTION && !plugin.quests().isEligible(factionId)) {
            return 0;
        }
        long wanted = q.getTarget() - q.getProgress();
        QuestProgressEvent event = new QuestProgressEvent(player, def, scope == QuestScope.FACTION ? factionId : null, wanted);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }
        wanted = Math.min(wanted, event.getAmount());
        PlayerInventory inv = player.getInventory();
        long taken = 0;
        for (int slot = 0; slot < inv.getSize() && taken < wanted; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().getId() == 0 || !matchesDelivery(def, item)) {
                continue;
            }
            int use = (int) Math.min(item.getAmount(), wanted - taken);
            taken += use;
            if (use >= item.getAmount()) {
                inv.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - use);
            }
        }
        if (taken <= 0) {
            plugin.msg().send(player, "quest.deliver-none");
            Sounds.play(player, plugin.settings().sound("erreur"));
            return 0;
        }
        player.updateInventory();
        long before = q.getProgress();
        q.add(taken);
        plugin.msg().send(player, "quest.deliver-done", "quantite", taken, "quete", def.name);
        if (scope == QuestScope.FACTION) {
            q.contribute(player.getUniqueId().toString(), taken);
            FactionData fd = plugin.quests().faction(factionId);
            if (q.isDone()) {
                completeFaction(player, factionId, fd, q, def);
            } else {
                notifyProgress(plugin.factionHook().getOnlineMembers(factionId), player, def, q, before);
            }
        } else {
            PlayerData pd = plugin.quests().player(player);
            if (q.isDone()) {
                completePlayer(player, pd, q, def);
            } else {
                notifyProgress(Collections.singletonList(player), player, def, q, before);
            }
        }
        return taken;
    }

    @SuppressWarnings("deprecation")
    private static boolean matchesDelivery(QuestDefinition def, ItemStack item) {
        if (item.hasItemMeta() && (item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore())) {
            return false;
        }
        return def.materials.isEmpty() || def.materials.contains(item.getTypeId(), item.getDurability());
    }

    // ======================================================= Administration

    /** Fixe l'avancement d'une quête joueur (la quête est validée si l'objectif est atteint). */
    public void setPlayerProgress(PlayerData pd, QuestProgress q, long value) {
        QuestDefinition def = plugin.catalog().get(QuestScope.JOUEUR, q.getQuest());
        boolean wasDone = q.isDone();
        q.setProgress(value);
        if (def != null && !wasDone && q.isDone()) {
            completePlayer(Bukkit.getPlayer(pd.getUniqueId()), pd, q, def);
        }
    }

    public void setFactionProgress(String factionId, FactionData fd, QuestProgress q, long value) {
        QuestDefinition def = plugin.catalog().get(QuestScope.FACTION, q.getQuest());
        boolean wasDone = q.isDone();
        q.setProgress(value);
        if (def != null && !wasDone && q.isDone()) {
            completeFaction(null, factionId, fd, q, def);
        }
    }

    // ============================================================ Réussite

    /** @param player joueur connecté, ou null (validation par un administrateur hors ligne) */
    private void completePlayer(Player player, PlayerData pd, QuestProgress q, QuestDefinition def) {
        Settings s = plugin.settings();
        q.setCompletedAt(System.currentTimeMillis());
        UUID uuid = pd.getUniqueId();
        String factionId = plugin.factionHook().getFactionId(uuid);
        List<String> rewards = plugin.view().rewardActions(def);
        Object[] vars = with(plugin.view().vars(def, q), "faction", factionId == null ? "" : plugin.factionHook().getTag(factionId),
                "joueur", pd.getName(), "player", pd.getName());
        long points = plugin.rewardService().grant(factionId, Collections.singleton(uuid), rewards, vars);
        if (player != null) {
            plugin.msg().send(player, "quest.complete", with(vars, "points", points));
            for (String line : plugin.view().rewards(def)) {
                plugin.msg().send(player, "quest.complete-rewards", "recompense", line);
            }
            if (factionId == null && plugin.rewardService().points(rewards) > 0) {
                plugin.msg().send(player, "quest.no-faction-points");
            }
            Sounds.play(player, s.sound("quete-terminee"));
        }
        Bukkit.getPluginManager().callEvent(new QuestCompleteEvent(player, def, factionId, points));

        int done = pd.countDone();
        for (Map.Entry<Integer, RewardSettings.Block> tier : plugin.rewards().playerTiers(pd.getDifficulty()).entrySet()) {
            if (tier.getKey() > done || pd.getTiers().contains(tier.getKey())) {
                continue;
            }
            pd.getTiers().add(tier.getKey());
            Object[] bonusVars = with(vars, "terminees", done, "total", pd.getQuests().size(),
                    "difficulte", s.difficultyName(pd.getDifficulty()));
            long bonusPoints = plugin.rewardService().grant(factionId, Collections.singleton(uuid), tier.getValue().rewards, bonusVars);
            if (player != null) {
                plugin.msg().send(player, "bonus.player", with(bonusVars, "points", bonusPoints));
                Sounds.play(player, s.sound("bonus"));
            }
            Bukkit.getPluginManager().callEvent(new BonusEarnedEvent(QuestScope.JOUEUR, uuid, factionId, tier.getKey(), bonusPoints));
        }
    }

    /** @param trigger joueur dont l'action a terminé la quête, ou null */
    private void completeFaction(Player trigger, String factionId, FactionData fd, QuestProgress q, QuestDefinition def) {
        Settings s = plugin.settings();
        FactionHook hook = plugin.factionHook();
        q.setCompletedAt(System.currentTimeMillis());
        String tag = hook.getTag(factionId);
        List<String> rewards = plugin.view().rewardActions(def);
        RewardSettings.Target target = plugin.rewards().factionPerQuest(def.difficulty).target;
        Object[] vars = with(plugin.view().vars(def, q), "faction", tag,
                "joueur", trigger == null ? "" : trigger.getName(), "player", trigger == null ? "" : trigger.getName());
        long points = plugin.rewardService().grant(factionId, recipients(factionId, target, q.getContributions().keySet()), rewards, vars);
        Object[] done = with(vars, "points", points);
        List<Player> online = hook.getOnlineMembers(factionId);
        if (s.factionNotify) {
            for (Player p : online) {
                plugin.msg().send(p, "quest.faction-complete", done);
                Sounds.play(p, s.sound("quete-terminee"));
            }
        }
        if (s.factionBroadcast) {
            plugin.msg().broadcast("quest.faction-complete-broadcast", done);
        }
        Bukkit.getPluginManager().callEvent(new QuestCompleteEvent(trigger, def, factionId, points));

        int count = fd.countDone();
        for (Map.Entry<Integer, RewardSettings.Block> tier : plugin.rewards().factionTiers().entrySet()) {
            if (tier.getKey() > count || fd.getTiers().contains(tier.getKey())) {
                continue;
            }
            fd.getTiers().add(tier.getKey());
            Object[] bonusVars = with(vars, "terminees", count, "total", fd.getQuests().size());
            long bonusPoints = plugin.rewardService().grant(factionId,
                    recipients(factionId, tier.getValue().target, fd.getContributors()), tier.getValue().rewards, bonusVars);
            for (Player p : online) {
                plugin.msg().send(p, "bonus.faction", with(bonusVars, "points", bonusPoints));
                Sounds.play(p, s.sound("bonus"));
            }
            Bukkit.getPluginManager().callEvent(new BonusEarnedEvent(QuestScope.FACTION, null, factionId, tier.getKey(), bonusPoints));
        }
    }

    /** Joueurs qui reçoivent les récompenses personnelles d'une réussite de faction. */
    private Collection<UUID> recipients(String factionId, RewardSettings.Target target, Set<String> contributors) {
        FactionHook hook = plugin.factionHook();
        switch (target) {
            case MEMBRES:
                return new ArrayList<UUID>(hook.getMembers(factionId));
            case MEMBRES_EN_LIGNE:
                List<UUID> online = new ArrayList<UUID>();
                for (Player p : hook.getOnlineMembers(factionId)) {
                    online.add(p.getUniqueId());
                }
                return online;
            case CONTRIBUTEURS:
                // les anciens membres gardent leur contribution dans la faction, mais pas les récompenses
                Set<UUID> members = hook.getMembers(factionId);
                Set<UUID> out = new HashSet<UUID>();
                for (String id : contributors) {
                    try {
                        UUID uuid = UUID.fromString(id);
                        if (members.contains(uuid)) {
                            out.add(uuid);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // identifiant corrompu
                    }
                }
                return out;
            default:
                return Collections.emptyList();
        }
    }

    // ============================================================== Outils

    /** Ajoute des variables en tête : elles l'emportent sur celles de même nom déjà présentes. */
    public static Object[] with(Object[] kv, Object... extra) {
        Object[] all = Arrays.copyOf(extra, extra.length + kv.length);
        System.arraycopy(kv, 0, all, extra.length, kv.length);
        return all;
    }

    /** Type d'objectif livraison ? */
    public boolean isDelivery(QuestDefinition def) {
        ObjectiveType type = def == null ? null : plugin.objectives().get(def.type);
        return type != null && type.isDelivery();
    }
}
