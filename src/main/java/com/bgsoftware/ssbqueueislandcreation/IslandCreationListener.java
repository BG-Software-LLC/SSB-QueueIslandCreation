package com.bgsoftware.ssbqueueislandcreation;

import com.bgsoftware.ssbqueueislandcreation.lang.Message;
import com.bgsoftware.superiorskyblock.api.events.PreIslandCreateEvent;
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
            Message.ALREADY_ACTIVE_REQUEST.send(event.getPlayer());
        }
    }

}
