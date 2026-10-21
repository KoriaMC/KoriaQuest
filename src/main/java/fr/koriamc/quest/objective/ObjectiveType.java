package fr.koriamc.quest.objective;

import fr.koriamc.quest.model.QuestDefinition;

import java.util.Locale;

/**
 * Type d'objectif (CASSER_BLOC, TUER_MOB...). Il indique seulement comment lire
 * les "cibles" d'une quête ; ce sont les listeners (ou d'autres plugins via
 * l'API) qui signalent les actions avec un {@link ProgressContext}.
 * <p>
 * Ajouter un type : l'enregistrer dans {@link ObjectiveRegistry} (ou
 * KoriaQuestAPI.registerObjectiveType) puis appeler ProgressService.handle
 * quand l'action a lieu.
 */
public final class ObjectiveType {

    /** Nature des cibles d'une quête de ce type. */
    public enum Targets {
        /** Pas de cible (distance, temps de jeu...) : toutes les actions comptent. */
        NONE,
        /** Matériaux : nom 1.8.8, ID ou ID:data. */
        MATERIALS,
        /** Noms libres comparés sans tenir compte de la casse (types d'entités, relations...). */
        NAMES
    }

    private final String id;
    private final Targets targets;
    /** Progression par livraison d'objets depuis le menu, et non par un évènement. */
    private final boolean delivery;

    public ObjectiveType(String id, Targets targets) {
        this(id, targets, false);
    }

    public ObjectiveType(String id, Targets targets, boolean delivery) {
        this.id = id.toUpperCase(Locale.ROOT);
        this.targets = targets;
        this.delivery = delivery;
    }

    public String getId() {
        return id;
    }

    public Targets getTargets() {
        return targets;
    }

    public boolean isDelivery() {
        return delivery;
    }

    /** L'action correspond-elle aux cibles de la quête ? Une quête sans cible accepte tout. */
    public boolean matches(QuestDefinition def, ProgressContext ctx) {
        switch (targets) {
            case MATERIALS:
                return def.materials.isEmpty() || (ctx.getMaterialId() >= 0
                        && def.materials.contains(ctx.getMaterialId(), Math.max(0, ctx.getMaterialData())));
            case NAMES:
                return def.names.isEmpty() || (ctx.getKey() != null && def.names.contains(ctx.getKey().toUpperCase(Locale.ROOT)));
            default:
                return true;
        }
    }
}
