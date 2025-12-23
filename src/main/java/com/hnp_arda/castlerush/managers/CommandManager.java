package com.hnp_arda.castlerush.managers;

import com.hnp_arda.castlerush.managers.GameManager.GameState;
import com.hnp_arda.castlerush.tools.BaseTool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.hnp_arda.castlerush.managers.GameManager.languageManager;

public record CommandManager(GameManager gameManager) implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
        } else switch (args[0].toLowerCase()) {
            case "settings" -> handleSettings(sender, args);
            case "time" -> handleTime(sender, args);
            case "tools" -> handleTools(sender, args);
            case "build" -> handleBuild(sender);
            case "start" -> handleRaceStart(sender);
            case "end" -> handleEnd(sender);
            case "reset" -> handleReset();
            case "info" -> handleInfo(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleSettings(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text(languageManager.get("command.settings.usage"), NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "language" -> handleLanguageSetting(sender, args);
            case "pvp" -> handlePvpSetting(sender, args);
            default ->
                    sender.sendMessage(Component.text(languageManager.get("command.settings.usage"), NamedTextColor.RED));
        }
    }

    private void handleLanguageSetting(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text(languageManager.get("command.settings.language_usage"), NamedTextColor.RED));
            return;
        }

        String requestedLanguage = args[2].toLowerCase(Locale.ROOT);
        if (languageManager.setLanguage(requestedLanguage)) {
            sender.sendMessage(Component.text(languageManager.get("command.settings.language_set", requestedLanguage), NamedTextColor.GREEN));
        } else {
            String available = String.join(", ", languageManager.getAvailableLanguages());
            sender.sendMessage(Component.text(languageManager.get("command.settings.invalid_language", available), NamedTextColor.RED));
        }
    }

    private void handlePvpSetting(CommandSender sender, String[] args) {
        if (gameManager.getGameState() == GameState.RACING) {
            sender.sendMessage(Component.text(languageManager.get("command.settings.pvp_only_before_race"), NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text(languageManager.get("command.settings.pvp_usage"), NamedTextColor.RED));
            return;
        }

        String value = args[2].toLowerCase(Locale.ROOT);
        Boolean enable = switch (value) {
            case "on", "true", "yes" -> true;
            case "off", "false", "no" -> false;
            default -> null;
        };

        if (enable == null) {
            sender.sendMessage(Component.text(languageManager.get("command.settings.pvp_usage"), NamedTextColor.RED));
            return;
        }

        gameManager.setPvpEnabled(enable);
        if (enable) {
            sender.sendMessage(Component.text(languageManager.get("command.settings.pvp_set_on"), NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(languageManager.get("command.settings.pvp_set_off"), NamedTextColor.GREEN));
        }
    }

    private void handleTools(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(languageManager.get("tools.only_players"), NamedTextColor.RED));
            return;
        }

        if (gameManager.getGameState() != GameState.BUILDING) {
            player.sendMessage(Component.text(languageManager.get("tools.no_game"), NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text(languageManager.get("tools.header"), NamedTextColor.GOLD));
            gameManager.getToolsManager().getTools().forEach(tool -> player.sendMessage(Component.text(tool.getCommand(), NamedTextColor.YELLOW)));
            return;
        }

        BaseTool tool = gameManager.getToolsManager().getTool(args[1]);
        if (tool == null) {
            player.sendMessage(Component.text(languageManager.get("tools.unknown"), NamedTextColor.RED));
            return;
        }
        tool.giveToPlayer(player);
        player.sendMessage(Component.text(tool.getReceived(), NamedTextColor.GREEN));
        player.sendMessage(Component.text(tool.getInfo(), NamedTextColor.GREEN));
        player.sendMessage(Component.text(tool.getDisplay(), NamedTextColor.GREEN));

    }

    private void handleTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text(languageManager.get("command.time.usage"), NamedTextColor.RED));
            return;
        }

        try {
            int seconds = Integer.parseInt(args[2]) * 60;
            if (args[1].equalsIgnoreCase("set")) {
                if (gameManager.getGameState() != GameState.WAITING) {
                    Bukkit.broadcast(Component.text(languageManager.get("command.time.already_running"), NamedTextColor.RED));
                    return;
                }
                gameManager.setBuildTime(seconds);
                gameManager.getPlugin().updateTimeSign(seconds / 60);
                sender.sendMessage(Component.text(languageManager.get("command.time.set", (seconds / 60)), NamedTextColor.GREEN));
            } else if (args[1].equalsIgnoreCase("add")) {
                gameManager.addBuildTime(seconds);
                Bukkit.broadcast(Component.text(languageManager.get("command.time.added", (seconds / 60)), NamedTextColor.GREEN));
                if (gameManager.getGameState() == GameState.WAITING)
                    gameManager.getPlugin().updateTimeSign(gameManager.getBuildTimeSeconds() / 60);
            } else {
                sender.sendMessage(Component.text(languageManager.get("command.time.usage"), NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text(languageManager.get("command.time.usage"), NamedTextColor.RED));
        }
    }

    private void handleBuild(CommandSender sender) {
        if (gameManager.getGameState() == GameState.BUILDING || gameManager.getGameState() == GameState.RACING) {
            sender.sendMessage(Component.text(languageManager.get("command.start.already_running"), NamedTextColor.RED));
            return;
        }

        gameManager.startBuild();
    }

    private void handleRaceStart(CommandSender sender) {
        if (gameManager.getGameState() != GameState.BUILDING) {
            sender.sendMessage(Component.text(languageManager.get("command.start.not_running"), NamedTextColor.RED));
            return;
        }

        gameManager.startRace();
    }

    private void handleEnd(CommandSender sender) {
        gameManager.end();
        sender.sendMessage(Component.text(languageManager.get("command.end.finished"), NamedTextColor.GREEN));
    }

    private void handleReset() {
        gameManager.resetGame();
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(Component.text(languageManager.get("command.info.header"), NamedTextColor.GOLD));
        sender.sendMessage(Component.text(languageManager.get("command.info.status", gameManager.getGameState()), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.info.time", (gameManager.getBuildTimeSeconds() / 60)), NamedTextColor.YELLOW));
        if (gameManager.getGameState() == GameState.BUILDING) {
            sender.sendMessage(Component.text(languageManager.get("command.info.remaining", (gameManager.getCurrentBuildTime() / 60), String.format("%02d", gameManager.getCurrentBuildTime() % 60)), NamedTextColor.YELLOW));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(languageManager.get("command.help.header"), NamedTextColor.GOLD));
        sender.sendMessage(Component.text(languageManager.get("command.help.settings"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.help.time_set"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.help.time_add"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.help.tools"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.help.start"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.help.end"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.help.reset"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(languageManager.get("command.help.info"), NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("build", "start", "end", "reset", "tools", "time", "settings", "info", "help"));
        } else if (args.length == 2) {
            completions = switch (args[0].toLowerCase()) {
                case "settings" -> Arrays.asList("language", "pvp");
                case "time" -> Arrays.asList("set", "add");
                case "tools" -> gameManager.getToolsManager().getToolsNames(true);
                default -> completions;
            };
        } else if (args.length == 3 && args[0].equalsIgnoreCase("settings")) {
            if (args[1].equalsIgnoreCase("language")) {
                completions.addAll(GameManager.getLanguageManager().getAvailableLanguages());
            } else if (args[1].equalsIgnoreCase("pvp")) {
                completions.addAll(Arrays.asList("on", "off"));
            }
        }

        return completions;
    }
}


