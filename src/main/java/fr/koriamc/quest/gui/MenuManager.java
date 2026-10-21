package fr.koriamc.quest.gui;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.command.SubCommand;
import fr.koriamc.quest.config.JsonFile;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.progress.QuestManager;
import fr.koriamc.quest.util.Sounds;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.Locale;

/**
 * Ouverture des menus et gestion des clics.
 */
public final class MenuManager implements Listener {

    private final KoriaQuest plugin;
    private final ChatPrompt prompt;

    public MenuManager(KoriaQuest plugin) {
        this.plugin = plugin;
        this.prompt = new ChatPrompt(plugin);
    }

    /** Saisie de valeurs au clavier (éditeur de configuration). */
    public ChatPrompt prompt() {
        return prompt;
    }

    /** Section de menus.json (jamais nulle). */
    public ConfigurationSection section(String path) {
        ConfigurationSection s = plugin.menusConfig().getConfigurationSection(path);
        return s != null ? s : new MemoryConfiguration().createSection(path);
    }

    /**
     * Ouvre un menu d'après son identifiant (action MENU:&lt;id&gt; de menus.json) :
     * joueur, difficulte, faction, config, config:&lt;fichier&gt;.
     */
    public void open(Player player, String id) {
        String raw = id == null ? "joueur" : id.trim();
        int colon = raw.indexOf(':');
        String key = (colon < 0 ? raw : raw.substring(0, colon)).toLowerCase(Locale.ROOT);
        String arg = colon < 0 ? null : raw.substring(colon + 1).trim();
        switch (key) {
            case "joueur":
            case "principal":
                openPlayer(player);
                break;
            case "difficulte":
                if (!plugin.settings().playerEnabled) {
                    plugin.msg().send(player, "quest.player-disabled");
                    return;
                }
                new DifficultyMenu(plugin, player).open();
                break;
            case "faction":
                openFaction(player);
                break;
            case "config":
                SubCommand command = plugin.commands().get("config");
                if (command == null || !command.hasPermission(player)) {
                    plugin.msg().send(player, "general.no-permission");
                } else if (!openConfig(player, arg)) {
                    plugin.msg().send(player, "config-editor.unknown-file", "file", arg);
                }
                break;
            default:
                plugin.getLogger().warning("Menu inconnu : " + id);
        }
    }

    /** Quêtes du joueur, ou le choix de la difficulté s'il ne l'a pas encore fait. */
    public void openPlayer(Player player) {
        if (!plugin.settings().playerEnabled) {
            if (plugin.settings().factionEnabled) {
                openFaction(player);
            } else {
                plugin.msg().send(player, "quest.player-disabled");
            }
            return;
        }
        PlayerData pd = plugin.quests().player(player);
        if (!pd.hasChosen() && plugin.settings().playerAutoOpenChoice) {
            new DifficultyMenu(plugin, player).open();
        } else {
            new PlayerQuestsMenu(plugin, player).open();
        }
    }

    public void openFaction(Player player) {
        if (!plugin.settings().factionEnabled) {
            plugin.msg().send(player, "quest.faction-disabled");
            return;
        }
        String factionId = plugin.factionHook().getFactionId(player);
        if (factionId == null) {
            plugin.msg().send(player, "quest.no-faction");
            return;
        }
        if (!plugin.quests().isEligible(factionId)) {
            plugin.msg().send(player, "quest.faction-too-small", "min", plugin.settings().factionMinMembers);
            return;
        }
        new FactionQuestsMenu(plugin, player, factionId).open();
    }

    /** Choix d'une difficulté (menu ou commande) et message correspondant. */
    public void choose(Player player, String difficulty) {
        QuestManager.Choice result = plugin.quests().choose(player, difficulty);
        String name = plugin.settings().difficultyName(difficulty);
        switch (result) {
            case OK:
                plugin.msg().send(player, "difficulty.chosen", "difficulte", name);
                new PlayerQuestsMenu(plugin, player).open();
                return;
            case CONFIRM:
                plugin.msg().send(player, "difficulty.confirm", "difficulte", name);
                return;
            case LOCKED:
                plugin.msg().send(player, "difficulty.locked");
                break;
            case UNKNOWN:
                plugin.msg().send(player, "difficulty.unknown", "difficultes",
                        String.join(", ", plugin.settings().difficultyArguments.keySet()));
                break;
            case NO_QUEST:
                plugin.msg().send(player, "quest.none-available");
                break;
            case NO_FACTION:
                plugin.msg().send(player, "quest.no-faction");
                break;
            default:
                plugin.msg().send(player, "quest.player-disabled");
                break;
        }
        Sounds.play(player, plugin.settings().sound("erreur"));
    }

    /**
     * Éditeur de configuration : liste des fichiers, ou directement un fichier.
     *
     * @return false si le fichier demandé n'existe pas
     */
    public boolean openConfig(Player player, String fileName) {
        if (fileName == null) {
            new ConfigFilesMenu(plugin, player).open();
            return true;
        }
        for (JsonFile file : plugin.configFiles()) {
            String name = file.getName();
            if (name.equalsIgnoreCase(fileName) || name.equalsIgnoreCase(fileName + ".json")) {
                new ConfigSectionMenu(plugin, player, file, Collections.<String>emptyList(), 0).open();
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof Menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize() && event.getWhoClicked() instanceof Player) {
            ((Menu) top.getHolder()).handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
