package fr.koriamc.quest.api.event;

import fr.koriamc.quest.model.QuestDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Appelé avant qu'une action fasse avancer une quête. La quantité peut être
 * modifiée ; l'annulation ignore l'action pour cette quête.
 */
public final class QuestProgressEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final QuestDefinition quest;
    /** Faction concernée pour une quête de faction, null pour une quête joueur. */
    private final String factionId;
    private long amount;
    private boolean cancelled;

    public QuestProgressEvent(Player player, QuestDefinition quest, String factionId, long amount) {
        this.player = player;
        this.quest = quest;
        this.factionId = factionId;
        this.amount = amount;
    }

    public Player getPlayer() {
        return player;
    }

    public QuestDefinition getQuest() {
        return quest;
    }

    public String getFactionId() {
        return factionId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
