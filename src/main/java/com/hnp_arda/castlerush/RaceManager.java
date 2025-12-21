package com.hnp_arda.castlerush;

import com.hnp_arda.castlerush.tools.MarkerData;
import com.hnp_arda.castlerush.tools.effect.Effect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static com.hnp_arda.castlerush.GameManager.languageManager;

public class RaceManager {

    private final GameManager gameManager;
    private final Map<UUID, RaceProgress> playerProgress;
    private final Map<UUID, ArmorStand> spectatorHeads;
    private final Map<UUID, ActiveEffect> activeEffects;
    public List<UUID> teleporting;
    private BukkitRunnable spectatorHeadTask;
    private List<World> castles;
    private long raceStartTime;

    public RaceManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.playerProgress = new HashMap<>();
        this.spectatorHeads = new HashMap<>();
        this.activeEffects = new HashMap<>();
        this.teleporting = new ArrayList<>();

        gameManager.getPlugin().getServer().getPluginManager().registerEvents(new RaceListener(gameManager), gameManager.getPlugin());
    }

    public void startRace(Set<UUID> participants) {
        playerProgress.clear();
        raceStartTime = System.currentTimeMillis();
        activeEffects.clear();
        cleanupSpectatorHeads();

        List<World> worlds = new ArrayList<>();
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                PlayerCastle playerCastle = gameManager.getPlayerCastle(p);
                if (playerCastle != null) {
                    worlds.add(playerCastle.getCasleWorld());
                }
            }
        }

        Collections.shuffle(worlds);
        this.castles = worlds;

        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !castles.isEmpty()) {
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();

                RaceProgress progress = new RaceProgress();
                playerProgress.put(uuid, progress);

                teleportToStart(player, 0);
            }
        }
    }

    private void teleportToStart(Player player, int castleIndex) {
        if (castleIndex >= castles.size()) {
            finishRace(player);
            return;
        }

        World castleWorld = castles.get(castleIndex);
        PlayerCastle playerCastle = findCastleByWorld(castleWorld);

        if (playerCastle == null) {
            player.sendMessage(Component.text(languageManager.get("race.castle_missing"), NamedTextColor.RED));
            return;
        }

        Location startLoc = getMarkerBase(castleIndex, "start");
        if (startLoc != null) {
            startLoc = startLoc.clone().add(0.5, 1, 0.5);
        } else {
            startLoc = castleWorld.getSpawnLocation();
        }
        teleporting.add(player.getUniqueId());
        player.teleport(startLoc);
        Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> teleporting.remove(player.getUniqueId()), 100L);

        RaceProgress progress = playerProgress.get(player.getUniqueId());
        progress.startCastle(castleIndex);

        String ownerName = getWorldOwnerName(castleWorld);
        String separator = "==============================";
        player.sendMessage(Component.text(separator, NamedTextColor.GOLD));
        player.sendMessage(Component.text(languageManager.get("race.castle_header", (castleIndex + 1), castles.size(), ownerName), NamedTextColor.GREEN));
        player.sendMessage(Component.text(separator, NamedTextColor.GOLD));
    }

    public void checkEndReached(Player player, Location location) {
        RaceProgress progress = playerProgress.get(player.getUniqueId());
        if (progress == null) return;

        int castleIndex = getCastleIndexByWorld(location.getWorld());
        if (castleIndex < 0) return;

        boolean finished = progress.isFinished();
        int currentCastle = progress.getCurrentCastle();
        if (!finished && currentCastle != castleIndex) {
            return;
        }

        Location startLoc = getMarkerBase(castleIndex, "start");
        Location endLoc = getMarkerBase(castleIndex, "end");
        if (finished) {
            if (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() == null) {
                if (endLoc != null && isOnTopOfMarker(location, endLoc) && castleIndex < castles.size() - 1)
                    teleportSpectatorToStart(player, castleIndex + 1);
                else if (startLoc != null && isOnTopOfMarker(location, startLoc) && castleIndex > 0)
                    teleportSpectatorToEnd(player, castleIndex - 1);
            }
            return;
        }

        if (endLoc != null && isOnTopOfMarker(location, endLoc)) {
            long duration = progress.finishCastle();
            String ownerName = getWorldOwnerName(castles.get(castleIndex));

            int minutes = (int) (duration / 60);
            int seconds = (int) (duration % 60);

            Bukkit.broadcast(Component.text(languageManager.get("race.castle_complete", player.getName(), ownerName, minutes, seconds), NamedTextColor.GREEN));

            teleportToStart(player, castleIndex + 1);

            if (gameManager.getScoreboardManager() != null) {
                gameManager.getScoreboardManager().updateAllScoreboards();
            }
        }
    }

    public void handleCheckpoint(Player player, Location location) {
        RaceProgress progress = playerProgress.get(player.getUniqueId());
        if (progress == null || progress.isFinished()) return;

        int currentCastle = progress.getCurrentCastle();
        if (currentCastle >= castles.size()) return;

        World castleWorld = castles.get(currentCastle);
        PlayerCastle playerCastle = findCastleByWorld(castleWorld);

        if (playerCastle == null) return;

        for (MarkerData checkpoint : playerCastle.getMarker("checkpoint")) {
            Location checkLoc = checkpoint.getLocation();

            if (location.getBlockX() == checkLoc.getBlockX() && location.getBlockZ() == checkLoc.getBlockZ() && location.getBlockY() == checkLoc.getBlockY() + 1) {

                Location lastCheckpoint = progress.getLastCheckpoint();
                if (lastCheckpoint == null || !lastCheckpoint.equals(checkLoc)) {
                    progress.setCheckpoint(checkLoc);
                    player.sendMessage(Component.text(languageManager.get("race.checkpoint"), NamedTextColor.GOLD));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                return;
            }
        }
    }

    public void handleDeathzone(Player player, Location location) {
        RaceProgress progress = playerProgress.get(player.getUniqueId());
        if (progress == null || progress.isFinished()) return;

        int currentCastle = progress.getCurrentCastle();
        if (currentCastle >= castles.size()) return;

        World castleWorld = castles.get(currentCastle);
        PlayerCastle playerCastle = findCastleByWorld(castleWorld);

        if (playerCastle == null) return;

        if (player.isDead()) {
            return;
        }

        for (MarkerData deathzone : playerCastle.getMarker("deathzone")) {
            Location dzLoc = deathzone.getLocation();

            boolean inDeathzone = false;

            if (location.getBlockX() == dzLoc.getBlockX() && location.getBlockY() == dzLoc.getBlockY() && location.getBlockZ() == dzLoc.getBlockZ()) {
                inDeathzone = true;
            } else if (location.getBlockX() == dzLoc.getBlockX() && location.getBlockZ() == dzLoc.getBlockZ() && location.getBlockY() == dzLoc.getBlockY() + 1) {
                inDeathzone = true;
            }

            if (inDeathzone) {
                handlePlayerDeath(player, progress, playerCastle);
                return;
            }
        }
    }

    public void handleEffectZone(Player player, Location location) {
        RaceProgress progress = playerProgress.get(player.getUniqueId());
        if (progress == null || progress.isFinished()) return;

        int currentCastle = progress.getCurrentCastle();
        if (castles == null || currentCastle >= castles.size()) return;

        World castleWorld = castles.get(currentCastle);
        PlayerCastle playerCastle = findCastleByWorld(castleWorld);

        if (playerCastle == null) return;

        ActiveEffect effect = findEffectAtLocation(playerCastle, location);
        ActiveEffect current = activeEffects.get(player.getUniqueId());

        if (effect == null) {
            if (current != null) {
                clearEffect(player, current);
                activeEffects.remove(player.getUniqueId());
            }
            return;
        }

        if (!isSameEffect(effect, current)) {
            if (current != null) {
                clearEffect(player, current);
            }
            applyEffect(player, effect);
            activeEffects.put(player.getUniqueId(), effect);
        }
    }

    public void handlePlayerDeath(Player player, RaceProgress progress, PlayerCastle playerCastle) {
        ActiveEffect currentEffect = activeEffects.remove(player.getUniqueId());
        if (currentEffect != null) {
            clearEffect(player, currentEffect);
        }

        Location respawn = progress.getLastCheckpoint();
        if (respawn == null) {
            respawn = playerCastle.getMarker("start").stream().findFirst().map(marker -> marker.getLocation().clone().add(0.5, 1, 0.5)).orElse(playerCastle.getCasleWorld().getSpawnLocation());
        } else {
            respawn = respawn.clone().add(0.5, 1, 0.5);
        }

        Location finalRespawn = respawn;
        Bukkit.getScheduler().runTask(gameManager.getPlugin(), () -> {
            player.teleport(finalRespawn);
            player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.setFallDistance(0);
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(Component.text(languageManager.get("race.deathzone"), NamedTextColor.RED));
        });
    }

    private ActiveEffect findEffectAtLocation(PlayerCastle playerCastle, Location playerLoc) {
        for (MarkerData effectMarker : playerCastle.getMarker("effect")) {
            Location effLoc = effectMarker.getLocation();
            boolean airMarker = effectMarker.getOriginalMaterial() == Material.AIR || effectMarker.getOriginalMaterial().isAir();
            boolean inZone = false;

            if (playerLoc.getBlockX() == effLoc.getBlockX() && playerLoc.getBlockZ() == effLoc.getBlockZ()) {
                if (airMarker) {
                    inZone = playerLoc.getBlockY() == effLoc.getBlockY();
                } else {
                    int markerY = effLoc.getBlockY();
                    int playerY = playerLoc.getBlockY();
                    inZone = playerY == markerY || playerY == markerY + 1;
                }
            }

            if (inZone) {
                Effect effect = effectMarker.getEffect();
                if (effect != null) {
                    int level = Math.max(1, effectMarker.getAmplifier());
                    return new ActiveEffect(effect, level);
                }
            }
        }
        return null;
    }

    private void applyEffect(Player player, ActiveEffect effect) {
        if (effect == null || effect.effect() == null) return;
        effect.effect().apply(player, effect.level());
    }

    private void clearEffect(Player player, ActiveEffect effect) {
        if (effect == null || effect.effect() == null) return;
        effect.effect().clear(player);
    }

    private boolean isSameEffect(ActiveEffect first, ActiveEffect second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.effect().getEffectName().equalsIgnoreCase(second.effect().getEffectName()) && first.level() == second.level();
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

        ActiveEffect current = activeEffects.remove(player.getUniqueId());
        if (current != null) {
            clearEffect(player, current);
        }
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        progress.setFinished(true);
        createSpectatorHead(player);

        if (gameManager.getScoreboardManager() != null) {
            gameManager.getScoreboardManager().updateAllScoreboards();
        }
    }

    private Location getMarkerBase(int castleIndex, String typeId) {
        if (castleIndex < 0 || castleIndex >= castles.size()) return null;
        World castleWorld = castles.get(castleIndex);
        PlayerCastle playerCastle = findCastleByWorld(castleWorld);
        if (playerCastle == null) return null;

        return playerCastle.getMarker(typeId).stream().findFirst().map(MarkerData::getLocation).orElse(null);
    }

    private boolean isOnTopOfMarker(Location playerLoc, Location markerLoc) {
        return playerLoc.getBlockX() == markerLoc.getBlockX() && playerLoc.getBlockZ() == markerLoc.getBlockZ() && playerLoc.getBlockY() == markerLoc.getBlockY() + 1;
    }

    private void teleportSpectatorToStart(Player player, int castleIndex) {
        Location target = getMarkerBase(castleIndex, "start");
        if (target == null) return;
        player.teleport(target.clone().add(0.5, 1, 0.5));
    }

    private void teleportSpectatorToEnd(Player player, int castleIndex) {
        Location target = getMarkerBase(castleIndex, "end");
        if (target == null) return;
        player.teleport(target.clone().add(0.5, 1, 0.5));
    }

    private int getCastleIndexByWorld(World world) {
        if (castles == null || world == null) return -1;
        for (int i = 0; i < castles.size(); i++) {
            if (castles.get(i).equals(world)) {
                return i;
            }
        }
        return -1;
    }

    public void createSpectatorHead(Player spectator) {
        removeSpectatorHead(spectator.getUniqueId());

        ArmorStand stand = spectator.getWorld().spawn(spectator.getLocation().add(0, 0.3, 0), ArmorStand.class, as -> {
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
                    stand.teleport(p.getLocation().add(0, 0.3, 0));
                }

                if (spectatorHeads.isEmpty()) {
                    cancel();
                    spectatorHeadTask = null;
                }
            }
        };

        spectatorHeadTask.runTaskTimer(gameManager.getPlugin(), 0L, 1L);
    }

    private PlayerCastle findCastleByWorld(World world) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerCastle playerCastle = gameManager.getPlayerCastle(p);
            if (playerCastle != null && playerCastle.getCasleWorld().equals(world)) {
                return playerCastle;
            }
        }
        return null;
    }

    private String getWorldOwnerName(World world) {
        return world.getName().replace("castle_rush_", "");
    }

    public void cleanup() {
        for (Map.Entry<UUID, ActiveEffect> entry : activeEffects.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                clearEffect(p, entry.getValue());
            }
        }
        activeEffects.clear();
        playerProgress.clear();
        if (castles != null) {
            castles.clear();
        }
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
        return castles != null ? castles.size() : 0;
    }

    private record ActiveEffect(com.hnp_arda.castlerush.tools.effect.Effect effect, int level) {}

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
