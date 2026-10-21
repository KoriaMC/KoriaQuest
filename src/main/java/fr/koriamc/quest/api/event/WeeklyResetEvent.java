package fr.koriamc.quest.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

/**
 * Appelé après le passage à une nouvelle semaine de quêtes de faction (non annulable).
 */
public final class WeeklyResetEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long cycle;
    private final List<String> factionQuests;

    public WeeklyResetEvent(long cycle, List<String> factionQuests) {
        this.cycle = cycle;
        this.factionQuests = Collections.unmodifiableList(factionQuests);
    }

    /** Numéro de la nouvelle semaine. */
    public long getCycle() {
        return cycle;
    }

    /** Quêtes de faction tirées ("difficulte.id"), communes à toutes les factions. */
    public List<String> getFactionQuests() {
        return factionQuests;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
