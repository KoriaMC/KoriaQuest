package fr.koriamc.quest.config;

import org.bukkit.configuration.ConfigurationSection;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Réglages typés de config.json, relus à chaque rechargement.
 */
public final class Settings {

    public boolean debug;

    // Stockage
    public String storageType;
    public int autoSaveMinutes;
    public int jsonBackups;
    public boolean jsonPretty;
    public String sqliteFile;
    public String mysqlHost;
    public int mysqlPort;
    public String mysqlDatabase;
    public String mysqlUser;
    public String mysqlPassword;
    public String mysqlPrefix;
    public String mysqlParameters;

    // Cycle hebdomadaire
    public DayOfWeek resetDay;
    public LocalTime resetTime;
    public ZoneId zone;
    public boolean announceNewWeek;
    public boolean remindUnfinished;

    // Cycle des quêtes joueur
    /** true : renouvellement chaque jour à {@link #playerResetTime} ; false : chaque semaine, comme les factions. */
    public boolean playerDaily;
    public LocalTime playerResetTime;
    public boolean announceNewPlayerPeriod;

    /** Difficultés dans l'ordre de config.json : id -> nom affiché. */
    public final Map<String, String> difficulties = new LinkedHashMap<String, String>();
    /** Nom tapé dans les commandes (easy) -> identifiant de la difficulté (facile). */
    public final Map<String, String> difficultyArguments = new LinkedHashMap<String, String>();

    // Quêtes de faction
    public boolean factionEnabled;
    public final Map<String, Integer> factionCounts = new LinkedHashMap<String, Integer>();
    public int factionMinMembers;
    public boolean factionNotify;
    public boolean factionBroadcast;

    // Quêtes joueur
    public boolean playerEnabled;
    public int playerCount;
    public boolean playerCanChange;
    public boolean playerConfirm;
    /** BLOQUER ou AUTORISER. */
    public boolean playerRequiresFaction;
    public boolean playerAutoOpenChoice;

    // Anti-abus
    public final Set<String> disabledWorlds = new HashSet<String>();
    public boolean ignoreCreative;
    public boolean ignorePlacedBlocks;
    public int placedBlocksMax;
    public boolean ignoreSpawnerMobs;
    public boolean ignoreEggMobs;
    public int sameVictimMinutes;
    public boolean ignoreSameIp;
    public int afkMinutes;
    public boolean travelInVehicle;

    // Notifications
    public boolean progressActionBar;
    public final List<Integer> milestones = new ArrayList<Integer>();
    private final Map<String, String> sounds = new HashMap<String, String>();

    // Affichage
    public int barLength;
    public String barSymbol;
    public String barFull;
    public String barEmpty;
    public int contributorsShown;
    public int percentDecimals;

    // Titres
    public int titleFadeIn;
    public int titleStay;
    public int titleFadeOut;

    public void load(ConfigurationSection c, Logger logger) {
        debug = c.getBoolean("debug");

        storageType = str(c, "stockage.type", "JSON").toUpperCase(Locale.ROOT);
        autoSaveMinutes = c.getInt("stockage.sauvegarde-auto-minutes");
        jsonBackups = c.getInt("stockage.copies-de-securite");
        jsonPretty = c.getBoolean("stockage.json-indente");
        sqliteFile = str(c, "stockage.sqlite-fichier", "donnees.db");
        mysqlHost = str(c, "stockage.mysql.hote", "localhost");
        mysqlPort = c.getInt("stockage.mysql.port", 3306);
        mysqlDatabase = str(c, "stockage.mysql.base", "koriaquest");
        mysqlUser = str(c, "stockage.mysql.utilisateur", "root");
        mysqlPassword = str(c, "stockage.mysql.mot-de-passe", "");
        mysqlPrefix = str(c, "stockage.mysql.prefixe-tables", "kq_");
        mysqlParameters = str(c, "stockage.mysql.parametres", "");

        resetDay = parseDay(c.getString("cycle.jour"), logger);
        resetTime = parseTime(c.getString("cycle.heure"), logger);
        try {
            zone = ZoneId.of(str(c, "cycle.fuseau", "Europe/Paris"));
        } catch (DateTimeException e) {
            logger.warning("Fuseau horaire inconnu dans cycle.fuseau : " + c.getString("cycle.fuseau") + " (Europe/Paris utilisé)");
            zone = ZoneId.of("Europe/Paris");
        }
        announceNewWeek = c.getBoolean("cycle.annonce-nouvelle-semaine");
        remindUnfinished = c.getBoolean("cycle.rappel-quetes-non-terminees");
        String period = str(c, "cycle-joueur.periode", "QUOTIDIEN").trim().toUpperCase(Locale.ROOT);
        if (!period.equals("QUOTIDIEN") && !period.equals("HEBDOMADAIRE")) {
            logger.warning("Période inconnue dans cycle-joueur.periode : " + period + " (QUOTIDIEN utilisé)");
        }
        playerDaily = !period.equals("HEBDOMADAIRE");
        playerResetTime = parseTime(c.getString("cycle-joueur.heure"), logger);
        announceNewPlayerPeriod = c.getBoolean("cycle-joueur.annonce-renouvellement");

        difficulties.clear();
        difficultyArguments.clear();
        ConfigurationSection diffs = c.getConfigurationSection("difficultes");
        if (diffs != null) {
            for (String id : diffs.getKeys(false)) {
                String key = id.toLowerCase(Locale.ROOT);
                difficulties.put(key, str(diffs, id + ".nom", id));
                difficultyArguments.put(str(diffs, id + ".argument", id).trim().toLowerCase(Locale.ROOT), key);
            }
        }
        if (difficulties.isEmpty()) {
            logger.warning("Aucune difficulté dans config.json (difficultes) : facile, moyen et difficile utilisées.");
            difficulties.put("facile", "&aFacile");
            difficulties.put("moyen", "&6Moyen");
            difficulties.put("difficile", "&cDifficile");
            difficultyArguments.put("easy", "facile");
            difficultyArguments.put("medium", "moyen");
            difficultyArguments.put("hard", "difficile");
        }

        factionEnabled = c.getBoolean("quetes-faction.actif");
        factionCounts.clear();
        for (String id : difficulties.keySet()) {
            factionCounts.put(id, Math.max(0, c.getInt("quetes-faction.nombre." + id, 0)));
        }
        factionMinMembers = Math.max(1, c.getInt("quetes-faction.membres-minimum", 1));
        factionNotify = c.getBoolean("quetes-faction.prevenir-la-faction");
        factionBroadcast = c.getBoolean("quetes-faction.annonce-globale");

        playerEnabled = c.getBoolean("quetes-joueur.actif");
        playerCount = Math.max(1, c.getInt("quetes-joueur.nombre", 3));
        playerCanChange = c.getBoolean("quetes-joueur.changer-difficulte");
        playerConfirm = c.getBoolean("quetes-joueur.confirmation-choix");
        playerRequiresFaction = !"AUTORISER".equalsIgnoreCase(c.getString("quetes-joueur.sans-faction"));
        playerAutoOpenChoice = c.getBoolean("quetes-joueur.ouvrir-choix-automatiquement");

        disabledWorlds.clear();
        for (String world : c.getStringList("anti-abus.mondes-desactives")) {
            disabledWorlds.add(world.toLowerCase(Locale.ROOT));
        }
        ignoreCreative = c.getBoolean("anti-abus.ignorer-creatif");
        ignorePlacedBlocks = c.getBoolean("anti-abus.ignorer-blocs-poses");
        placedBlocksMax = Math.max(1000, c.getInt("anti-abus.blocs-poses-memoire-max", 200000));
        ignoreSpawnerMobs = c.getBoolean("anti-abus.ignorer-mobs-spawner");
        ignoreEggMobs = c.getBoolean("anti-abus.ignorer-mobs-oeufs");
        sameVictimMinutes = Math.max(0, c.getInt("anti-abus.delai-meme-victime-minutes"));
        ignoreSameIp = c.getBoolean("anti-abus.ignorer-meme-ip");
        afkMinutes = Math.max(0, c.getInt("anti-abus.afk-minutes"));
        travelInVehicle = c.getBoolean("anti-abus.parcourir-en-vehicule");

        progressActionBar = c.getBoolean("notifications.progression-actionbar");
        milestones.clear();
        for (Integer pct : c.getIntegerList("notifications.paliers-pourcentage")) {
            if (pct != null && pct > 0 && pct < 100) {
                milestones.add(pct);
            }
        }
        sounds.clear();
        ConfigurationSection soundSection = c.getConfigurationSection("notifications.sons");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                sounds.put(key, soundSection.getString(key, ""));
            }
        }

        barLength = Math.max(1, c.getInt("affichage.barre.longueur", 20));
        barSymbol = str(c, "affichage.barre.symbole", "|");
        barFull = str(c, "affichage.barre.couleur-pleine", "&a");
        barEmpty = str(c, "affichage.barre.couleur-vide", "&8");
        contributorsShown = Math.max(0, c.getInt("affichage.contributeurs-affiches", 5));
        percentDecimals = Math.max(0, Math.min(2, c.getInt("affichage.decimales-pourcentage")));

        titleFadeIn = c.getInt("titres.apparition", 5);
        titleStay = c.getInt("titres.duree", 40);
        titleFadeOut = c.getInt("titres.disparition", 10);
    }

    /** Son de notifications.sons (vide si absent). */
    public String sound(String key) {
        String value = sounds.get(key);
        return value == null ? "" : value;
    }

    public boolean isWorldDisabled(String world) {
        return world != null && disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }

    public boolean isDifficulty(String id) {
        return id != null && difficulties.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /** Identifiant de la difficulté tapée dans une commande (easy -> facile), ou le texte tel quel. */
    public String difficultyFromArgument(String argument) {
        if (argument == null) {
            return null;
        }
        String id = difficultyArguments.get(argument.trim().toLowerCase(Locale.ROOT));
        return id != null ? id : argument;
    }

    public String difficultyName(String id) {
        String name = id == null ? null : difficulties.get(id.toLowerCase(Locale.ROOT));
        return name == null ? (id == null ? "" : id) : name;
    }

    private static String str(ConfigurationSection c, String path, String def) {
        String value = c.getString(path);
        return value == null ? def : value;
    }

    private static final String[] FRENCH_DAYS = {"LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI", "DIMANCHE"};

    private static DayOfWeek parseDay(String value, Logger logger) {
        String v = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < FRENCH_DAYS.length; i++) {
            if (FRENCH_DAYS[i].equals(v)) {
                return DayOfWeek.of(i + 1);
            }
        }
        try {
            return DayOfWeek.valueOf(v);
        } catch (IllegalArgumentException e) {
            logger.warning("Jour inconnu dans cycle.jour : " + value + " (LUNDI utilisé)");
            return DayOfWeek.MONDAY;
        }
    }

    private static LocalTime parseTime(String value, Logger logger) {
        try {
            return LocalTime.parse(value == null ? "00:00" : value.trim().length() == 4 ? "0" + value.trim() : value.trim());
        } catch (DateTimeException e) {
            logger.warning("Heure invalide dans cycle.heure : " + value + " (00:00 utilisé, format HH:mm)");
            return LocalTime.MIDNIGHT;
        }
    }
}
