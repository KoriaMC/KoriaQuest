package fr.koriamc.quest.progress;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.config.RewardSettings;
import fr.koriamc.quest.config.Settings;
import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestProgress;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.util.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Variables d'affichage d'une quête, communes aux menus, aux messages et à PlaceholderAPI :
 * {quete}, {description}, {progression}, {objectif}, {pourcentage}, {barre},
 * {difficulte}, {icone}, {points}, {recompenses}, {type}.
 */
public final class QuestView {

    private final KoriaQuest plugin;

    public QuestView(KoriaQuest plugin) {
        this.plugin = plugin;
    }

    public Object[] vars(QuestDefinition def, QuestProgress q) {
        List<String> description = new ArrayList<String>();
        for (String line : def.description) {
            description.add(Text.replace(line, "objectif", Text.number(q.getTarget(), 0)));
        }
        List<String> rewards = rewards(def);
        return new Object[]{
                "quete", def.name,
                "description", description,
                "progression", Text.number(q.getProgress(), 0),
                "objectif", Text.number(q.getTarget(), 0),
                "restant", Text.number(q.getTarget() - q.getProgress(), 0),
                "pourcentage", percent(q),
                "barre", bar(q),
                "difficulte", plugin.settings().difficultyName(def.difficulty),
                "icone", def.icon,
                "type", def.type,
                "points", Text.number(points(def), 0),
                "recompenses", rewards
        };
    }

    /** Récompenses de la quête : les siennes, sinon celles de sa difficulté. */
    public List<String> rewardActions(QuestDefinition def) {
        if (!def.rewards.isEmpty()) {
            return def.rewards;
        }
        RewardSettings r = plugin.rewards();
        return def.scope == QuestScope.FACTION ? r.factionPerQuest(def.difficulty).rewards : r.playerPerQuest(def.difficulty).rewards;
    }

    public List<String> rewards(QuestDefinition def) {
        if (!def.rewards.isEmpty()) {
            return plugin.rewardService().describe(def.rewards);
        }
        RewardSettings r = plugin.rewards();
        return plugin.rewardService().describe(def.scope == QuestScope.FACTION ? r.factionPerQuest(def.difficulty)
                : r.playerPerQuest(def.difficulty));
    }

    public long points(QuestDefinition def) {
        return plugin.rewardService().points(rewardActions(def));
    }

    public String percent(QuestProgress q) {
        return Text.number(Math.floor(q.getPercent() * Math.pow(10, plugin.settings().percentDecimals))
                / Math.pow(10, plugin.settings().percentDecimals), plugin.settings().percentDecimals);
    }

    /** Barre de progression colorée. */
    public String bar(QuestProgress q) {
        Settings s = plugin.settings();
        int full = (int) Math.floor(q.getPercent() / 100.0 * s.barLength);
        StringBuilder sb = new StringBuilder(s.barFull);
        for (int i = 0; i < full; i++) {
            sb.append(s.barSymbol);
        }
        sb.append(s.barEmpty);
        for (int i = full; i < s.barLength; i++) {
            sb.append(s.barSymbol);
        }
        return Text.color(sb.toString());
    }
}
