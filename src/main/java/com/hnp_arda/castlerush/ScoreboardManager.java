package com.hnp_arda.castlerush;

import com.hnp_arda.castlerush.GameManager.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class ScoreboardManager {

    private final Main plugin;
    private final GameManager gameManager;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private BukkitRunnable updateTask;

    public ScoreboardManager(Main plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.playerScoreboards = new HashMap<>();
    }

    public void createScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
                "castlerush",
                Criteria.DUMMY,
                Component.text(GameManager.getLanguageManager().get("scoreboard.title"), NamedTextColor.GOLD)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), scoreboard);

        updateScoreboard(player);
    }

    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("castlerush");
        if (objective == null) return;

        for (String entry : new ArrayList<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        int line = 15;
        objective.getScore(" ").setScore(line--);

        GameState state = gameManager.getGameState();

        if (state == GameState.BUILDING) {
            int timeLeft = gameManager.getCurrentBuildTime();
            int minutes = timeLeft / 60;
            int seconds = timeLeft % 60;

            objective.getScore(GameManager.getLanguageManager().get("scoreboard.time_label")).setScore(line--);
            objective.getScore("  " + String.format("%02d:%02d", minutes, seconds)).setScore(line--);
            objective.getScore("  ").setScore(line--);

            objective.getScore(GameManager.getLanguageManager().get("scoreboard.build_task")).setScore(line--);

        } else if (state == GameState.RACING) {
            RaceManager raceManager = gameManager.getRaceManager();
            long raceTime = (System.currentTimeMillis() - raceManager.getRaceStartTime()) / 1000;
            int minutes = (int) (raceTime / 60);
            int seconds = (int) (raceTime % 60);

            objective.getScore(GameManager.getLanguageManager().get("scoreboard.time_label")).setScore(line--);
            objective.getScore("  " + String.format("%02d:%02d", minutes, seconds)).setScore(line--);
            objective.getScore("   ").setScore(line--);

            objective.getScore(GameManager.getLanguageManager().get("scoreboard.players")).setScore(line--);

            List<Map.Entry<UUID, RaceManager.RaceProgress>> sortedPlayers = new ArrayList<>(raceManager.getPlayerProgress().entrySet());
            sortedPlayers.sort((a, b) -> {
                RaceManager.RaceProgress progressA = a.getValue();
                RaceManager.RaceProgress progressB = b.getValue();

                if (progressA.isFinished() && !progressB.isFinished()) return -1;
                if (!progressA.isFinished() && progressB.isFinished()) return 1;

                if (progressA.isFinished() && progressB.isFinished()) {
                    return Long.compare(progressA.getTotalTime(), progressB.getTotalTime());
                }

                return Integer.compare(progressB.getCurrentCastle(), progressA.getCurrentCastle());
            });

            int totalCastles = raceManager.getTotalCastles();

            for (Map.Entry<UUID, RaceManager.RaceProgress> entry : sortedPlayers) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p == null) continue;

                RaceManager.RaceProgress progress = entry.getValue();
                String playerName = p.getName();

                if (progress.isFinished()) {
                    long totalTime = progress.getTotalTime();
                    int mins = (int) (totalTime / 60);
                    int secs = (int) (totalTime % 60);
                    String entryLine = "✔ " + playerName + " " + String.format("%02d:%02d", mins, secs);
                    objective.getScore(entryLine).setScore(line--);
                } else {
                    int currentCastle = progress.getCurrentCastle() + 1;
                    boolean isSelf = p.getUniqueId().equals(player.getUniqueId());
                    String prefix = isSelf ? "> " : "  ";
                    String entryLine = prefix + playerName + " [" + currentCastle + "/" + totalCastles + "]";
                    objective.getScore(entryLine).setScore(line--);
                }
            }

        } else if (state == GameState.WAITING) {
            objective.getScore(GameManager.getLanguageManager().get("scoreboard.waiting")).setScore(line--);
        }

        objective.getScore("    ").setScore(line - 1);
    }

    public void updateAllScoreboards() {
        for (UUID uuid : new ArrayList<>(playerScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updateScoreboard(player);
            }
        }
    }

    public void startAutoUpdate() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                GameState state = gameManager.getGameState();
                if (state == GameState.BUILDING || state == GameState.RACING) {
                    updateAllScoreboards();
                }
            }
        };

        updateTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopAutoUpdate() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void removeAllScoreboards() {
        for (UUID uuid : new ArrayList<>(playerScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeScoreboard(player);
            }
        }
        playerScoreboards.clear();
    }
}
