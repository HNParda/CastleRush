package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.GameManager;
import com.hnp_arda.castlerush.RaceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import static com.hnp_arda.castlerush.GameManager.languageManager;

public class EndTool extends Tool {

    public EndTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public String getName() {
        return "End";
    }

    @Override
    public Material getToolItem() {
        return Material.GOLDEN_AXE;
    }

    @Override
    public void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle) {
        if (event.getClickedBlock() == null) return;
        event.getPlayer().sendMessage("");
        playerCastle.setEnd(event.getClickedBlock().getLocation());
        placeSimpleMarker(event.getPlayer(), playerCastle, getTypeId(), event.getClickedBlock().getLocation());
        revealMarkers(event.getPlayer(), playerCastle, getTypeId());
        event.getPlayer().sendMessage("");
    }

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {
        revealMarkers(player, playerCastle, getTypeId());
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {
        hideMarkers(player, playerCastle, getTypeId());
    }

    @Override
    protected Material getDisplayMaterial(World world, MarkerData marker) {
        return Material.DIAMOND_BLOCK;
    }

    @Override
    public void triggerEnter(Player player, MarkerData marker) {

        RaceManager.RaceProgress progress = gameManager.getRaceManager().getPlayerProgress().get(player.getUniqueId());

        int castleIndex = progress.getCurrentCastle();

        boolean isLastCastle = gameManager.getRaceManager().getTotalCastles() == castleIndex + 1;


        if (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() == null) {
            if (isLastCastle) return;

            PlayerCastle playerCastle = gameManager.getRaceManager().getCastle(castleIndex + 1);
            Location target = playerCastle.getEnd();
            if (target == null) return;
            player.teleport(target.clone().add(0.5, 1, 0.5));
            return;
        }

        PlayerCastle playerCastle = gameManager.getRaceManager().getCastle(castleIndex);


        long duration = progress.finishCastle();
        String ownerName = gameManager.getRaceManager().getCastleOwnerName(playerCastle);

        int minutes = (int) (duration / 60);
        int seconds = (int) (duration % 60);

        Bukkit.broadcast(Component.text(languageManager.get("race.castle_complete", player.getName(), ownerName, minutes, seconds), NamedTextColor.GREEN));

        gameManager.getRaceManager().teleportToStart(player, castleIndex + 1);

        if (gameManager.getScoreboardManager() != null) {
            gameManager.getScoreboardManager().updateAllScoreboards();
        }

    }

    @Override
    public void triggerExit(Player player) {

    }
}
