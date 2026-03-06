// src/client/java/com/lenemon/guieditor/GuiEditorClient.java
package com.lenemon.guieditor;

import com.lenemon.guieditor.editor.EditorIO;
import com.lenemon.guieditor.editor.EditorState;
import com.lenemon.guieditor.screen.GuiEditorScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * The type Gui editor client.
 */
public final class GuiEditorClient {

    /**
     * The constant MOD_ID.
     */
    public static final String MOD_ID = "lenemon_gui_editor";
    private static boolean openRequested = false;

    private GuiEditorClient() {}

    /**
     * Init.
     */
    public static void init() {
        EditorIO.initFolders();
        com.lenemon.guieditor.overlay.GuiEditorOverlay.init();
        // Ouvrir au tick suivant pour éviter que le chat ferme l'écran
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!openRequested) return;
            openRequested = false;
            if (client.player == null) return;

            client.setScreen(new GuiEditorScreen());
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("guieditor")
                    .executes(ctx -> {
                        requestOpen(ctx.getSource());
                        return 1;
                    })
                    .then(literal("open").executes(ctx -> {
                        com.lenemon.guieditor.overlay.GuiEditorOverlay.toggle();
                        ctx.getSource().sendFeedback(Text.literal("[GUI Editor] toggled"));
                        return 1;
                    }))
                    .then(literal("export").executes(ctx -> {
                        FabricClientCommandSource src = ctx.getSource();
                        String name = "scene";
                        try {
                            EditorIO.saveScene(name, EditorState.get().scene);
                            src.sendFeedback(Text.literal("[GUI Editor] Exported to " + EditorIO.scenePath(name)));
                        } catch (Exception e) {
                            src.sendError(Text.literal("[GUI Editor] Export failed: " + e.getMessage()));
                        }
                        return 1;
                    }))
            );
        });
    }

    private static void requestOpen(FabricClientCommandSource src) {
        openRequested = true;
        src.sendFeedback(Text.literal("[GUI Editor] Opening..."));
    }
}