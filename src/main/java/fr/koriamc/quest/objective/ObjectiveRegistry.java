package fr.koriamc.quest.objective;

import fr.koriamc.quest.objective.ObjectiveType.Targets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Types d'objectifs connus : ceux du plugin, plus ceux déclarés par d'autres
 * plugins (KoriaQuestAPI.registerObjectiveType).
 */
public final class ObjectiveRegistry {

    public static final String BREAK_BLOCK = "CASSER_BLOC";
    public static final String PLACE_BLOCK = "POSER_BLOC";
    public static final String HARVEST = "RECOLTER";
    public static final String KILL_MOB = "TUER_MOB";
    public static final String KILL_PLAYER = "TUER_JOUEUR";
    public static final String DEAL_DAMAGE = "INFLIGER_DEGATS";
    public static final String FISH = "PECHER";
    public static final String CRAFT = "CRAFTER";
    public static final String SMELT = "FONDRE";
    public static final String ENCHANT = "ENCHANTER";
    public static final String BREW = "BRASSER";
    public static final String CONSUME = "CONSOMMER";
    public static final String BREED = "ELEVER";
    public static final String TAME = "APPRIVOISER";
    public static final String SHEAR = "TONDRE";
    public static final String TRAVEL = "PARCOURIR";
    public static final String PLAYTIME = "TEMPS_EN_LIGNE";
    public static final String GAIN_XP = "GAGNER_EXPERIENCE";
    public static final String LEVEL_UP = "MONTER_NIVEAU";
    public static final String DELIVER = "LIVRER";
    public static final String CLAIM = "CLAIMER";

    private final Map<String, ObjectiveType> types = new LinkedHashMap<String, ObjectiveType>();

    public ObjectiveRegistry() {
        register(new ObjectiveType(BREAK_BLOCK, Targets.MATERIALS));
        register(new ObjectiveType(PLACE_BLOCK, Targets.MATERIALS));
        register(new ObjectiveType(HARVEST, Targets.MATERIALS));
        register(new ObjectiveType(KILL_MOB, Targets.NAMES));
        register(new ObjectiveType(KILL_PLAYER, Targets.NAMES));
        register(new ObjectiveType(DEAL_DAMAGE, Targets.NAMES));
        register(new ObjectiveType(FISH, Targets.MATERIALS));
        register(new ObjectiveType(CRAFT, Targets.MATERIALS));
        register(new ObjectiveType(SMELT, Targets.MATERIALS));
        register(new ObjectiveType(ENCHANT, Targets.MATERIALS));
        register(new ObjectiveType(BREW, Targets.MATERIALS));
        register(new ObjectiveType(CONSUME, Targets.MATERIALS));
        register(new ObjectiveType(BREED, Targets.NAMES));
        register(new ObjectiveType(TAME, Targets.NAMES));
        register(new ObjectiveType(SHEAR, Targets.NAMES));
        register(new ObjectiveType(TRAVEL, Targets.NONE));
        register(new ObjectiveType(PLAYTIME, Targets.NONE));
        register(new ObjectiveType(GAIN_XP, Targets.NONE));
        register(new ObjectiveType(LEVEL_UP, Targets.NONE));
        register(new ObjectiveType(DELIVER, Targets.MATERIALS, true));
        register(new ObjectiveType(CLAIM, Targets.NONE));
    }

    /** Ajoute (ou remplace) un type. */
    public void register(ObjectiveType type) {
        types.put(type.getId(), type);
    }

    public ObjectiveType get(String id) {
        return id == null ? null : types.get(id.toUpperCase(Locale.ROOT));
    }

    public Collection<ObjectiveType> getAll() {
        return Collections.unmodifiableCollection(types.values());
    }

    public List<String> ids() {
        return new ArrayList<String>(types.keySet());
    }
}
