package fr.koriamc.quest.gui;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.config.RewardSettings;
import fr.koriamc.quest.progress.ProgressService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Choix de la difficulté des quêtes joueur (menus.json : difficulte). Les objets
 * portent l'action CHOISIR:&lt;difficulté&gt; ; pour chaque difficulté, les
 * variables {recompenses_&lt;id&gt;} et {bonus_&lt;id&gt;} décrivent ses récompenses.
 */
final class DifficultyMenu extends Menu {

    DifficultyMenu(KoriaQuest plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("difficulte");
    }

    @Override
    protected Object[] vars() {
        List<Object> kv = new ArrayList<Object>();
        int count = plugin.settings().playerCount;
        kv.add("temps_restant");
        kv.add(plugin.msg().duration(plugin.cycles().playerTimeLeft()));
        kv.add("nombre");
        kv.add(count);
        for (String id : plugin.settings().difficulties.keySet()) {
            kv.add("recompenses_" + id);
            kv.add(plugin.rewardService().describe(plugin.rewards().playerPerQuest(id)));
            kv.add("bonus_" + id);
            kv.add(plugin.rewardService().describe(RewardSettings.mainTier(plugin.rewards().playerTiers(id), count)));
            kv.add("nom_" + id);
            kv.add(plugin.settings().difficultyName(id));
        }
        return ProgressService.with(super.vars(), kv.toArray());
    }

    @Override
    protected void render() {
        ConfigurationSection items = section().getConfigurationSection("objets");
        if (items == null) {
            return;
        }
        Object[] kv = vars();
        for (String key : items.getKeys(false)) {
            setConfigured(items.getConfigurationSection(key), kv);
        }
    }
}
