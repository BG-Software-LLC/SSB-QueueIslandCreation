package com.bgsoftware.ssbqueueislandcreation;

import com.bgsoftware.ssbqueueislandcreation.lang.Message;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.algorithm.DelegateIslandCreationAlgorithm;
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

public final class QueuedIslandCreationAlgorithm extends DelegateIslandCreationAlgorithm {

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
        super(original);
        this.plugin = plugin;
        this.original = original;
        this.queueInterval = queueInterval;
        this.creationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::createIslandTask, queueInterval, queueInterval);
        this.notifyTask = Bukkit.getScheduler().runTaskTimer(plugin, this::notifyPendingPlayers, 20L, 20L);
    }

    @Override
    public CompletableFuture<IslandCreationResult> createIsland(Island.Builder builder, BlockPosition lastIsland) {
        IslandCreationRequest creationRequest = new IslandCreationRequest(builder);
        boolean isQueueEmpty = this.requestQueue.isEmpty();

        if (isQueueEmpty && canCreateIslandNow) {
            this.createIsland(creationRequest);
        } else {
            requestQueue.add(creationRequest);
            if (!builder.getName().isEmpty())
                pendingIslandNames.add(builder.getName().toLowerCase(Locale.ENGLISH));
            pendingOwners.add(builder.getOwner().getUniqueId());

            int queueSize = this.requestQueue.size();
            Message.QUEUE_UPDATE.send(builder.getOwner(), queueSize, queueSize,
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
            Message.QUEUE_UPDATE.send(pendingRequest.builder.getOwner(), currentQueueIndex, queueSize,
                    TimeUnit.MILLISECONDS.toSeconds(currentQueueIndex * queueInterval * 50));
            currentQueueIndex++;
        }
    }

    private void createIsland(IslandCreationRequest request) {
        canCreateIslandNow = false;
        BlockPosition lastIsland = plugin.getFactory().createBlockPosition(plugin.getGrid().getLastIslandLocation());
        original.createIsland(request.builder, lastIsland).whenComplete(((islandCreationResult, error) -> {
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
        pendingIslandNames.remove(request.builder.getName().toLowerCase(Locale.ENGLISH));
        pendingOwners.remove(request.builder.getOwner().getUniqueId());

        createIsland(request);
    }

    private static class IslandCreationRequest {

        private final Island.Builder builder;
        private final CompletableFuture<IslandCreationResult> islandCreationResultFuture;

        IslandCreationRequest(Island.Builder builder) {
            this.builder = builder;
            this.islandCreationResultFuture = new CompletableFuture<>();
        }

    }

}
