package com.bgsoftware.ssbqueueislandcreation;

import com.bgsoftware.superiorskyblock.api.events.PreIslandCreateEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class IslandCreationListener implements Listener {

    private final QueueIslandCreationModule module;

    public IslandCreationListener(QueueIslandCreationModule module) {
        this.module = module;
    }

    @EventHandler
    public void onIslandCreate(PreIslandCreateEvent event) {
        if (this.module.getAlgorithm().hasActiveRequest(event.getIslandName())) {
            event.setCancelled(true);

            // TODO: Actual message sending

            Player player = event.getPlayer().asPlayer();
            assert player != null;
            player.sendMessage(ChatColor.RED + "No!");
        }
    }

}
