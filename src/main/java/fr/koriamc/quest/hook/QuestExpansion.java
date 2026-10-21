package fr.koriamc.quest.hook;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.model.FactionData;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestProgress;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Variables PlaceholderAPI :
 * <ul>
 *   <li>%koriaquest_difficulte%, %koriaquest_terminees%, %koriaquest_total%, %koriaquest_temps_joueur%, %koriaquest_temps_faction%</li>
 *   <li>%koriaquest_faction_terminees%, %koriaquest_faction_total%</li>
 *   <li>%koriaquest_quete_&lt;n&gt;_&lt;nom|progression|objectif|pourcentage|termine&gt;%</li>
 *   <li>%koriaquest_faction_quete_&lt;n&gt;_&lt;nom|progression|objectif|pourcentage|termine&gt;%</li>
 * </ul>
 */
final class QuestExpansion extends PlaceholderExpansion {

    private final KoriaQuest plugin;

    QuestExpansion(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "koriaquest";
    }

    @Override
    public String getAuthor() {
        return "KoriaMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.equals("temps_joueur")) {
            return plugin.msg().duration(plugin.cycles().playerTimeLeft());
        }
        if (key.equals("temps_faction")) {
            return plugin.msg().duration(plugin.cycles().factionTimeLeft());
        }
        if (player == null) {
            return "";
        }
        PlayerData pd = plugin.quests().player(player);
        String factionId = plugin.factionHook().getFactionId(player);
        FactionData fd = plugin.quests().isEligible(factionId) ? plugin.quests().faction(factionId) : null;
        List<QuestProgress> factionQuests = fd == null ? Collections.<QuestProgress>emptyList() : fd.getQuests();
        switch (key) {
            case "difficulte":
                return pd.hasChosen() ? Text.color(plugin.settings().difficultyName(pd.getDifficulty())) : plugin.msg().text("values.none");
            case "terminees":
                return String.valueOf(pd.countDone());
            case "total":
                return String.valueOf(pd.getQuests().size());
            case "faction_terminees":
                return fd == null ? "0" : String.valueOf(fd.countDone());
            case "faction_total":
                return String.valueOf(factionQuests.size());
            default:
                break;
        }
        if (key.startsWith("faction_quete_")) {
            return quest(QuestScope.FACTION, factionQuests, key.substring("faction_quete_".length()));
        }
        if (key.startsWith("quete_")) {
            return quest(QuestScope.JOUEUR, pd.getQuests(), key.substring("quete_".length()));
        }
        return null;
    }

    /** "&lt;n&gt;_&lt;champ&gt;" */
    private String quest(QuestScope scope, List<QuestProgress> quests, String rest) {
        int underscore = rest.indexOf('_');
        Integer index = underscore < 0 ? null : Text.parseInt(rest.substring(0, underscore));
        if (index == null || index < 1 || index > quests.size()) {
            return "";
        }
        QuestProgress q = quests.get(index - 1);
        QuestDefinition def = plugin.catalog().get(scope, q.getQuest());
        switch (rest.substring(underscore + 1)) {
            case "nom":
                return def == null ? "" : Text.color(def.name);
            case "progression":
                return String.valueOf(q.getProgress());
            case "objectif":
                return String.valueOf(q.getTarget());
            case "pourcentage":
                return plugin.view().percent(q);
            case "termine":
                return String.valueOf(q.isDone());
            default:
                return null;
        }
    }
}
