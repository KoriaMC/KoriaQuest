package fr.koriamc.quest.progress;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.api.event.PlayerQuestsResetEvent;
import fr.koriamc.quest.api.event.WeeklyResetEvent;
import fr.koriamc.quest.config.Settings;
import fr.koriamc.quest.model.CycleState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Périodes des quêtes : la semaine des quêtes de faction (jour, heure et fuseau de
 * "cycle") et la période des quêtes joueur (chaque jour ou chaque semaine selon
 * "cycle-joueur"). Vérification régulière et passage à la période suivante.
 */
public final class CycleManager {

    /** Vérification toutes les 30 secondes. */
    private static final long CHECK_TICKS = 20L * 30;

    private final KoriaQuest plugin;
    private BukkitTask task;

    public CycleManager(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    /** Début de la semaine en cours : dernier jour/heure de réinitialisation passé. */
    public ZonedDateTime currentWeekStart() {
        Settings s = plugin.settings();
        ZonedDateTime now = ZonedDateTime.now(s.zone);
        ZonedDateTime start = now.with(TemporalAdjusters.previousOrSame(s.resetDay)).with(s.resetTime);
        if (start.isAfter(now)) {
            start = start.minusWeeks(1);
        }
        return start;
    }

    /** Début de la période en cours des quêtes joueur. */
    public ZonedDateTime currentPlayerStart() {
        Settings s = plugin.settings();
        if (!s.playerDaily) {
            return currentWeekStart();
        }
        ZonedDateTime now = ZonedDateTime.now(s.zone);
        ZonedDateTime start = now.with(s.playerResetTime);
        if (start.isAfter(now)) {
            start = start.minusDays(1);
        }
        return start;
    }

    /** Millisecondes avant les nouvelles quêtes de faction. */
    public long factionTimeLeft() {
        return Math.max(0, currentWeekStart().plusWeeks(1).toInstant().toEpochMilli() - System.currentTimeMillis());
    }

    /** Millisecondes avant les nouvelles quêtes joueur. */
    public long playerTimeLeft() {
        ZonedDateTime start = currentPlayerStart();
        ZonedDateTime next = plugin.settings().playerDaily ? start.plusDays(1) : start.plusWeeks(1);
        return Math.max(0, next.toInstant().toEpochMilli() - System.currentTimeMillis());
    }

    public void start() {
        stop();
        check();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::check, CHECK_TICKS, CHECK_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Passe à la période suivante si son début est atteint (ou au tout premier démarrage). */
    public void check() {
        CycleState state = plugin.data().cycle();
        long week = currentWeekStart().toInstant().toEpochMilli();
        if (state.getCycle() == 0 || state.getWeekStart() != week) {
            newWeek(state.getCycle() != 0);
        }
        long player = currentPlayerStart().toInstant().toEpochMilli();
        if (state.getPlayerCycle() == 0 || state.getPlayerStart() != player) {
            newPlayerPeriod(state.getPlayerCycle() != 0);
        }
    }

    /**
     * Nouvelle semaine des quêtes de faction : nouveau tirage commun ; chaque faction
     * repart de zéro à son prochain accès.
     */
    public void newWeek(boolean announce) {
        Settings s = plugin.settings();
        CycleState state = plugin.data().cycle();
        if (announce && s.remindUnfinished) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.quests().remindFactionUnfinished(p);
            }
        }
        long start = currentWeekStart().toInstant().toEpochMilli();
        state.next(start, plugin.quests().drawFactionQuests());
        plugin.getLogger().info("Nouvelle semaine de quêtes de faction (n°" + state.getCycle() + ", début "
                + Instant.ofEpochMilli(start).atZone(s.zone).toLocalDateTime() + ") : " + state.getFactionQuests());
        Bukkit.getPluginManager().callEvent(new WeeklyResetEvent(state.getCycle(), state.getFactionQuests()));
        if (announce && s.announceNewWeek) {
            plugin.msg().broadcast("cycle.new-week", "label", plugin.commands().getLabel());
        }
        plugin.data().saveAsync();
    }

    /** Nouvelle période des quêtes joueur : chacun devra rechoisir sa difficulté. */
    public void newPlayerPeriod(boolean announce) {
        Settings s = plugin.settings();
        CycleState state = plugin.data().cycle();
        if (announce && s.remindUnfinished) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.quests().remindPlayerUnfinished(p);
            }
        }
        long start = currentPlayerStart().toInstant().toEpochMilli();
        state.nextPlayer(start);
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.quests().player(p);
        }
        Bukkit.getPluginManager().callEvent(new PlayerQuestsResetEvent(state.getPlayerCycle(), s.playerDaily));
        if (announce && s.announceNewPlayerPeriod) {
            plugin.msg().broadcast("cycle.new-player-period", "label", plugin.commands().getLabel());
        }
        plugin.data().saveAsync();
    }
}
