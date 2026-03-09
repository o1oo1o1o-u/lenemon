package com.lenemon.discord;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
                    CommandManager.literal("discord")
                            .executes(ctx -> executePublic(ctx.getSource()))
            );

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
                                                DiscordLinkConfig.load();
                                                String url = DiscordWebhookConfig.get().getWebhookUrl();
                                                boolean hasUrl = url != null && !url.isBlank();
                                                ctx.getSource().sendFeedback(
                                                        () -> Text.literal("§e[Lenemon] Config Discord rechargée. URL : "
                                                                + (hasUrl ? "§a✔ définie" : "§c✘ manquante")
                                                                + " §7| Lien public : §b" + DiscordLinkConfig.get().getInviteUrl()), true);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
            );
        });
    }

    private static int executePublic(ServerCommandSource source) {
        DiscordLinkConfig config = DiscordLinkConfig.get();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("[Lenemon] Discord : " + config.getInviteUrl()), false);
            return Command.SINGLE_SUCCESS;
        }

        StringBuilder spacer = new StringBuilder();
        for (int i = 0; i < config.getBlankLinesAboveLogo(); i++) {
            spacer.append('\n');
        }

        player.sendMessage(Text.literal(spacer + config.getLogoGlyph())
                .formatted(Formatting.WHITE), false);

        player.sendMessage(
                Text.literal(config.getPromptText())
                        .append(Text.literal(config.getClickText())
                                .setStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, config.getInviteUrl()))
                                        .withColor(Formatting.AQUA)
                                        .withUnderline(true)
                                        .withBold(true))),
                false
        );
        return Command.SINGLE_SUCCESS;
    }
}
