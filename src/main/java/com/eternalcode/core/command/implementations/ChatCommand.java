/*
 * Copyright (c) 2021. EternalCode.pl
 */

package com.eternalcode.core.command.implementations;

import com.eternalcode.core.configuration.ConfigurationManager;
import com.eternalcode.core.configuration.MessagesConfiguration;
import com.eternalcode.core.utils.ChatUtils;
import net.dzikoysk.funnycommands.commands.CommandInfo;
import net.dzikoysk.funnycommands.resources.ValidationException;
import net.dzikoysk.funnycommands.stereotypes.FunnyCommand;
import net.dzikoysk.funnycommands.stereotypes.FunnyComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.stream.IntStream;

import static com.eternalcode.core.command.Valid.when;

@FunnyComponent
public final class ChatCommand {
    public static boolean chatMuted = false;

    private final MessagesConfiguration messagesConfiguration;

    public ChatCommand(ConfigurationManager configurationManager) {
        this.messagesConfiguration = configurationManager.getMessagesConfiguration();
    }

    @FunnyCommand(
        name = "chat",
        aliases = {"czat"},
        permission = "eternalcore.command.chat",
        usage = "&cPoprawne użycie &7/chat <clear/on/off>",
        acceptsExceeded = true
    )
    public void execute(CommandSender sender, String[] args, CommandInfo commandInfo) {
        when(args.length < 1, commandInfo.getUsageMessage());
        switch (args[0].toLowerCase()) {
            case "clear" -> {
                IntStream.range(0, 100).forEach(i -> Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(" ")));
                Bukkit.broadcast(Component.text(ChatUtils.color(messagesConfiguration.chatCleared).replace("{NICK}", sender.getName())));
            }
            case "on" -> {
                if (!chatMuted) {
                    sender.sendMessage(ChatUtils.color(messagesConfiguration.chatAlreadyEnabled));
                    return;
                }

                chatMuted = false;
                Bukkit.broadcast(Component.text(ChatUtils.color(messagesConfiguration.chatEnabled).replace("{NICK}", sender.getName())));
            }
            case "off" -> {
                if (chatMuted) {
                    sender.sendMessage(ChatUtils.color(messagesConfiguration.chatAlreadyDisabled));
                    return;
                }

                chatMuted = true;
                Bukkit.broadcast(Component.text(ChatUtils.color(messagesConfiguration.chatDisabled).replace("{NICK}", sender.getName())));
            }
            default -> throw new ValidationException(commandInfo.getUsageMessage());
        }
    }
}

