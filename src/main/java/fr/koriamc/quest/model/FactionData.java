package fr.koriamc.quest.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quêtes de la semaine d'une faction. La progression reste attachée à la faction :
 * un joueur qui la quitte lui laisse ses contributions.
 */
public final class FactionData {

    private String faction;
    private long cycle;
    private List<QuestProgress> quests = new ArrayList<QuestProgress>();
    private Set<Integer> tiers = new HashSet<Integer>();

    public FactionData() {
    }

    public FactionData(String factionId) {
        this.faction = factionId;
    }

    public void ensureInit() {
        if (quests == null) quests = new ArrayList<QuestProgress>();
        if (tiers == null) tiers = new HashSet<Integer>();
    }

    public String getFactionId() {
        return faction;
    }

    public long getCycle() {
        return cycle;
    }

    public void reset(long cycle) {
        this.cycle = cycle;
        this.quests.clear();
        this.tiers.clear();
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

    /** Joueurs ayant contribué à au moins une quête de la semaine. */
    public Set<String> getContributors() {
        Set<String> all = new HashSet<String>();
        for (QuestProgress q : quests) {
            all.addAll(q.getContributions().keySet());
        }
        return all;
    }
}
