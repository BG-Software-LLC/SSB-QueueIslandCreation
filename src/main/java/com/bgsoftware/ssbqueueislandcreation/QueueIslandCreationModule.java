package com.bgsoftware.ssbqueueislandcreation;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.commands.SuperiorCommand;
import com.bgsoftware.superiorskyblock.api.modules.ModuleLoadTime;
import com.bgsoftware.superiorskyblock.api.modules.PluginModule;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

public final class QueueIslandCreationModule extends PluginModule {

    private QueuedIslandCreationAlgorithm algorithm;

    public QueueIslandCreationModule() {
        super("QueueIslandCreation", "Ome_R");
    }

    @Override
    public void onEnable(SuperiorSkyblock plugin) {
        algorithm = new QueuedIslandCreationAlgorithm(plugin, plugin.getGrid().getIslandCreationAlgorithm());
        plugin.getGrid().setIslandCreationAlgorithm(algorithm);
    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {
    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        plugin.getGrid().setIslandCreationAlgorithm(null);
        algorithm.getCreationTask().cancel();
    }

    @Nullable
    @Override
    public Listener[] getModuleListeners(SuperiorSkyblock plugin) {
        return new Listener[]{new IslandCreationListener(this)};
    }

    @Nullable
    @Override
    public SuperiorCommand[] getSuperiorCommands(SuperiorSkyblock plugin) {
        return null;
    }

    @Nullable
    @Override
    public SuperiorCommand[] getSuperiorAdminCommands(SuperiorSkyblock plugin) {
        return null;
    }

    @Override
    public ModuleLoadTime getLoadTime() {
        return ModuleLoadTime.AFTER_HANDLERS_LOADING;
    }

    public QueuedIslandCreationAlgorithm getAlgorithm() {
        return algorithm;
    }

}
