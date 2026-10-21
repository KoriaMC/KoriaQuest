package fr.koriamc.quest.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Appelé après le renouvellement des quêtes joueur (non annulable) : chaque jour
 * par défaut, ou chaque semaine selon cycle-joueur.periode.
 */
public final class PlayerQuestsResetEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long period;
    private final boolean daily;

    public PlayerQuestsResetEvent(long period, boolean daily) {
        this.period = period;
        this.daily = daily;
    }

    /** Identifiant de la nouvelle période. */
    public long getPeriod() {
        return period;
    }

    /** true si les quêtes joueur sont quotidiennes, false si hebdomadaires. */
    public boolean isDaily() {
        return daily;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
