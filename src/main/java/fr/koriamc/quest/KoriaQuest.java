package fr.koriamc.quest;

import fr.koriamc.quest.command.CommandManager;
import fr.koriamc.quest.config.ConfigDocs;
import fr.koriamc.quest.config.JsonFile;
import fr.koriamc.quest.config.Messages;
import fr.koriamc.quest.config.QuestCatalog;
import fr.koriamc.quest.config.RewardSettings;
import fr.koriamc.quest.config.Settings;
import fr.koriamc.quest.gui.Menu;
import fr.koriamc.quest.gui.MenuManager;
import fr.koriamc.quest.hook.FactionHook;
import fr.koriamc.quest.hook.PapiHook;
import fr.koriamc.quest.hook.VaultHook;
import fr.koriamc.quest.listener.ObjectiveListener;
import fr.koriamc.quest.listener.SessionListener;
import fr.koriamc.quest.objective.ObjectiveRegistry;
import fr.koriamc.quest.progress.AntiAbuse;
import fr.koriamc.quest.progress.CycleManager;
import fr.koriamc.quest.progress.ProgressService;
import fr.koriamc.quest.progress.QuestManager;
import fr.koriamc.quest.progress.QuestView;
import fr.koriamc.quest.reward.RewardService;
import fr.koriamc.quest.storage.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * KoriaQuest : quêtes hebdomadaires de faction et de joueur pour KoriaMC (Spigot 1.8.8).
 */
public final class KoriaQuest extends JavaPlugin {

    private static KoriaQuest instance;

    // Fichiers
    private JsonFile configFile;
    private JsonFile factionQuestsFile;
    private JsonFile playerQuestsFile;
    private JsonFile rewardsFile;
    private JsonFile messagesFile;
    private JsonFile menusFile;
    private Configuration config = new MemoryConfiguration();
    private Configuration messagesConfig = new MemoryConfiguration();
    private Configuration menusConfig = new MemoryConfiguration();

    // Réglages
    private final Settings settings = new Settings();
    private final QuestCatalog catalog = new QuestCatalog();
    private final RewardSettings rewards = new RewardSettings();
    private final Messages messages = new Messages();
    private final ConfigDocs configDocs = new ConfigDocs();
    private final ObjectiveRegistry objectives = new ObjectiveRegistry();

    // Services
    private DataManager data;
    private FactionHook factionHook;
    private QuestManager quests;
    private CycleManager cycles;
    private ProgressService progress;
    private QuestView view;
    private RewardService rewardService;
    private AntiAbuse antiAbuse;
    private MenuManager menus;
    private CommandManager commands;
    private ObjectiveListener objectiveListener;
    private PapiHook papi;
    private VaultHook vault;

    public static KoriaQuest get() {
        return instance;
    }

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;

        configFile = new JsonFile(this, "config.json", "difficultes", "quetes-faction.nombre");
        factionQuestsFile = new JsonFile(this, "quests_faction.json", "*");
        playerQuestsFile = new JsonFile(this, "quests_player.json", "*");
        rewardsFile = new JsonFile(this, "rewards.json", "faction.par-quete", "faction.paliers", "joueur", "joueur.*.paliers");
        messagesFile = new JsonFile(this, "messages.json");
        menusFile = new JsonFile(this, "menus.json", "joueur.objets", "difficulte.objets", "faction.objets", "editeur.fichiers");

        data = new DataManager(this);
        factionHook = new FactionHook();
        quests = new QuestManager(this);
        cycles = new CycleManager(this);
        progress = new ProgressService(this);
        view = new QuestView(this);
        rewardService = new RewardService(this);
        antiAbuse = new AntiAbuse(this);
        menus = new MenuManager(this);
        commands = new CommandManager(this);
        objectiveListener = new ObjectiveListener(this);

        for (String problem : loadConfigs()) {
            getLogger().warning(problem);
        }
        configDocs.load(this);
        configDocs.writeReadme(this, configFiles());

        try {
            data.init();
            data.load();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Impossible de charger les données : le plugin est désactivé pour éviter toute perte.", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        antiAbuse.loadPlaced(placedBlocksFile());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                papi = new PapiHook(this);
                getLogger().info("PlaceholderAPI détecté : variables %koriaquest_...% enregistrées.");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Impossible de s'accrocher à PlaceholderAPI", t);
                papi = null;
            }
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(objectiveListener, this);
        pm.registerEvents(new SessionListener(this), this);
        pm.registerEvents(menus, this);
        pm.registerEvents(menus.prompt(), this);

        commands.load(config, messagesConfig);
        commands.registerBukkitCommand();

        cycles.start();
        data.startAutoSave();
        objectiveListener.start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            quests.player(player);
            antiAbuse.activity(player);
        }
        getLogger().info("KoriaQuest activé en " + (System.currentTimeMillis() - start) + " ms (stockage : "
                + data.getStorageName() + ", " + catalog.count() + " quêtes).");
    }

    @Override
    public void onDisable() {
        if (data == null) {
            return;
        }
        if (commands != null) {
            commands.unregisterBukkitCommand();
        }
        if (cycles != null) {
            cycles.stop();
        }
        if (objectiveListener != null) {
            objectiveListener.stop();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
        if (papi != null) {
            papi.unregister();
            papi = null;
        }
        if (antiAbuse != null) {
            antiAbuse.savePlaced(placedBlocksFile());
        }
        data.shutdown();
        instance = null;
    }

    private File placedBlocksFile() {
        return new File(getDataFolder(), "blocs-poses.dat");
    }

    /** @return les problèmes à signaler (erreurs JSON, quêtes invalides) */
    private List<String> loadConfigs() {
        List<String> problems = new ArrayList<String>();
        for (JsonFile file : configFiles()) {
            String error = file.load();
            if (error != null) {
                problems.add(file.getName() + " : " + error);
            }
        }
        config = configFile.toConfiguration();
        messagesConfig = messagesFile.toConfiguration();
        menusConfig = menusFile.toConfiguration();

        settings.load(config, getLogger());
        messages.load(messagesConfig, settings);
        rewards.load(rewardsFile.toConfiguration());
        catalog.load(factionQuestsFile.toConfiguration(), playerQuestsFile.toConfiguration(), settings, objectives);
        problems.addAll(rewards.getProblems());
        problems.addAll(catalog.getProblems());
        return problems;
    }

    /**
     * Recharge toute la configuration (/quests reload, éditeur).
     *
     * @return les problèmes détectés, à afficher à l'auteur de la commande
     */
    public List<String> reload() {
        List<String> problems = loadConfigs();
        commands.load(config, messagesConfig);
        data.startAutoSave();
        cycles.check();
        return problems;
    }

    /** Dépose de l'argent via Vault. @return false si aucune économie n'est disponible */
    public boolean deposit(Player player, double amount) {
        if (vault == null && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vault = VaultHook.create();
        }
        if (vault == null) {
            return false;
        }
        vault.deposit(player, amount);
        return true;
    }

    // ============================================================ Accesseurs

    public Settings settings() {
        return settings;
    }

    public QuestCatalog catalog() {
        return catalog;
    }

    public RewardSettings rewards() {
        return rewards;
    }

    public Messages msg() {
        return messages;
    }

    /** Explication des options (éditeur /quests config et LISEZMOI.txt). */
    public ConfigDocs configDocs() {
        return configDocs;
    }

    public ObjectiveRegistry objectives() {
        return objectives;
    }

    public Configuration config() {
        return config;
    }

    public Configuration menusConfig() {
        return menusConfig;
    }

    /** Fichiers de configuration, dans l'ordre de l'éditeur. */
    public List<JsonFile> configFiles() {
        return Arrays.asList(configFile, factionQuestsFile, playerQuestsFile, rewardsFile, messagesFile, menusFile);
    }

    public DataManager data() {
        return data;
    }

    public FactionHook factionHook() {
        return factionHook;
    }

    public QuestManager quests() {
        return quests;
    }

    public CycleManager cycles() {
        return cycles;
    }

    public ProgressService progress() {
        return progress;
    }

    public QuestView view() {
        return view;
    }

    public RewardService rewardService() {
        return rewardService;
    }

    public AntiAbuse antiAbuse() {
        return antiAbuse;
    }

    public MenuManager menus() {
        return menus;
    }

    public CommandManager commands() {
        return commands;
    }
}
