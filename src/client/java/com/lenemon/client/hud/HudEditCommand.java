package com.lenemon.client.hud;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * The type Hud edit command.
 */
public class HudEditCommand {

    /**
     * Register.
     */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("lenemonhud")
                            .then(literal("edit")
                                    .then(literal("x")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "x", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("y")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "y", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("panelwidth")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "panelwidth", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("panelheight")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "panelheight", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("coinoffsetx")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "coinoffsetx", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("coinoffsety")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "coinoffsety", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("coinsize")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "coinsize", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("textoffsetx")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "textoffsetx", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("textoffsety")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "textoffsety", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("textscale")
                                            .then(argument("value", FloatArgumentType.floatArg(0.1f, 5f))
                                                    .executes(ctx -> setFloat(ctx, "textscale", FloatArgumentType.getFloat(ctx, "value")))))
                                    .then(literal("balancepanelw")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "balancepanelw", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("pantex")
                                            .then(argument("px", IntegerArgumentType.integer())
                                                    .then(argument("py", IntegerArgumentType.integer())
                                                            .executes(ctx -> {
                                                                HudConfig.panelTexX = IntegerArgumentType.getInteger(ctx, "px");
                                                                HudConfig.panelTexY = IntegerArgumentType.getInteger(ctx, "py");
                                                                feedback(ctx, "panelTexX=" + HudConfig.panelTexX + " panelTexY=" + HudConfig.panelTexY);
                                                                return 1;
                                                            }))))
                                    .then(literal("pantexw")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "pantexw", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("pantexh")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "pantexh", IntegerArgumentType.getInteger(ctx, "value")))))
                                    // Hunter
                                    .then(literal("hunteroffset")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "hunteroffset", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("hunterpanelw")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "hunterpanelw", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("hunterpanelh")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "hunterpanelh", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("baroffsetx")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "baroffsetx", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("baroffsety")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "baroffsety", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("barwidth")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "barwidth", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("barheight")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "barheight", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("levelscale")
                                            .then(argument("value", FloatArgumentType.floatArg(0.1f, 5f))
                                                    .executes(ctx -> setFloat(ctx, "levelscale", FloatArgumentType.getFloat(ctx, "value")))))
                                    .then(literal("leveltextoffsetx")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "leveltextoffsetx", IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(literal("leveltextoffsety")
                                            .then(argument("value", IntegerArgumentType.integer())
                                                    .executes(ctx -> set(ctx, "leveltextoffsety", IntegerArgumentType.getInteger(ctx, "value")))))
                            )
                            .then(literal("print")
                                    .executes(ctx -> {
                                        feedback(ctx,
                                                "x=" + HudConfig.x +
                                                        " y=" + HudConfig.y +
                                                        " panelWidth=" + HudConfig.panelWidth +
                                                        " panelHeight=" + HudConfig.panelHeight +
                                                        " coinOffsetX=" + HudConfig.coinOffsetX +
                                                        " coinOffsetY=" + HudConfig.coinOffsetY +
                                                        " coinSize=" + HudConfig.coinSize +
                                                        " textOffsetX=" + HudConfig.textOffsetX +
                                                        " textOffsetY=" + HudConfig.textOffsetY +
                                                        " textScale=" + HudConfig.textScale +
                                                        " balancePanelW=" + HudConfig.balancePanelW +
                                                        " panelTexX=" + HudConfig.panelTexX +
                                                        " panelTexY=" + HudConfig.panelTexY +
                                                        " panelTexW=" + HudConfig.panelTexW +
                                                        " panelTexH=" + HudConfig.panelTexH +
                                                        " hunterOffset=" + HudConfig.hunterPanelOffsetY +
                                                        " hunterPanelW=" + HudConfig.hunterPanelW +
                                                        " hunterPanelH=" + HudConfig.hunterPanelH +
                                                        " barOffsetX=" + HudConfig.barOffsetX +
                                                        " barOffsetY=" + HudConfig.barOffsetY +
                                                        " barWidth=" + HudConfig.barWidth +
                                                        " barHeight=" + HudConfig.barHeight +
                                                        " levelScale=" + HudConfig.levelTextScale +
                                                        " levelTextOffsetX=" + HudConfig.levelTextOffsetX +
                                                        " levelTextOffsetY=" + HudConfig.levelTextOffsetY
                                        );
                                        return 1;
                                    }))
            );
        });
    }

    private static int set(CommandContext<FabricClientCommandSource> ctx, String key, int value) {
        switch (key) {
            case "x"               -> HudConfig.x = value;
            case "y"               -> HudConfig.y = value;
            case "panelwidth"      -> HudConfig.panelWidth = value;
            case "panelheight"     -> HudConfig.panelHeight = value;
            case "coinoffsetx"     -> HudConfig.coinOffsetX = value;
            case "coinoffsety"     -> HudConfig.coinOffsetY = value;
            case "coinsize"        -> HudConfig.coinSize = value;
            case "textoffsetx"     -> HudConfig.textOffsetX = value;
            case "textoffsety"     -> HudConfig.textOffsetY = value;
            case "balancepanelw"   -> HudConfig.balancePanelW = value;
            case "pantexw"         -> HudConfig.panelTexW = value;
            case "pantexh"         -> HudConfig.panelTexH = value;
            case "hunteroffset"    -> HudConfig.hunterPanelOffsetY = value;
            case "hunterpanelw"    -> HudConfig.hunterPanelW = value;
            case "hunterpanelh"    -> HudConfig.hunterPanelH = value;
            case "baroffsetx"      -> HudConfig.barOffsetX = value;
            case "baroffsety"      -> HudConfig.barOffsetY = value;
            case "barwidth"        -> HudConfig.barWidth = value;
            case "barheight"       -> HudConfig.barHeight = value;
            case "leveltextoffsetx"-> HudConfig.levelTextOffsetX = value;
            case "leveltextoffsety"-> HudConfig.levelTextOffsetY = value;
        }
        feedback(ctx, key + " = " + value);
        return 1;
    }

    private static int setFloat(CommandContext<FabricClientCommandSource> ctx, String key, float value) {
        switch (key) {
            case "textscale"  -> HudConfig.textScale = value;
            case "levelscale" -> HudConfig.levelTextScale = value;
        }
        feedback(ctx, key + " = " + value);
        return 1;
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(Text.literal("§a[HudEdit] " + msg));
    }
}