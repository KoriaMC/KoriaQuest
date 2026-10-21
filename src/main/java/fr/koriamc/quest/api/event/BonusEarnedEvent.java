package fr.koriamc.quest.api.event;

import fr.koriamc.quest.model.QuestScope;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Appelé après l'obtention d'un palier de bonus (non annulable).
 */
public final class BonusEarnedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final QuestScope scope;
    private final UUID player;
    private final String factionId;
    private final int tier;
    private final long points;

    public BonusEarnedEvent(QuestScope scope, UUID player, String factionId, int tier, long points) {
        this.scope = scope;
        this.player = player;
        this.factionId = factionId;
        this.tier = tier;
        this.points = points;
    }

    public QuestScope getScope() {
        return scope;
    }

    /** Joueur concerné (bonus joueur), null pour un bonus de faction. */
    public UUID getPlayer() {
        return player;
    }

    public String getFactionId() {
        return factionId;
    }

    /** Nombre de quêtes terminées qui a débloqué ce palier. */
    public int getTier() {
        return tier;
    }

    public long getPoints() {
        return points;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
