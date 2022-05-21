package com.bgsoftware.ssbqueueislandcreation;

import com.bgsoftware.ssbqueueislandcreation.lang.Message;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.schematic.Schematic;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import com.bgsoftware.superiorskyblock.api.wrappers.BlockPosition;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class QueuedIslandCreationAlgorithm implements IslandCreationAlgorithm {

    private final Queue<IslandCreationRequest> requestQueue = new LinkedList<>();
    private final Set<String> pendingIslandNames = new HashSet<>();
    private final Set<UUID> pendingOwners = new HashSet<>();

    private final SuperiorSkyblock plugin;
    private final IslandCreationAlgorithm original;
    private final long queueInterval;
    private final BukkitTask creationTask;
    private final BukkitTask notifyTask;

    private boolean canCreateIslandNow = true;

    public QueuedIslandCreationAlgorithm(SuperiorSkyblock plugin, IslandCreationAlgorithm original, long queueInterval) {
        this.plugin = plugin;
        this.original = original;
        this.queueInterval = queueInterval;
        this.creationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::createIslandTask, queueInterval, queueInterval);
        this.notifyTask = Bukkit.getScheduler().runTaskTimer(plugin, this::notifyPendingPlayers, 20L, 20L);
    }

    @Override
    public CompletableFuture<IslandCreationResult> createIsland(UUID islandUUID, SuperiorPlayer owner,
                                                                BlockPosition lastIsland, String islandName,
                                                                Schematic schematic) {
        IslandCreationRequest creationRequest = new IslandCreationRequest(islandUUID, owner, islandName, schematic);
        boolean isQueueEmpty = this.requestQueue.isEmpty();

        if (isQueueEmpty && canCreateIslandNow) {
            this.createIsland(creationRequest);
        } else {
            requestQueue.add(creationRequest);
            pendingIslandNames.add(islandName.toLowerCase(Locale.ENGLISH));
            pendingOwners.add(owner.getUniqueId());

            int queueSize = this.requestQueue.size();
            Message.QUEUE_UPDATE.send(owner, queueSize, queueSize,
                    TimeUnit.MILLISECONDS.toSeconds(queueSize * queueInterval * 50));
        }

        return creationRequest.islandCreationResultFuture;
    }

    public boolean hasActiveRequest(String islandName) {
        return pendingIslandNames.contains(islandName.toLowerCase(Locale.ENGLISH));
    }

    public boolean hasActiveRequest(SuperiorPlayer superiorPlayer) {
        return pendingOwners.contains(superiorPlayer.getUniqueId());
    }

    public BukkitTask getCreationTask() {
        return this.creationTask;
    }

    public BukkitTask getNotifyTask() {
        return this.notifyTask;
    }

    private void notifyPendingPlayers() {
        int currentQueueIndex = 1;
        int queueSize = requestQueue.size();
        for (IslandCreationRequest pendingRequest : requestQueue) {
            Message.QUEUE_UPDATE.send(pendingRequest.owner, currentQueueIndex, queueSize,
                    TimeUnit.MILLISECONDS.toSeconds(currentQueueIndex * queueInterval * 50));
            currentQueueIndex++;
        }
    }

    private void createIsland(IslandCreationRequest request) {
        canCreateIslandNow = false;
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
                    }, queueInterval);
                }));
    }

    private void createIslandTask() {
        if (requestQueue.isEmpty())
            return;

        IslandCreationRequest request = requestQueue.remove();
        pendingIslandNames.remove(request.islandName.toLowerCase(Locale.ENGLISH));
        pendingOwners.remove(request.owner.getUniqueId());

        createIsland(request);
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
