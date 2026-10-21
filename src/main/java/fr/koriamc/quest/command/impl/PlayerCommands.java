package fr.koriamc.quest.command.impl;

import fr.koriamc.quest.KoriaQuest;
import fr.koriamc.quest.command.CommandContext;
import fr.koriamc.quest.command.SubCommand;
import fr.koriamc.quest.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Commandes des joueurs : aide, menus, difficulté, temps restant.
 */
public final class PlayerCommands {

    private PlayerCommands() {
    }

    /** /quests help */
    public static final class Help extends SubCommand {
        public Help(KoriaQuest plugin) {
            super(plugin, "help", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            ctx.msg("help.header");
            for (SubCommand sub : plugin.commands().visible(ctx.sender)) {
                ctx.msg("help.line", "command", sub.getId(), "usage", sub.getUsage(), "description", sub.getDescription());
            }
            ctx.msg("help.footer");
        }
    }

    /** /quests menu */
    public static final class OpenMenu extends SubCommand {
        public OpenMenu(KoriaQuest plugin) {
            super(plugin, "menu", true, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.menus().openPlayer(ctx.player);
        }
    }

    /** /quests faction */
    public static final class FactionMenu extends SubCommand {
        public FactionMenu(KoriaQuest plugin) {
            super(plugin, "faction", true, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.menus().openFaction(ctx.player);
        }
    }

    /** /quests difficulty [easy|medium|hard] */
    public static final class Difficulty extends SubCommand {
        public Difficulty(KoriaQuest plugin) {
            super(plugin, "difficulty", true, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (ctx.size() == 0) {
                plugin.menus().open(ctx.player, "difficulte");
            } else {
                plugin.menus().choose(ctx.player, plugin.settings().difficultyFromArgument(ctx.arg(0)));
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), new ArrayList<String>(plugin.settings().difficultyArguments.keySet()))
                    : Collections.<String>emptyList();
        }
    }

    /** /quests time : avant les nouvelles quêtes joueur et de faction */
    public static final class TimeLeft extends SubCommand {
        public TimeLeft(KoriaQuest plugin) {
            super(plugin, "time", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            ctx.msg("cycle.time-left", "temps_joueur", plugin.msg().duration(plugin.cycles().playerTimeLeft()),
                    "temps_faction", plugin.msg().duration(plugin.cycles().factionTimeLeft()));
        }
    }
}
