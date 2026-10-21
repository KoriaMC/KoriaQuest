package fr.koriamc.quest.listener;

import fr.koriamc.factions.api.event.FactionDisbandEvent;
import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Connexion (récompenses en attente, rappel du choix de difficulté), déconnexion
 * et dissolution de faction.
 */
public final class SessionListener implements Listener {

    private final KoriaQuest plugin;

    public SessionListener(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        plugin.quests().player(player);
        plugin.antiAbuse().activity(player);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                PlayerData pd = plugin.quests().player(player);
                plugin.rewardService().deliverPending(player, pd);
                if (plugin.settings().playerEnabled && !pd.hasChosen()) {
                    plugin.msg().send(player, "difficulty.not-chosen", "label", plugin.commands().getLabel());
                }
            }
        }, 60L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.antiAbuse().forget(event.getPlayer().getUniqueId());
        plugin.quests().forget(event.getPlayer().getUniqueId());
    }

    /** Une dissolution forcée ignore l'annulation : on vérifie au tick suivant que la faction a disparu. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisband(FactionDisbandEvent event) {
        final String id = event.getFaction().getId();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (plugin.factionHook().getFaction(id) == null) {
                    plugin.data().removeFaction(id);
                }
            }
        });
    }
}
