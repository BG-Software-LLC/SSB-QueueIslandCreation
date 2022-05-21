package com.bgsoftware.ssbqueueislandcreation;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.ssbqueueislandcreation.lang.Message;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.commands.SuperiorCommand;
import com.bgsoftware.superiorskyblock.api.modules.ModuleLoadTime;
import com.bgsoftware.superiorskyblock.api.modules.PluginModule;
import com.bgsoftware.superiorskyblock.api.service.message.MessagesService;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public final class QueueIslandCreationModule extends PluginModule {

    private static QueueIslandCreationModule instance;

    private QueuedIslandCreationAlgorithm algorithm;
    private MessagesService messagesService;

    private long queueInterval;

    public QueueIslandCreationModule() {
        super("QueueIslandCreation", "Ome_R");
        instance = this;
    }

    @Override
    public void onEnable(SuperiorSkyblock plugin) {
        RegisteredServiceProvider<MessagesService> messagesServiceProvider = Bukkit.getServicesManager().getRegistration(MessagesService.class);

        if (messagesServiceProvider == null)
            throw new IllegalStateException("Missing MessagesService class.");

        this.messagesService = messagesServiceProvider.getProvider();

        onReload(plugin);

        algorithm = new QueuedIslandCreationAlgorithm(plugin, plugin.getGrid().getIslandCreationAlgorithm(), queueInterval);
        plugin.getGrid().setIslandCreationAlgorithm(algorithm);

    }

    @Override
    public void onReload(SuperiorSkyblock plugin) {
        Message.reload();
        loadConfig();
    }

    @Override
    public void onDisable(SuperiorSkyblock plugin) {
        plugin.getGrid().setIslandCreationAlgorithm(null);
        algorithm.getCreationTask().cancel();
        algorithm.getNotifyTask().cancel();
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

    public MessagesService getMessagesService() {
        return messagesService;
    }

    public static void log(String message) {
        instance.getLogger().info(message);
    }

    public static QueueIslandCreationModule getInstance() {
        return instance;
    }

    private void loadConfig() {
        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists())
            saveResource("config.yml");

        CommentedConfiguration cfg = CommentedConfiguration.loadConfiguration(file);

        try {
            cfg.syncWithConfig(file, getResource("config.yml"), "config.yml");
        } catch (IOException error) {
            throw new RuntimeException(error);
        }

        queueInterval = cfg.getLong("queue-interval", 100L);
    }

}
