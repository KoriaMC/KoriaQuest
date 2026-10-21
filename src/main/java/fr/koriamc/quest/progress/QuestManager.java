package fr.koriamc.quest.progress;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.config.Settings;
import fr.koriamc.quest.model.FactionData;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestProgress;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.util.Sounds;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Quêtes de la semaine : accès aux données à jour (une ancienne semaine est
 * remise à zéro au premier accès), tirages et choix de la difficulté.
 */
public final class QuestManager {

    private static final long CONFIRM_MS = 5000L;

    /** Résultat d'un choix de difficulté. */
    public enum Choice {
        OK,
        CONFIRM,
        LOCKED,
        UNKNOWN,
        NO_QUEST,
        DISABLED,
        NO_FACTION
    }

    private final KoriaQuest plugin;
    private final Random random = new Random();
    private final Map<UUID, String> confirmTarget = new HashMap<UUID, String>();
    private final Map<UUID, Long> confirmTime = new HashMap<UUID, Long>();

    public QuestManager(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    /** Semaine en cours des quêtes de faction. */
    public long cycle() {
        return plugin.data().cycle().getCycle();
    }

    /** Période en cours des quêtes joueur (jour ou semaine). */
    public long playerCycle() {
        return plugin.data().cycle().getPlayerCycle();
    }

    // ============================================================== Joueurs

    public PlayerData player(Player player) {
        return player(player.getUniqueId(), player.getName());
    }

    /** Données du joueur pour la période en cours. */
    public PlayerData player(UUID uuid, String name) {
        PlayerData pd = plugin.data().player(uuid, name);
        if (pd.getCycle() != playerCycle()) {
            pd.reset(playerCycle());
        }
        return pd;
    }

    /**
     * Choix de la difficulté (menu ou commande). Avec la confirmation activée, le
     * premier appel renvoie CONFIRM et le second, dans les 5 secondes, valide.
     */
    public Choice choose(Player player, String difficulty) {
        Settings s = plugin.settings();
        if (!s.playerEnabled) {
            return Choice.DISABLED;
        }
        String diff = difficulty == null ? "" : difficulty.toLowerCase(Locale.ROOT);
        if (!s.isDifficulty(diff)) {
            return Choice.UNKNOWN;
        }
        PlayerData pd = player(player);
        if (pd.hasChosen() && (!s.playerCanChange || diff.equals(pd.getDifficulty()))) {
            return Choice.LOCKED;
        }
        if (s.playerRequiresFaction && plugin.factionHook().getFactionId(player) == null) {
            return Choice.NO_FACTION;
        }
        List<QuestDefinition> pool = plugin.catalog().list(QuestScope.JOUEUR, diff);
        if (pool.isEmpty()) {
            return Choice.NO_QUEST;
        }
        UUID uuid = player.getUniqueId();
        if (s.playerConfirm) {
            Long time = confirmTime.get(uuid);
            if (!diff.equals(confirmTarget.get(uuid)) || time == null || System.currentTimeMillis() - time > CONFIRM_MS) {
                confirmTarget.put(uuid, diff);
                confirmTime.put(uuid, System.currentTimeMillis());
                return Choice.CONFIRM;
            }
        }
        confirmTarget.remove(uuid);
        confirmTime.remove(uuid);
        pd.reset(playerCycle());
        pd.setDifficulty(diff);
        for (QuestDefinition def : draw(pool, s.playerCount)) {
            pd.getQuests().add(new QuestProgress(def.key(), def.objective));
        }
        Sounds.play(player, s.sound("choix-difficulte"));
        return Choice.OK;
    }

    public void forget(UUID uuid) {
        confirmTarget.remove(uuid);
        confirmTime.remove(uuid);
    }

    // ============================================================= Factions

    /** La faction a-t-elle des quêtes (système actif, assez de membres) ? */
    public boolean isEligible(String factionId) {
        return plugin.settings().factionEnabled && factionId != null
                && plugin.factionHook().getSize(factionId) >= plugin.settings().factionMinMembers;
    }

    /** Données de la faction pour la semaine (tirage commun, objectifs selon sa taille). */
    public FactionData faction(String factionId) {
        FactionData fd = plugin.data().faction(factionId);
        if (fd.getCycle() != cycle()) {
            fd.reset(cycle());
            int size = plugin.factionHook().getSize(factionId);
            for (String key : plugin.data().cycle().getFactionQuests()) {
                QuestDefinition def = plugin.catalog().get(QuestScope.FACTION, key);
                if (def != null) {
                    fd.getQuests().add(new QuestProgress(key, def.targetFor(size)));
                }
            }
        }
        return fd;
    }

    /** Tirage des quêtes de faction de la semaine, par difficulté. */
    public List<String> drawFactionQuests() {
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : plugin.settings().factionCounts.entrySet()) {
            for (QuestDefinition def : draw(plugin.catalog().list(QuestScope.FACTION, e.getKey()), e.getValue())) {
                keys.add(def.key());
            }
        }
        return keys;
    }

    /** Tirage pondéré (option "poids") sans doublon. */
    public List<QuestDefinition> draw(List<QuestDefinition> pool, int count) {
        List<QuestDefinition> left = new ArrayList<QuestDefinition>(pool);
        List<QuestDefinition> out = new ArrayList<QuestDefinition>();
        while (out.size() < count && !left.isEmpty()) {
            int total = 0;
            for (QuestDefinition def : left) {
                total += def.weight;
            }
            int roll = random.nextInt(Math.max(1, total));
            for (int i = 0; i < left.size(); i++) {
                roll -= left.get(i).weight;
                if (roll < 0 || i == left.size() - 1) {
                    out.add(left.remove(i));
                    break;
                }
            }
        }
        return out;
    }

    // ====================================================== Fin de période

    /** Rappelle au joueur ses quêtes non terminées de la période qui s'achève. */
    public void remindPlayerUnfinished(Player player) {
        PlayerData pd = plugin.data().findPlayer(player.getUniqueId());
        if (pd != null && pd.getCycle() == playerCycle()) {
            for (QuestProgress q : pd.getQuests()) {
                QuestDefinition def = plugin.catalog().get(QuestScope.JOUEUR, q.getQuest());
                if (def != null && !q.isDone()) {
                    plugin.msg().send(player, "quest.failed", "quete", def.name, "progression", q.getProgress(),
                            "objectif", q.getTarget());
                }
            }
        }
    }

    /** Rappelle au joueur les quêtes de sa faction non terminées de la semaine qui s'achève. */
    public void remindFactionUnfinished(Player player) {
        String factionId = plugin.factionHook().getFactionId(player);
        FactionData fd = factionId == null ? null : plugin.data().findFaction(factionId);
        if (fd != null && fd.getCycle() == cycle()) {
            for (QuestProgress q : fd.getQuests()) {
                QuestDefinition def = plugin.catalog().get(QuestScope.FACTION, q.getQuest());
                if (def != null && !q.isDone()) {
                    plugin.msg().send(player, "quest.faction-failed", "quete", def.name, "progression", q.getProgress(),
                            "objectif", q.getTarget());
                }
            }
        }
    }

    public void resetPlayer(UUID uuid, String name) {
        plugin.data().player(uuid, name).reset(playerCycle());
    }

    public void resetFaction(String factionId) {
        plugin.data().faction(factionId).reset(-1);
        faction(factionId);
    }
}
