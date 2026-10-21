package fr.koriamc.quest.gui;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.config.RewardSettings;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.progress.ProgressService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Quêtes de la semaine du joueur (menus.json : joueur).
 */
final class PlayerQuestsMenu extends QuestListMenu {

    PlayerQuestsMenu(KoriaQuest plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("joueur");
    }

    @Override
    protected Object[] vars() {
        PlayerData pd = plugin.quests().player(player);
        int total = pd.hasChosen() ? pd.getQuests().size() : plugin.settings().playerCount;
        int done = pd.countDone();
        RewardSettings.Block bonus = RewardSettings.mainTier(plugin.rewards().playerTiers(pd.getDifficulty()), total);
        return ProgressService.with(super.vars(),
                "temps_restant", plugin.msg().duration(plugin.cycles().playerTimeLeft()),
                "difficulte", pd.hasChosen() ? plugin.settings().difficultyName(pd.getDifficulty())
                        : section().getString("textes.sans-difficulte", ""),
                "terminees", done,
                "total", total,
                "restantes", Math.max(0, total - done),
                "bonus", plugin.rewardService().describe(bonus),
                "bonus_etat", bonusState(done, total));
    }

    @Override
    protected void render() {
        Object[] kv = vars();
        renderQuests(QuestScope.JOUEUR, plugin.quests().player(player).getQuests(), kv);
        renderItems(kv);
    }
}
