package fr.koriamc.quest.model;

import fr.koriamc.quest.util.MaterialSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Une quête telle qu'écrite dans quests_faction.json ou quests_player.json.
 * Immuable : un rechargement recrée toutes les définitions.
 */
public final class QuestDefinition {

    public final QuestScope scope;
    public final String difficulty;
    public final String id;
    public final String name;
    /** Identifiant du type d'objectif (CASSER_BLOC, TUER_MOB...). */
    public final String type;
    /** Cibles telles qu'écrites (matériaux, entités, relations...). */
    public final List<String> targets;
    /** Cibles lues comme matériaux (types à matériaux). */
    public final MaterialSet materials;
    /** Cibles en majuscules (types à noms). */
    public final Set<String> names;
    public final long objective;
    /** Objectif ajouté par membre de la faction (quêtes de faction). */
    public final long perMember;
    /** Mondes autorisés en minuscules (vide = tous). */
    public final Set<String> worlds;
    /** Zones autorisées (WILDERNESS, SAFEZONE, WARZONE, OWN, ALLY, ENEMY ; vide = toutes). */
    public final Set<String> zones;
    public final int weight;
    public final String icon;
    public final List<String> description;
    /** Récompenses propres à la quête (vide = celles de la difficulté dans rewards.json). */
    public final List<String> rewards;
    /** Outils à tenir en main pour progresser (vide = n'importe lequel). */
    public final MaterialSet tools;

    public QuestDefinition(QuestScope scope, String difficulty, String id, String name, String type, List<String> targets,
                           MaterialSet materials, Set<String> names, long objective, long perMember, Set<String> worlds,
                           Set<String> zones, int weight, String icon, List<String> description, List<String> rewards,
                           MaterialSet tools) {
        this.scope = scope;
        this.difficulty = difficulty;
        this.id = id;
        this.name = name;
        this.type = type;
        this.targets = Collections.unmodifiableList(targets);
        this.materials = materials;
        this.names = Collections.unmodifiableSet(names);
        this.objective = objective;
        this.perMember = perMember;
        this.worlds = Collections.unmodifiableSet(worlds);
        this.zones = Collections.unmodifiableSet(zones);
        this.weight = weight;
        this.icon = icon;
        this.description = Collections.unmodifiableList(description);
        this.rewards = Collections.unmodifiableList(rewards);
        this.tools = tools;
    }

    /** Clé unique "difficulte.id", enregistrée dans la progression. */
    public String key() {
        return key(difficulty, id);
    }

    public static String key(String difficulty, String id) {
        return difficulty + "." + id;
    }

    /** Objectif à atteindre pour une faction de {@code members} membres (1 minimum). */
    public long targetFor(int members) {
        return Math.max(1, objective + perMember * Math.max(0, members));
    }
}
