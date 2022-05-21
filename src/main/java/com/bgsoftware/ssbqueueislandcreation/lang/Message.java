package com.bgsoftware.ssbqueueislandcreation.lang;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.ssbqueueislandcreation.QueueIslandCreationModule;
import com.bgsoftware.superiorskyblock.api.service.message.IMessageComponent;
import com.bgsoftware.superiorskyblock.api.service.message.MessagesService;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public enum Message {

    ALREADY_ACTIVE_REQUEST,
    QUEUE_UPDATE;

    private static final QueueIslandCreationModule module = QueueIslandCreationModule.getInstance();

    private static final MessagesService MESSAGES_SERVICE = Objects.requireNonNull(Bukkit.getServicesManager().getRegistration(MessagesService.class)).getProvider();
    private static final IMessageComponent EMPTY_COMPONENT = MESSAGES_SERVICE.newBuilder().build();

    private final Map<Locale, IMessageComponent> messages = new HashMap<>();

    public void send(SuperiorPlayer superiorPlayer, Object... objects) {
        send(superiorPlayer.asPlayer(), superiorPlayer.getUserLocale(), objects);
    }

    public void send(CommandSender sender, java.util.Locale locale, Object... args) {
        if (sender != null)
            messages.getOrDefault(locale, EMPTY_COMPONENT).sendMessage(sender, args);
    }

    private void setMessage(java.util.Locale locale, IMessageComponent messageComponent) {
        messages.put(locale, messageComponent);
    }

    public static void reload() {
        QueueIslandCreationModule.log("Loading messages started...");
        long startTime = System.currentTimeMillis();

        File langFolder = new File(module.getDataFolder(), "lang");

        if (!langFolder.exists()) {
            module.saveResource("lang/en-US.yml");
        }

        int messagesAmount = 0;
        boolean countMessages = true;

        for (File langFile : Objects.requireNonNull(langFolder.listFiles())) {
            String fileName = langFile.getName().split("\\.")[0];
            java.util.Locale fileLocale;

            try {
                fileLocale = LocaleUtils.getLocale(fileName);
            } catch (IllegalArgumentException ex) {
                QueueIslandCreationModule.log("&cThe language \"" + fileName + "\" is invalid. Please correct the file name.");
                continue;
            }

            CommentedConfiguration cfg = CommentedConfiguration.loadConfiguration(langFile);
            InputStream inputStream = module.getResource("lang/" + langFile.getName());

            try {
                cfg.syncWithConfig(langFile, inputStream == null ? module.getResource("lang/en-US.yml") :
                        inputStream, "lang/en-US.yml");
            } catch (IOException error) {
                throw new RuntimeException(error);
            }

            for (Message message : values()) {
                message.setMessage(fileLocale, MESSAGES_SERVICE.parseComponent(cfg, message.name()));
                if (countMessages)
                    messagesAmount++;
            }

            countMessages = false;
        }

        QueueIslandCreationModule.log(" - Found " + messagesAmount + " messages in the language files.");
        QueueIslandCreationModule.log("Loading messages done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

}
