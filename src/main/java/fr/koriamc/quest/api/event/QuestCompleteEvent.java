package fr.koriamc.quest.api.event;

import fr.koriamc.quest.model.QuestDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Appelé après la réussite d'une quête, une fois les récompenses données (non annulable).
 */
public final class QuestCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final QuestDefinition quest;
    private final String factionId;
    private final long points;

    public QuestCompleteEvent(Player player, QuestDefinition quest, String factionId, long points) {
        this.player = player;
        this.quest = quest;
        this.factionId = factionId;
        this.points = points;
    }

    /** Joueur dont l'action a terminé la quête (peut être null pour une action d'administration). */
    public Player getPlayer() {
        return player;
    }

    public QuestDefinition getQuest() {
        return quest;
    }

    /** Faction qui a reçu les points (null si aucune). */
    public String getFactionId() {
        return factionId;
    }

    /** Points de faction crédités. */
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
