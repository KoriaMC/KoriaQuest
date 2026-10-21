package fr.koriamc.quest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Périodes en cours. Quêtes de faction : numéro de semaine (incrémenté à chaque
 * nouvelle semaine, même forcée), début et tirage commun à toutes les factions.
 * Quêtes joueur : identifiant de période (jour ou semaine selon la config) et début.
 */
public final class CycleState {

    private long cycle;
    private long weekStart;
    private List<String> factionQuests = new ArrayList<String>();
    /** Identifiant unique de la période des quêtes joueur (début en ms, toujours croissant). */
    private long playerCycle;
    private long playerStart;

    public void ensureInit() {
        if (factionQuests == null) factionQuests = new ArrayList<String>();
    }

    public long getCycle() {
        return cycle;
    }

    public long getWeekStart() {
        return weekStart;
    }

    /** Passe à la semaine suivante (quêtes de faction). */
    public void next(long weekStart, List<String> factionQuests) {
        this.cycle++;
        this.weekStart = weekStart;
        this.factionQuests = new ArrayList<String>(factionQuests);
    }

    /** Clés "difficulte.id" des quêtes de faction de la semaine. */
    public List<String> getFactionQuests() {
        return factionQuests;
    }

    public long getPlayerCycle() {
        return playerCycle;
    }

    public long getPlayerStart() {
        return playerStart;
    }

    /**
     * Passe à la période suivante des quêtes joueur. L'identifiant reste unique même
     * pour un renouvellement forcé dans la même période.
     */
    public void nextPlayer(long start) {
        this.playerCycle = Math.max(start, playerCycle + 1);
        this.playerStart = start;
    }
}
