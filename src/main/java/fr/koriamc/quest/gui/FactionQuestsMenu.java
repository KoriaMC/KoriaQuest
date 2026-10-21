package fr.koriamc.quest.gui;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.config.RewardSettings;
import fr.koriamc.quest.model.FactionData;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestProgress;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.progress.ProgressService;
import fr.koriamc.quest.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quêtes communes de la faction du joueur (menus.json : faction).
 */
final class FactionQuestsMenu extends QuestListMenu {

    private final String factionId;

    FactionQuestsMenu(KoriaQuest plugin, Player player, String factionId) {
        super(plugin, player);
        this.factionId = factionId;
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("faction");
    }

    @Override
    protected Object[] vars() {
        FactionData fd = plugin.quests().faction(factionId);
        int total = fd.getQuests().size();
        int done = fd.countDone();
        RewardSettings.Block bonus = RewardSettings.mainTier(plugin.rewards().factionTiers(), total);
        return ProgressService.with(super.vars(),
                "faction", plugin.factionHook().getTag(factionId),
                "points", Text.number(plugin.factionHook().getPoints(factionId), 0),
                "terminees", done,
                "total", total,
                "restantes", Math.max(0, total - done),
                "bonus", plugin.rewardService().describe(bonus),
                "bonus_etat", bonusState(done, total));
    }

    @Override
    protected Object[] questVars(QuestDefinition def, QuestProgress q) {
        ConfigurationSection s = section();
        List<String> lines = new ArrayList<String>();
        int rank = 0;
        for (Map.Entry<String, Long> e : q.getRanking()) {
            if (rank >= plugin.settings().contributorsShown) {
                break;
            }
            rank++;
            lines.add(Text.replace(s.getString("textes.contributeur", "{joueur}"), "rang", rank, "joueur", name(e.getKey()),
                    "quantite", Text.number(e.getValue(), 0)));
        }
        if (lines.isEmpty()) {
            lines.add(s.getString("textes.aucun-contributeur", ""));
        }
        return new Object[]{"contribution", Text.number(q.getContribution(player.getUniqueId().toString()), 0),
                "contributeurs", lines};
    }

    private String name(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            PlayerData pd = plugin.data().findPlayer(id);
            if (pd != null) {
                return pd.getName();
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            return op.getName() == null ? "?" : op.getName();
        } catch (IllegalArgumentException e) {
            return "?";
        }
    }

    @Override
    protected void render() {
        Object[] kv = vars();
        renderQuests(QuestScope.FACTION, plugin.quests().faction(factionId).getQuests(), kv);
        renderItems(kv);
    }
}
