package com.lenemon.discord;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

/**
 * The type Discord command.
 */
public class DiscordCommand {

    /**
     * Register.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("lenemon")
                            .requires(src -> src.hasPermissionLevel(2))
                            .then(CommandManager.literal("discord")
                                    .then(CommandManager.literal("enable")
                                            .executes(ctx -> {
                                                DiscordWebhookConfig.get().setEnabled(true);
                                                ctx.getSource().sendFeedback(
                                                        () -> Text.literal("§a[Lenemon] Webhook Discord §lactivé§r§a."), true);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                                    .then(CommandManager.literal("disable")
                                            .executes(ctx -> {
                                                DiscordWebhookConfig.get().setEnabled(false);
                                                ctx.getSource().sendFeedback(
                                                        () -> Text.literal("§c[Lenemon] Webhook Discord §ldésactivé§r§c."), true);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                                    .then(CommandManager.literal("reload")
                                            .executes(ctx -> {
                                                DiscordWebhookConfig.load();
                                                String url = DiscordWebhookConfig.get().getWebhookUrl();
                                                boolean hasUrl = url != null && !url.isBlank();
                                                ctx.getSource().sendFeedback(
                                                        () -> Text.literal("§e[Lenemon] Config Discord rechargée. URL : "
                                                                + (hasUrl ? "§a✔ définie" : "§c✘ manquante")), true);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
            );
        });
    }
}