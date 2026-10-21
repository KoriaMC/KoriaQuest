package fr.koriamc.quest.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.config.Settings;
import fr.koriamc.quest.model.CycleState;
import fr.koriamc.quest.model.FactionData;
import fr.koriamc.quest.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Données en mémoire (joueurs, factions, semaine en cours), chargées au démarrage
 * puis sérialisées en JSON sur le thread principal et écrites par un thread dédié.
 */
public final class DataManager {

    private static final String CYCLE_KEY = "cycle";

    private final KoriaQuest plugin;
    private final Map<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();
    private final Map<String, FactionData> factions = new HashMap<String, FactionData>();
    private CycleState cycle = new CycleState();
    private Storage storage;
    private Gson gson;
    private ExecutorService executor;
    private BukkitTask autoSave;
    /** Tant que le chargement n'a pas réussi, rien n'est écrit (évite d'écraser les données). */
    private boolean loaded;

    public DataManager(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    public void init() throws Exception {
        Settings s = plugin.settings();
        GsonBuilder builder = new GsonBuilder().disableHtmlEscaping();
        File folder = plugin.getDataFolder();
        if ("MYSQL".equals(s.storageType) || "MARIADB".equals(s.storageType)) {
            storage = SqlStorage.mysql(s.mysqlHost, s.mysqlPort, s.mysqlDatabase, s.mysqlUser, s.mysqlPassword,
                    s.mysqlPrefix, s.mysqlParameters);
        } else if ("SQLITE".equals(s.storageType)) {
            storage = SqlStorage.sqlite(new File(folder, s.sqliteFile));
        } else {
            if (!"JSON".equals(s.storageType)) {
                plugin.getLogger().warning("Type de stockage inconnu '" + s.storageType + "', JSON utilisé.");
            }
            if (s.jsonPretty) {
                builder.setPrettyPrinting();
            }
            storage = new JsonStorage(new File(folder, "data"), s.jsonBackups, plugin.getLogger());
        }
        gson = builder.create();
        storage.init();
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "KoriaQuest-Save");
            thread.setDaemon(true);
            return thread;
        });
    }

    public String getStorageName() {
        return storage == null ? "?" : storage.getName();
    }

    public void load() throws Exception {
        DataSnapshot data = storage.load();
        players.clear();
        factions.clear();
        for (Map.Entry<String, String> e : data.players.entrySet()) {
            try {
                PlayerData pd = gson.fromJson(e.getValue(), PlayerData.class);
                if (pd != null) {
                    pd.ensureInit();
                    players.put(pd.getUniqueId(), pd);
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Joueur illisible ignoré (" + e.getKey() + ") : " + ex.getMessage());
            }
        }
        for (Map.Entry<String, String> e : data.factions.entrySet()) {
            try {
                FactionData fd = gson.fromJson(e.getValue(), FactionData.class);
                if (fd != null && fd.getFactionId() != null) {
                    fd.ensureInit();
                    factions.put(fd.getFactionId(), fd);
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Faction illisible ignorée (" + e.getKey() + ") : " + ex.getMessage());
            }
        }
        String rawCycle = data.meta.get(CYCLE_KEY);
        CycleState state = rawCycle == null ? null : gson.fromJson(rawCycle, CycleState.class);
        cycle = state == null ? new CycleState() : state;
        cycle.ensureInit();
        loaded = true;
        plugin.getLogger().info("Données chargées (" + getStorageName() + ") : " + players.size() + " joueur(s), "
                + factions.size() + " faction(s).");
    }

    // ============================================================== Accès

    public CycleState cycle() {
        return cycle;
    }

    /** Données d'un joueur (créées si besoin). */
    public PlayerData player(UUID uuid, String name) {
        PlayerData pd = players.get(uuid);
        if (pd == null) {
            pd = new PlayerData(uuid, name);
            pd.reset(cycle.getPlayerCycle());
            players.put(uuid, pd);
        } else if (name != null) {
            pd.setName(name);
        }
        return pd;
    }

    /** @return les données d'un joueur, ou null s'il n'en a pas. */
    public PlayerData findPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public Collection<PlayerData> players() {
        return players.values();
    }

    /** Données d'une faction (créées si besoin, sans quête : à initialiser par l'appelant). */
    public FactionData faction(String factionId) {
        FactionData fd = factions.get(factionId);
        if (fd == null) {
            fd = new FactionData(factionId);
            fd.reset(-1);
            factions.put(factionId, fd);
        }
        return fd;
    }

    public FactionData findFaction(String factionId) {
        return factions.get(factionId);
    }

    public Collection<FactionData> factions() {
        return factions.values();
    }

    public void removeFaction(String factionId) {
        factions.remove(factionId);
    }

    // ========================================================== Sauvegarde

    /** Copie sérialisée des données utiles (thread principal) : les anciennes périodes sont oubliées. */
    public DataSnapshot snapshot() {
        DataSnapshot snapshot = new DataSnapshot();
        long current = cycle.getCycle();
        for (PlayerData pd : players.values()) {
            if (!pd.isDisposable(cycle.getPlayerCycle())) {
                snapshot.players.put(pd.getId(), gson.toJson(pd));
            }
        }
        for (FactionData fd : factions.values()) {
            if (fd.getCycle() == current && !fd.getQuests().isEmpty()) {
                snapshot.factions.put(fd.getFactionId(), gson.toJson(fd));
            }
        }
        snapshot.meta.put(CYCLE_KEY, gson.toJson(cycle));
        return snapshot;
    }

    public void saveAsync() {
        if (!loaded || executor == null || executor.isShutdown()) {
            return;
        }
        final DataSnapshot snapshot = snapshot();
        executor.submit(() -> write(snapshot));
    }

    private void write(DataSnapshot snapshot) {
        try {
            storage.save(snapshot);
            if (plugin.settings().debug) {
                plugin.getLogger().info("Sauvegarde effectuée.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Échec de la sauvegarde des données", e);
        }
    }

    public void startAutoSave() {
        stopAutoSave();
        int minutes = plugin.settings().autoSaveMinutes;
        if (minutes <= 0) {
            return;
        }
        long period = minutes * 60L * 20L;
        autoSave = Bukkit.getScheduler().runTaskTimer(plugin, this::saveAsync, period, period);
    }

    public void stopAutoSave() {
        if (autoSave != null) {
            autoSave.cancel();
            autoSave = null;
        }
    }

    /** Sauvegarde synchrone finale et fermeture du stockage. */
    public void shutdown() {
        stopAutoSave();
        if (storage == null) {
            return;
        }
        if (!loaded) {
            storage.close();
            return;
        }
        DataSnapshot snapshot = snapshot();
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Des sauvegardes asynchrones n'ont pas pu se terminer à temps.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        write(snapshot);
        storage.close();
    }
}
