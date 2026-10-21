package fr.koriamc.quest.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Données persistantes d'un joueur : difficulté et quêtes de la semaine,
 * paliers de bonus déjà reçus et récompenses en attente (reçues hors ligne).
 */
public final class PlayerData {

    private String uuid;
    private String name;
    /** Cycle (semaine) auquel appartiennent les quêtes ; différent du cycle courant = à réinitialiser. */
    private long cycle;
    private String difficulty;
    private List<QuestProgress> quests = new ArrayList<QuestProgress>();
    private Set<Integer> tiers = new HashSet<Integer>();
    /** Actions de récompense déjà résolues, exécutées à la prochaine connexion. */
    private List<String> pending = new ArrayList<String>();

    public PlayerData() {
    }

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid.toString();
        this.name = name;
    }

    /** Corrige les collections nulles après une désérialisation. */
    public void ensureInit() {
        if (quests == null) quests = new ArrayList<QuestProgress>();
        if (tiers == null) tiers = new HashSet<Integer>();
        if (pending == null) pending = new ArrayList<String>();
    }

    public UUID getUniqueId() {
        return UUID.fromString(uuid);
    }

    public String getId() {
        return uuid;
    }

    public String getName() {
        return name == null ? "?" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCycle() {
        return cycle;
    }

    /** Vide les quêtes de la semaine (les récompenses en attente sont conservées). */
    public void reset(long cycle) {
        this.cycle = cycle;
        this.difficulty = null;
        this.quests.clear();
        this.tiers.clear();
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean hasChosen() {
        return difficulty != null;
    }

    public List<QuestProgress> getQuests() {
        return quests;
    }

    public int countDone() {
        int n = 0;
        for (QuestProgress q : quests) {
            if (q.isDone()) {
                n++;
            }
        }
        return n;
    }

    public Set<Integer> getTiers() {
        return tiers;
    }

    public List<String> getPending() {
        return pending;
    }

    /** Rien à conserver : quêtes d'une ancienne semaine et aucune récompense en attente. */
    public boolean isDisposable(long currentCycle) {
        return pending.isEmpty() && (cycle != currentCycle || (difficulty == null && quests.isEmpty()));
    }
}
