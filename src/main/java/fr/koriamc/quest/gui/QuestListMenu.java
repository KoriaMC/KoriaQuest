package fr.koriamc.quest.gui;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestProgress;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.progress.ProgressService;
import fr.koriamc.quest.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Menu qui affiche des quêtes aux cases "quetes.cases" avec les modèles
 * "quetes.en-cours" et "quetes.terminee", puis les objets libres de "objets".
 * Action propre aux quêtes : LIVRER.
 */
abstract class QuestListMenu extends Menu {

    QuestListMenu(KoriaQuest plugin, Player player) {
        super(plugin, player);
    }

    /** Variables supplémentaires propres à une quête (contribution...). */
    protected Object[] questVars(QuestDefinition def, QuestProgress q) {
        return new Object[0];
    }

    protected void renderQuests(final QuestScope scope, List<QuestProgress> quests, Object[] menuVars) {
        ConfigurationSection s = section();
        List<Integer> slots = s.getIntegerList("quetes.cases");
        int shown = 0;
        for (final QuestProgress q : quests) {
            if (shown >= slots.size()) {
                break;
            }
            QuestDefinition def = plugin.catalog().get(scope, q.getQuest());
            if (def == null) {
                continue;
            }
            ConfigurationSection template = s.getConfigurationSection(q.isDone() ? "quetes.terminee" : "quetes.en-cours");
            String deliver = !q.isDone() && plugin.progress().isDelivery(def) ? s.getString("textes.livrer", "") : "";
            Object[] kv = ProgressService.with(ProgressService.with(plugin.view().vars(def, q), questVars(def, q)),
                    "livrer", deliver);
            kv = ProgressService.with(menuVars, kv);
            set(slots.get(shown), Items.fromSection(template, kv), clickHandler(template, new CustomAction() {
                @Override
                public boolean run(String type, String value) {
                    if (!type.equals("LIVRER")) {
                        return false;
                    }
                    if (plugin.progress().deliver(player, scope, q) > 0) {
                        refreshLater();
                    }
                    return true;
                }
            }, kv));
            shown++;
        }
    }

    protected void renderItems(Object[] kv) {
        ConfigurationSection items = section().getConfigurationSection("objets");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            setConfigured(items.getConfigurationSection(key), kv);
        }
    }

    /** Texte "bonus obtenu" ou "encore n quête(s)". */
    protected String bonusState(int done, int total) {
        return done >= total ? section().getString("textes.bonus-obtenu", "")
                : section().getString("textes.bonus-a-obtenir", "").replace("{restantes}", String.valueOf(total - done));
    }
}
