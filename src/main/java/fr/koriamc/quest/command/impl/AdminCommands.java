package fr.koriamc.quest.command.impl;

import fr.koriamc.factions.model.Faction;
import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.command.CommandContext;
import fr.koriamc.quest.command.SubCommand;
import fr.koriamc.quest.model.FactionData;
import fr.koriamc.quest.model.PlayerData;
import fr.koriamc.quest.model.QuestDefinition;
import fr.koriamc.quest.model.QuestProgress;
import fr.koriamc.quest.model.QuestScope;
import fr.koriamc.quest.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Commandes d'administration : consultation, progression, réinitialisation,
 * renouvellement forcé, éditeur, sauvegarde et rechargement.
 */
public final class AdminCommands {

    private static final List<String> TARGETS = Arrays.asList("player", "faction");

    private AdminCommands() {
    }

    /** Joueur connu (connecté ou enregistré) d'après son pseudo, ou null. */
    private static PlayerData findPlayer(KoriaQuest plugin, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return plugin.quests().player(online);
        }
        for (PlayerData pd : plugin.data().players()) {
            if (pd.getName().equalsIgnoreCase(name)) {
                return plugin.quests().player(pd.getUniqueId(), pd.getName());
            }
        }
        return null;
    }

    private static List<String> onlineNames(String input) {
        List<String> names = new ArrayList<String>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return Text.complete(input, names);
    }

    /** /quests view &lt;joueur&gt; */
    public static final class View extends SubCommand {
        public View(KoriaQuest plugin) {
            super(plugin, "view", false, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            PlayerData pd = findPlayer(plugin, ctx.arg(0));
            if (pd == null) {
                ctx.msg("general.unknown-player", "player", ctx.arg(0));
                return;
            }
            ctx.msg("admin.view-header", "player", pd.getName());
            ctx.msg("admin.view-difficulty", "difficulte", pd.hasChosen() ? plugin.settings().difficultyName(pd.getDifficulty())
                    : plugin.msg().text("values.none"));
            list(ctx, QuestScope.JOUEUR, pd.getQuests());
            String factionId = plugin.factionHook().getFactionId(pd.getUniqueId());
            if (factionId != null && plugin.quests().isEligible(factionId)) {
                FactionData fd = plugin.quests().faction(factionId);
                ctx.msg("admin.view-faction", "faction", plugin.factionHook().getTag(factionId), "terminees", fd.countDone(),
                        "total", fd.getQuests().size());
                list(ctx, QuestScope.FACTION, fd.getQuests());
            }
        }

        private void list(CommandContext ctx, QuestScope scope, List<QuestProgress> quests) {
            int n = 0;
            for (QuestProgress q : quests) {
                n++;
                QuestDefinition def = plugin.catalog().get(scope, q.getQuest());
                ctx.msg("admin.view-quest", "numero", n, "quete", def == null ? q.getQuest() : def.name,
                        "etat", plugin.msg().text(q.isDone() ? "admin.state-done" : "admin.state-running"),
                        "progression", Text.number(q.getProgress(), 0), "objectif", Text.number(q.getTarget(), 0));
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /quests progress &lt;joueur|faction&gt; &lt;nom&gt; &lt;n° quête&gt; &lt;valeur&gt; */
    public static final class SetProgress extends SubCommand {
        public SetProgress(KoriaQuest plugin) {
            super(plugin, "progress", false, 4, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            Integer index = Text.parseInt(ctx.arg(2));
            Double value = Text.parseDouble(ctx.arg(3));
            if (index == null || value == null || value < 0) {
                ctx.msg("general.invalid-number", "input", index == null ? ctx.arg(2) : ctx.arg(3));
                return;
            }
            if (ctx.arg(0).equalsIgnoreCase("faction")) {
                Faction f = plugin.factionHook().getByTag(ctx.arg(1));
                if (f == null) {
                    ctx.msg("general.unknown-faction", "faction", ctx.arg(1));
                    return;
                }
                FactionData fd = plugin.quests().faction(f.getId());
                QuestProgress q = index < 1 || index > fd.getQuests().size() ? null : fd.getQuests().get(index - 1);
                if (q == null) {
                    ctx.msg("admin.no-quest", "numero", index);
                    return;
                }
                plugin.progress().setFactionProgress(f.getId(), fd, q, value.longValue());
                done(ctx, QuestScope.FACTION, q);
                return;
            }
            PlayerData pd = findPlayer(plugin, ctx.arg(1));
            if (pd == null) {
                ctx.msg("general.unknown-player", "player", ctx.arg(1));
                return;
            }
            QuestProgress q = index < 1 || index > pd.getQuests().size() ? null : pd.getQuests().get(index - 1);
            if (q == null) {
                ctx.msg("admin.no-quest", "numero", index);
                return;
            }
            plugin.progress().setPlayerProgress(pd, q, value.longValue());
            done(ctx, QuestScope.JOUEUR, q);
        }

        private void done(CommandContext ctx, QuestScope scope, QuestProgress q) {
            QuestDefinition def = plugin.catalog().get(scope, q.getQuest());
            ctx.msg("admin.progress-set", "quete", def == null ? q.getQuest() : def.name,
                    "progression", Text.number(q.getProgress(), 0), "objectif", Text.number(q.getTarget(), 0));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return Text.complete(ctx.arg(0), TARGETS);
            }
            if (ctx.size() == 2) {
                return ctx.arg(0).equalsIgnoreCase("faction") ? Text.complete(ctx.arg(1), plugin.factionHook().getTags())
                        : onlineNames(ctx.arg(1));
            }
            return Collections.emptyList();
        }
    }

    /** /quests reset &lt;joueur|faction&gt; &lt;nom&gt; */
    public static final class Reset extends SubCommand {
        public Reset(KoriaQuest plugin) {
            super(plugin, "reset", false, 2, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (ctx.arg(0).toLowerCase(Locale.ROOT).equals("faction")) {
                Faction f = plugin.factionHook().getByTag(ctx.arg(1));
                if (f == null) {
                    ctx.msg("general.unknown-faction", "faction", ctx.arg(1));
                    return;
                }
                plugin.quests().resetFaction(f.getId());
                ctx.msg("admin.reset-faction", "faction", f.getTag());
                return;
            }
            PlayerData pd = findPlayer(plugin, ctx.arg(1));
            if (pd == null) {
                ctx.msg("general.unknown-player", "player", ctx.arg(1));
                return;
            }
            plugin.quests().resetPlayer(pd.getUniqueId(), pd.getName());
            ctx.msg("admin.reset-player", "player", pd.getName());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return Text.complete(ctx.arg(0), TARGETS);
            }
            if (ctx.size() == 2) {
                return ctx.arg(0).equalsIgnoreCase("faction") ? Text.complete(ctx.arg(1), plugin.factionHook().getTags())
                        : onlineNames(ctx.arg(1));
            }
            return Collections.emptyList();
        }
    }

    /** /quests renew [tout|faction|joueur] : nouvelles quêtes immédiatement */
    public static final class Renew extends SubCommand {
        private static final List<String> SCOPES = Arrays.asList("all", "faction", "player");

        public Renew(KoriaQuest plugin) {
            super(plugin, "renew", false, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            String scope = ctx.size() == 0 ? "all" : ctx.arg(0).toLowerCase(Locale.ROOT);
            if (!SCOPES.contains(scope)) {
                ctx.msg("general.usage", "command", ctx.subLabel, "usage", getUsage());
                return;
            }
            if (!scope.equals("player")) {
                plugin.cycles().newWeek(true);
                ctx.msg("admin.renewed-faction");
            }
            if (!scope.equals("faction")) {
                plugin.cycles().newPlayerPeriod(true);
                ctx.msg("admin.renewed-player");
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), SCOPES) : Collections.<String>emptyList();
        }
    }

    /** /quests config [fichier] */
    public static final class Config extends SubCommand {
        public Config(KoriaQuest plugin) {
            super(plugin, "config", true, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.menus().openConfig(ctx.player, ctx.arg(0))) {
                ctx.msg("config-editor.unknown-file", "file", ctx.arg(0));
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            List<String> names = new ArrayList<String>();
            for (fr.koriamc.quest.config.JsonFile file : plugin.configFiles()) {
                names.add(file.getName().replace(".json", ""));
            }
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), names) : Collections.<String>emptyList();
        }
    }

    /** /quests save */
    public static final class Save extends SubCommand {
        public Save(KoriaQuest plugin) {
            super(plugin, "save", false, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.data().saveAsync();
            ctx.msg("general.saved");
        }
    }

    /** /quests reload */
    public static final class Reload extends SubCommand {
        public Reload(KoriaQuest plugin) {
            super(plugin, "reload", false, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            List<String> problems = plugin.reload();
            ctx.msg("general.reload-done", "quests", plugin.catalog().count());
            if (!problems.isEmpty()) {
                ctx.msg("general.reload-problems", "count", problems.size());
                for (String problem : problems) {
                    ctx.msg("general.reload-problem", "problem", problem);
                }
            }
        }
    }
}
