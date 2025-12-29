package com.hnp_arda.castlerush.managers;

import com.hnp_arda.castlerush.core.Marker;
import com.hnp_arda.castlerush.core.PlayerCastle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static com.hnp_arda.castlerush.managers.GameManager.languageManager;

public class RaceManager {

    private final GameManager gameManager;
    private final Map<UUID, RaceProgress> playerProgress;
    private final Map<UUID, ArmorStand> spectatorHeads;
    private final List<PlayerCastle> castles;
    public List<UUID> teleporting;
    private BukkitRunnable spectatorHeadTask;
    private long raceStartTime;

    public RaceManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.playerProgress = new HashMap<>();
        this.spectatorHeads = new HashMap<>();
        this.teleporting = new ArrayList<>();
        castles = new ArrayList<>();
    }

    public void startRace(List<UUID> players) {
        playerProgress.clear();
        castles.clear();
        raceStartTime = System.currentTimeMillis();
        cleanupSpectatorHeads();

        castles.addAll(gameManager.playerCastles.values());

        Collections.shuffle(castles);

        players.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !castles.isEmpty()) {
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();

                RaceProgress progress = new RaceProgress();
                playerProgress.put(player.getUniqueId(), progress);

                teleportToStart(player, 0);
            }
        });
    }

    public void teleportToStart(Player player, int castleIndex) {

        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

        if (castleIndex >= castles.size()) {
            finishRace(player);
            return;
        }

        PlayerCastle playerCastle = castles.get(castleIndex);

        if (playerCastle == null) {
            player.sendMessage(Component.text(languageManager.get("race.castle_missing"), NamedTextColor.RED));
            return;
        }

        Location startLoc = playerCastle.getStart();

        teleporting.add(player.getUniqueId());
        player.teleport(startLoc);
        Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> teleporting.remove(player.getUniqueId()), 100L);

        RaceProgress progress = playerProgress.get(player.getUniqueId());
        progress.startCastle(castleIndex);
        progress.setCheckpoint(playerCastle.getStart());

        String ownerName = getCastleOwnerName(playerCastle);
        String separator = "==============================";
        player.sendMessage(Component.text(separator, NamedTextColor.GOLD));
        player.sendMessage(Component.text(languageManager.get("race.castle_header", (castleIndex + 1), castles.size(), ownerName), NamedTextColor.GREEN));
        player.sendMessage(Component.text(separator, NamedTextColor.GOLD));
    }

    public void checkTrigger(Player player, Location oldLocation, Location newLocation) {

        RaceProgress progress = playerProgress.get(player.getUniqueId());
        if (progress == null) return;

        int currentCastle = progress.getCurrentCastle();
        if (currentCastle >= castles.size()) return;

        PlayerCastle playerCastle = castles.get(currentCastle);

        if (playerCastle == null) return;

        Marker oldMarker = playerCastle.getMarkers().stream().filter(marker -> {
            Location loc = marker.getLocation();
            boolean y = oldLocation.getBlockY() == loc.getBlockY();
            y = marker.isAir() ? y : y || oldLocation.getBlockY() == loc.getBlockY() + 1;
            return oldLocation.getBlockX() == loc.getBlockX() && oldLocation.getBlockZ() == loc.getBlockZ() && y;
        }).findFirst().orElse(null);

        Marker newMarker = playerCastle.getMarkers().stream().filter(marker -> {
            Location loc = marker.getLocation();
            boolean y = newLocation.getBlockY() == loc.getBlockY();
            y = marker.isAir() ? y : y || newLocation.getBlockY() == loc.getBlockY() + 1;
            return newLocation.getBlockX() == loc.getBlockX() && newLocation.getBlockZ() == loc.getBlockZ() && y;
        }).findFirst().orElse(null);


        if (oldMarker == null && newMarker == null) return;

        if (oldMarker != null && newMarker != null) {
            if (oldMarker.isAdvancedToolMarker() && oldMarker.getAdvancedToolData().equals(newMarker.getAdvancedToolData()))
                return;
            oldMarker.triggerMarkerExit(player);
            newMarker.triggerMarkerEnter(player);
        } else if (oldMarker != null) oldMarker.triggerMarkerExit(player);
        else newMarker.triggerMarkerEnter(player);

    }

    public void setCheckpoint(Player player, Location location) {

        RaceProgress progress = playerProgress.get(player.getUniqueId());

        Location lastCheckpoint = progress.getLastCheckpoint();
        if (lastCheckpoint == null || !lastCheckpoint.equals(location)) {
            progress.setCheckpoint(location);
            player.sendMessage(Component.text(languageManager.get("race.checkpoint"), NamedTextColor.GOLD));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

    }


    public void handlePlayerDeath(Player player) {

        RaceProgress progress = playerProgress.get(player.getUniqueId());
        PlayerCastle playerCastle = castles.get(progress.getCurrentCastle());

        if (playerCastle == null) return;

        Location respawn = progress.getLastCheckpoint();

        Bukkit.getScheduler().runTask(gameManager.getPlugin(), () -> {
            player.teleport(respawn);
            player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.setFallDistance(0);
            player.setGameMode(GameMode.SURVIVAL);
            gameManager.getPlugin().getLogger().info("death aa");
            player.sendMessage(Component.text(languageManager.get("race.dead"), NamedTextColor.RED));
        });
    }

    private void finishRace(Player player) {
        RaceProgress progress = playerProgress.get(player.getUniqueId());
        if (progress == null) return;

        long totalTime = progress.getTotalTime();
        int minutes = (int) (totalTime / 60);
        int seconds = (int) (totalTime % 60);

        String separator = "==============================";
        Bukkit.broadcast(Component.text(separator, NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text(languageManager.get("race.all_finished", player.getName()), NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text(languageManager.get("race.total_time", minutes, seconds), NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text(separator, NamedTextColor.GOLD));

        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        progress.setFinished(true);
        createSpectatorHead(player);

        if (gameManager.getScoreboardManager() != null) {
            gameManager.getScoreboardManager().updateAllScoreboards();
        }
    }


    public void createSpectatorHead(Player spectator) {
        removeSpectatorHead(spectator.getUniqueId());

        ArmorStand stand = spectator.getWorld().spawn(spectator.getLocation().add(0, 0, 0), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setSmall(true);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setSilent(true);
            as.setCollidable(false);

            ItemStack skull = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(spectator);
            skull.setItemMeta(meta);
            as.getEquipment().setHelmet(skull);
        });

        spectatorHeads.put(spectator.getUniqueId(), stand);
        startSpectatorHeadTask();
    }

    public boolean hasSpectatorHead(UUID uuid) {
        return spectatorHeads.containsKey(uuid);
    }

    public void removeSpectatorHead(UUID uuid) {
        ArmorStand stand = spectatorHeads.remove(uuid);
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    private void cleanupSpectatorHeads() {
        for (ArmorStand stand : spectatorHeads.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        spectatorHeads.clear();
    }

    private void startSpectatorHeadTask() {
        if (spectatorHeadTask != null) return;

        spectatorHeadTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, ArmorStand>> it = spectatorHeads.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, ArmorStand> entry = it.next();
                    Player p = Bukkit.getPlayer(entry.getKey());
                    ArmorStand stand = entry.getValue();
                    if (p == null || !p.isOnline() || stand == null || stand.isDead()) {
                        if (stand != null && !stand.isDead()) stand.remove();
                        it.remove();
                        continue;
                    }
                    stand.teleport(p.getLocation().add(0, 0, 0));
                }

                if (spectatorHeads.isEmpty()) {
                    cancel();
                    spectatorHeadTask = null;
                }
            }
        };

        spectatorHeadTask.runTaskTimer(gameManager.getPlugin(), 0L, 1L);
    }


    public String getCastleOwnerName(PlayerCastle castle) {
        return castle.getCasleWorld().getName().replace("castle_rush_", "");
    }

    public void cleanup() {
        playerProgress.clear();
        castles.clear();

        cleanupSpectatorHeads();
        if (spectatorHeadTask != null) {
            spectatorHeadTask.cancel();
            spectatorHeadTask = null;
        }
    }

    public long getRaceStartTime() {
        return raceStartTime;
    }

    public Map<UUID, RaceProgress> getPlayerProgress() {
        return playerProgress;
    }

    public int getTotalCastles() {
        return castles.size();
    }

    public PlayerCastle getCastle(int i) {
        return castles.get(i);
    }

    public static class RaceProgress {
        private int currentCastle;
        private long castleStartTime;
        private long totalTime;
        private Location lastCheckpoint;
        private boolean finished;

        public RaceProgress() {
            this.currentCastle = 0;
            this.totalTime = 0;
            this.lastCheckpoint = null;
            this.finished = false;
        }

        public void startCastle(int castleIndex) {
            this.currentCastle = castleIndex;
            this.castleStartTime = System.currentTimeMillis();
            this.lastCheckpoint = null;
        }

        public long finishCastle() {
            long duration = (System.currentTimeMillis() - castleStartTime) / 1000;
            totalTime += duration;
            return duration;
        }

        public int getCurrentCastle() {
            return currentCastle;
        }

        public long getTotalTime() {
            return totalTime;
        }

        public void setCheckpoint(Location checkpoint) {
            this.lastCheckpoint = checkpoint.clone();
        }

        public Location getLastCheckpoint() {
            return lastCheckpoint;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

    }
}
