package fr.koriamc.quest.api;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.model.FactionData;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.objective.ObjectiveType;
import fr.koriamc.quest.objective.ProgressContext;
import org.bukkit.entity.Player;

/**
 * Accès pour les autres plugins KoriaMC (métiers, événements...).
 * <pre>
 * // au démarrage : un nouveau type utilisable dans les fichiers de quêtes
 * KoriaQuestAPI.registerObjectiveType("GAGNER_EVENEMENT", ObjectiveType.Targets.NAMES);
 * // quand l'action a lieu :
 * KoriaQuestAPI.progress(ProgressContext.of(player, "GAGNER_EVENEMENT", 1).key("KOTH"));
 * </pre>
 */
public final class KoriaQuestAPI {

    private KoriaQuestAPI() {
    }

    private static KoriaQuest plugin() {
        KoriaQuest plugin = KoriaQuest.get();
        if (plugin == null) {
            throw new IllegalStateException("KoriaQuest n'est pas activé");
        }
        return plugin;
    }

    public static boolean isAvailable() {
        return KoriaQuest.get() != null;
    }

    /**
     * Déclare un type d'objectif. Les quêtes qui l'utilisent sont ignorées tant
     * qu'il n'existe pas : faites /quests reload après l'enregistrement si besoin.
     */
    public static void registerObjectiveType(String id, ObjectiveType.Targets targets) {
        plugin().objectives().register(new ObjectiveType(id, targets));
    }

    /** Signale une action (fait avancer les quêtes du joueur et de sa faction qui l'utilisent). */
    public static void progress(ProgressContext context) {
        plugin().progress().handle(context);
    }

    /** Difficulté choisie pour la période en cours (jour par défaut), ou null. */
    public static String getDifficulty(Player player) {
        return plugin().quests().player(player).getDifficulty();
    }

    public static PlayerData getPlayerData(Player player) {
        return plugin().quests().player(player);
    }

    /** Quêtes de la semaine d'une faction (identifiant KoriaFactions), ou null si elle n'en a pas. */
    public static FactionData getFactionData(String factionId) {
        return plugin().quests().isEligible(factionId) ? plugin().quests().faction(factionId) : null;
    }

    /** Millisecondes avant les nouvelles quêtes joueur. */
    public static long getPlayerTimeLeft() {
        return plugin().cycles().playerTimeLeft();
    }

    /** Millisecondes avant les nouvelles quêtes de faction. */
    public static long getFactionTimeLeft() {
        return plugin().cycles().factionTimeLeft();
    }
}
