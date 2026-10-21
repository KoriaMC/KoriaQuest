package fr.koriamc.quest.config;

import fr.koriamc.quest.util.Items;
import fr.koriamc.quest.util.Text;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Récompenses de rewards.json : par quête terminée (selon la difficulté) et par
 * palier (nombre de quêtes terminées dans la semaine).
 */
public final class RewardSettings {

    /** Joueurs d'une faction qui reçoivent les récompenses personnelles d'une quête de faction. */
    public enum Target {
        /** Membres actuels ayant fait avancer la quête (ou une quête de la semaine pour un palier). */
        CONTRIBUTEURS,
        /** Membres connectés au moment de la réussite. */
        MEMBRES_EN_LIGNE,
        /** Tous les membres ; les absents reçoivent la récompense à leur connexion. */
        MEMBRES,
        /** Personne : seuls les points et les annonces sont donnés. */
        AUCUNE
    }

    /** Un ensemble de récompenses. */
    public static final class Block {
        public final Target target;
        public final List<String> rewards;
        /** Texte affiché dans les menus (vide = généré depuis les récompenses). */
        public final List<String> text;

        Block(Target target, List<String> rewards, List<String> text) {
            this.target = target;
            this.rewards = Collections.unmodifiableList(rewards);
            this.text = Collections.unmodifiableList(text);
        }
    }

    private static final Block EMPTY = new Block(Target.AUCUNE, new ArrayList<String>(), new ArrayList<String>());

    private final Map<String, Block> factionPerQuest = new TreeMap<String, Block>();
    private final TreeMap<Integer, Block> factionTiers = new TreeMap<Integer, Block>();
    private final Map<String, Block> playerPerQuest = new TreeMap<String, Block>();
    private final Map<String, TreeMap<Integer, Block>> playerTiers = new TreeMap<String, TreeMap<Integer, Block>>();
    private final List<String> problems = new ArrayList<String>();

    public void load(ConfigurationSection c) {
        factionPerQuest.clear();
        factionTiers.clear();
        playerPerQuest.clear();
        playerTiers.clear();
        problems.clear();
        ConfigurationSection perQuest = c.getConfigurationSection("faction.par-quete");
        if (perQuest != null) {
            for (String diff : perQuest.getKeys(false)) {
                factionPerQuest.put(diff.toLowerCase(Locale.ROOT), block(perQuest.getConfigurationSection(diff), "faction.par-quete." + diff));
            }
        }
        readTiers(c.getConfigurationSection("faction.paliers"), factionTiers, "faction.paliers");
        ConfigurationSection player = c.getConfigurationSection("joueur");
        if (player != null) {
            for (String diff : player.getKeys(false)) {
                ConfigurationSection d = player.getConfigurationSection(diff);
                if (d == null) {
                    continue;
                }
                String id = diff.toLowerCase(Locale.ROOT);
                playerPerQuest.put(id, block(d.getConfigurationSection("par-quete"), "joueur." + diff + ".par-quete"));
                TreeMap<Integer, Block> tiers = new TreeMap<Integer, Block>();
                readTiers(d.getConfigurationSection("paliers"), tiers, "joueur." + diff + ".paliers");
                playerTiers.put(id, tiers);
            }
        }
    }

    private void readTiers(ConfigurationSection s, Map<Integer, Block> target, String where) {
        if (s == null) {
            return;
        }
        for (String key : s.getKeys(false)) {
            Integer count = Text.parseInt(key);
            if (count == null || count <= 0) {
                problems.add("rewards.json : " + where + "." + key + " doit être un nombre de quêtes (ex : \"3\")");
                continue;
            }
            target.put(count, block(s.getConfigurationSection(key), where + "." + key));
        }
    }

    private Block block(ConfigurationSection s, String where) {
        if (s == null) {
            return EMPTY;
        }
        Target target = Target.CONTRIBUTEURS;
        String raw = s.getString("cible");
        if (raw != null) {
            try {
                target = Target.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                problems.add("rewards.json : " + where + ".cible inconnue '" + raw + "'");
            }
        }
        return new Block(target, Items.stringList(s, "recompenses"), Items.stringList(s, "texte"));
    }

    public Block factionPerQuest(String difficulty) {
        Block b = factionPerQuest.get(difficulty);
        return b == null ? EMPTY : b;
    }

    public TreeMap<Integer, Block> factionTiers() {
        return factionTiers;
    }

    public Block playerPerQuest(String difficulty) {
        Block b = difficulty == null ? null : playerPerQuest.get(difficulty);
        return b == null ? EMPTY : b;
    }

    public TreeMap<Integer, Block> playerTiers(String difficulty) {
        TreeMap<Integer, Block> tiers = difficulty == null ? null : playerTiers.get(difficulty);
        return tiers == null ? new TreeMap<Integer, Block>() : tiers;
    }

    /** Palier affiché comme "bonus" : celui du nombre de quêtes, sinon le plus haut. */
    public static Block mainTier(TreeMap<Integer, Block> tiers, int total) {
        if (tiers.isEmpty()) {
            return EMPTY;
        }
        Block exact = tiers.get(total);
        if (exact != null) {
            return exact;
        }
        return tiers.lastEntry().getValue();
    }

    public List<String> getProblems() {
        return problems;
    }
}
