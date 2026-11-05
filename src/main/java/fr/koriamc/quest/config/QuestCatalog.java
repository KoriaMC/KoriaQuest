package fr.koriamc.quest.config;

import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.objective.ObjectiveRegistry;
import fr.koriamc.quest.objective.ObjectiveType;
import fr.koriamc.quest.util.Items;
import fr.koriamc.quest.util.MaterialSet;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Quêtes de quests_faction.json et quests_player.json, classées par niveau et
 * par difficulté. Les erreurs (type inconnu, objectif nul...) sont remontées au
 * rechargement et la quête fautive est ignorée.
 */
public final class QuestCatalog {

    private static final Set<String> ZONES = new HashSet<String>(Arrays.asList(
            "WILDERNESS", "SAFEZONE", "WARZONE", "OWN", "ALLY", "ENEMY"));

    private final Map<QuestScope, Map<String, QuestDefinition>> byKey = new HashMap<QuestScope, Map<String, QuestDefinition>>();
    private final Set<String> usedTypes = new HashSet<String>();
    private final List<String> problems = new ArrayList<String>();

    public void load(ConfigurationSection faction, ConfigurationSection player, Settings settings, ObjectiveRegistry registry) {
        byKey.clear();
        usedTypes.clear();
        problems.clear();
        byKey.put(QuestScope.FACTION, read(QuestScope.FACTION, faction, "quests_faction.json", settings, registry));
        byKey.put(QuestScope.JOUEUR, read(QuestScope.JOUEUR, player, "quests_player.json", settings, registry));
    }

    private Map<String, QuestDefinition> read(QuestScope scope, ConfigurationSection root, String file, Settings settings,
                                              ObjectiveRegistry registry) {
        Map<String, QuestDefinition> map = new LinkedHashMap<String, QuestDefinition>();
        for (String difficulty : root.getKeys(false)) {
            if (!settings.isDifficulty(difficulty)) {
                problems.add(file + " : difficulté '" + difficulty + "' absente de config.json (difficultes), section ignorée");
                continue;
            }
            ConfigurationSection quests = root.getConfigurationSection(difficulty);
            if (quests == null) {
                continue;
            }
            for (String id : quests.getKeys(false)) {
                ConfigurationSection q = quests.getConfigurationSection(id);
                if (q == null || !q.getBoolean("actif", true)) {
                    continue;
                }
                QuestDefinition def = parse(scope, difficulty.toLowerCase(Locale.ROOT), id, q, file, registry);
                if (def != null) {
                    map.put(def.key(), def);
                    usedTypes.add(def.type);
                }
            }
        }
        return map;
    }

    private QuestDefinition parse(QuestScope scope, String difficulty, String id, ConfigurationSection q, String file,
                                  ObjectiveRegistry registry) {
        String where = file + " : " + difficulty + "." + id;
        ObjectiveType type = registry.get(q.getString("type"));
        if (type == null) {
            problems.add(where + " : type d'objectif inconnu '" + q.getString("type") + "'");
            return null;
        }
        long objective = q.getLong("objectif");
        if (objective <= 0) {
            problems.add(where + " : objectif doit être supérieur à 0");
            return null;
        }
        List<String> targets = Items.stringList(q, "cibles");
        MaterialSet materials = MaterialSet.of(Collections.<String>emptyList(), null, where);
        Set<String> names = new HashSet<String>();
        if (type.getTargets() == ObjectiveType.Targets.MATERIALS) {
            List<String> bad = new ArrayList<String>();
            for (String t : targets) {
                if (MaterialSet.parse(t) == null) {
                    bad.add(t);
                }
            }
            if (!bad.isEmpty()) {
                problems.add(where + " : matériau(x) inconnu(s) " + bad);
            }
            materials = MaterialSet.of(targets, null, where);
            if (!targets.isEmpty() && materials.isEmpty()) {
                return null;
            }
        } else {
            for (String t : targets) {
                names.add(t.trim().toUpperCase(Locale.ROOT));
            }
        }
        Set<String> worlds = new HashSet<String>();
        for (String w : q.getStringList("mondes")) {
            worlds.add(w.toLowerCase(Locale.ROOT));
        }
        Set<String> zones = new HashSet<String>();
        for (String z : q.getStringList("zones")) {
            String zone = z.trim().toUpperCase(Locale.ROOT);
            if (ZONES.contains(zone)) {
                zones.add(zone);
            } else {
                problems.add(where + " : zone inconnue '" + z + "' (WILDERNESS, SAFEZONE, WARZONE, OWN, ALLY, ENEMY)");
            }
        }
        List<String> toolNames = Items.stringList(q, "outils");
        for (String t : toolNames) {
            if (MaterialSet.parse(t) == null) {
                problems.add(where + " : outil inconnu '" + t + "'");
            }
        }
        MaterialSet tools = MaterialSet.of(toolNames, null, where);
        long perMember = scope == QuestScope.FACTION ? Math.max(0, q.getLong("par-membre")) : 0;
        return new QuestDefinition(scope, difficulty, id, q.getString("nom", id), type.getId(), targets, materials, names,
                objective, perMember, worlds, zones, Math.max(1, q.getInt("poids", 1)), q.getString("icone", "PAPER"),
                Items.stringList(q, "description"), Items.stringList(q, "recompenses"), tools);
    }

    public QuestDefinition get(QuestScope scope, String key) {
        Map<String, QuestDefinition> map = byKey.get(scope);
        return map == null || key == null ? null : map.get(key);
    }

    /** Quêtes actives d'une difficulté. */
    public List<QuestDefinition> list(QuestScope scope, String difficulty) {
        List<QuestDefinition> out = new ArrayList<QuestDefinition>();
        Map<String, QuestDefinition> map = byKey.get(scope);
        if (map == null) {
            return out;
        }
        for (QuestDefinition def : map.values()) {
            if (def.difficulty.equalsIgnoreCase(difficulty)) {
                out.add(def);
            }
        }
        return out;
    }

    public int count() {
        int n = 0;
        for (Map<String, QuestDefinition> map : byKey.values()) {
            n += map.size();
        }
        return n;
    }

    /** Au moins une quête utilise ce type (évite de traiter des évènements fréquents pour rien). */
    public boolean usesType(String type) {
        return usedTypes.contains(type);
    }

    public List<String> getProblems() {
        return problems;
    }
}
