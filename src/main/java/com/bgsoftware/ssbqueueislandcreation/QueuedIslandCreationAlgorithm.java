package com.bgsoftware.ssbqueueislandcreation;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.schematic.Schematic;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import com.bgsoftware.superiorskyblock.api.wrappers.BlockPosition;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class QueuedIslandCreationAlgorithm implements IslandCreationAlgorithm {

    private final Queue<IslandCreationRequest> requestQueue = new LinkedList<>();
    private final List<String> pendingIslandNames = new ArrayList<>();

    private final SuperiorSkyblock plugin;
    private final IslandCreationAlgorithm original;
    private final BukkitTask creationTask;

    private boolean canCreateIslandNow = true;

    public QueuedIslandCreationAlgorithm(SuperiorSkyblock plugin, IslandCreationAlgorithm original) {
        this.plugin = plugin;
        this.original = original;
        this.creationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::createIslandTask, 100L, 100L);
    }

    @Override
    public CompletableFuture<IslandCreationResult> createIsland(UUID islandUUID, SuperiorPlayer owner,
                                                                BlockPosition lastIsland, String islandName,
                                                                Schematic schematic) {
        IslandCreationRequest creationRequest = new IslandCreationRequest(islandUUID, owner, islandName, schematic);
        boolean isQueueEmpty = this.requestQueue.isEmpty();

        requestQueue.add(creationRequest);
        pendingIslandNames.add(islandName.toLowerCase(Locale.ENGLISH));

        if (isQueueEmpty && canCreateIslandNow)
            this.createIslandTask();

        return creationRequest.islandCreationResultFuture;
    }

    public boolean hasActiveRequest(String islandName) {
        return pendingIslandNames.contains(islandName.toLowerCase(Locale.ENGLISH));
    }

    public BukkitTask getCreationTask() {
        return this.creationTask;
    }

    private void createIslandTask() {
        if (requestQueue.isEmpty())
            return;

        canCreateIslandNow = false;

        IslandCreationRequest request = requestQueue.remove();
        pendingIslandNames.remove(request.islandName.toLowerCase(Locale.ENGLISH));

        BlockPosition lastIsland = plugin.getFactory().createBlockPosition(plugin.getGrid().getLastIslandLocation());
        original.createIsland(request.islandUUID, request.owner, lastIsland, request.islandName, request.schematic)
                .whenComplete(((islandCreationResult, error) -> {
                    if (error != null) {
                        request.islandCreationResultFuture.completeExceptionally(error);
                    } else {
                        request.islandCreationResultFuture.complete(islandCreationResult);
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        canCreateIslandNow = true;
                        this.createIslandTask();
                    }, 100L);
                }));
    }

    private static class IslandCreationRequest {

        private final UUID islandUUID;
        private final SuperiorPlayer owner;
        private final String islandName;
        private final Schematic schematic;
        private final CompletableFuture<IslandCreationResult> islandCreationResultFuture;

        IslandCreationRequest(UUID islandUUID, SuperiorPlayer owner, String islandName, Schematic schematic) {
            this.islandUUID = islandUUID;
            this.owner = owner;
            this.islandName = islandName;
            this.schematic = schematic;
            this.islandCreationResultFuture = new CompletableFuture<>();
        }

    }

}
