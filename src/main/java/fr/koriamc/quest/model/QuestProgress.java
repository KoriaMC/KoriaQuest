package fr.koriamc.quest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Avancement d'une quête pendant la semaine en cours.
 */
public final class QuestProgress {

    private String quest;
    private long target;
    private long progress;
    private long completedAt;
    /** Quêtes de faction : UUID du joueur -> quantité apportée. */
    private Map<String, Long> contributions;

    public QuestProgress() {
    }

    public QuestProgress(String quest, long target) {
        this.quest = quest;
        this.target = Math.max(1, target);
    }

    /** Clé "difficulte.id" de la quête. */
    public String getQuest() {
        return quest;
    }

    public String getDifficulty() {
        int dot = quest.indexOf('.');
        return dot < 0 ? "" : quest.substring(0, dot);
    }

    public long getTarget() {
        return target;
    }

    public long getProgress() {
        return progress;
    }

    /** @return la quantité réellement ajoutée (l'avancement ne dépasse jamais l'objectif) */
    public long add(long amount) {
        long before = progress;
        progress = Math.max(0, Math.min(target, progress + amount));
        return progress - before;
    }

    public void setProgress(long progress) {
        this.progress = Math.max(0, Math.min(target, progress));
    }

    public boolean isDone() {
        return progress >= target;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public double getPercent() {
        return target <= 0 ? 100 : Math.min(100.0, progress * 100.0 / target);
    }

    public void contribute(String uuid, long amount) {
        if (amount <= 0) {
            return;
        }
        if (contributions == null) {
            contributions = new HashMap<String, Long>();
        }
        Long current = contributions.get(uuid);
        contributions.put(uuid, (current == null ? 0 : current) + amount);
    }

    public long getContribution(String uuid) {
        Long value = contributions == null ? null : contributions.get(uuid);
        return value == null ? 0 : value;
    }

    public Map<String, Long> getContributions() {
        return contributions == null ? Collections.<String, Long>emptyMap() : contributions;
    }

    /** Contributeurs triés du plus au moins actif. */
    public List<Map.Entry<String, Long>> getRanking() {
        List<Map.Entry<String, Long>> list = new ArrayList<Map.Entry<String, Long>>(getContributions().entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return Long.compare(b.getValue(), a.getValue());
            }
        });
        return list;
    }
}
