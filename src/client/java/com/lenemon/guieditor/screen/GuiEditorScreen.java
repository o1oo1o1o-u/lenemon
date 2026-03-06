// src/client/java/com/lenemon/guieditor/screen/GuiEditorScreen.java
package com.lenemon.guieditor.screen;

import com.lenemon.guieditor.editor.EditorIO;
import com.lenemon.guieditor.editor.EditorState;
import com.lenemon.guieditor.editor.TextureCache;
import com.lenemon.guieditor.editor.model.Element;
import com.lenemon.guieditor.editor.model.ElementType;
import com.lenemon.guieditor.editor.model.Scene;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The type Gui editor screen.
 */
public class GuiEditorScreen extends Screen {

    private enum Tool {
        /**
         * Select tool.
         */
        SELECT,
        /**
         * Add rect tool.
         */
        ADD_RECT,
        /**
         * Add image tool.
         */
        ADD_IMAGE
    }

    private enum ResizeHandle {
        /**
         * None resize handle.
         */
        NONE,
        /**
         * Tl resize handle.
         */
        TL,
        /**
         * Tr resize handle.
         */
        TR,
        /**
         * Bl resize handle.
         */
        BL,
        /**
         * Br resize handle.
         */
        BR
    }

    private Tool tool = Tool.SELECT;

    private TextFieldWidget sceneNameField;
    private TextFieldWidget importPathField;
    private Text statusText = Text.empty();

    private boolean dragging = false;
    private int dragOffX = 0;
    private int dragOffY = 0;

    private ResizeHandle activeHandle = ResizeHandle.NONE;

    private static final int HANDLE_SIZE = 6;

    /**
     * Instantiates a new Gui editor screen.
     */
    public GuiEditorScreen() {
        super(Text.literal("GUI Editor"));
    }

    @Override
    protected void init() {
        Scene scene = EditorState.get().scene;

        int left = 10;
        int top = 10;
        int y = top;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Select"), b -> {
            tool = Tool.SELECT;
            status("Tool: SELECT (click to select and drag)");
        }).position(left, y).size(80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Rect"), b -> {
            tool = Tool.ADD_RECT;
            status("Tool: ADD_RECT (click in canvas to add)");
        }).position(left + 90, y).size(80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Image"), b -> {
            tool = Tool.ADD_IMAGE;
            status("Tool: ADD_IMAGE (paste path, Import, then click canvas)");
        }).position(left + 180, y).size(80, 20).build());

        y += 26;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> deleteSelected())
                .position(left, y).size(80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Export"), b -> exportScene())
                .position(left + 90, y).size(80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Load"), b -> loadScene())
                .position(left + 180, y).size(80, 20).build());

        y += 26;

        sceneNameField = new TextFieldWidget(this.textRenderer, left, y, 250, 20, Text.literal("Scene name"));
        sceneNameField.setText("scene");
        sceneNameField.setMaxLength(128);
        this.addDrawableChild(sceneNameField);

        y += 26;

        // Champ import large + support chemins longs
        importPathField = new TextFieldWidget(this.textRenderer, left, y, 520, 20, Text.literal("Local image path"));
        importPathField.setPlaceholder(Text.literal("C:\\\\...\\\\my.png or /home/.../my.png"));
        importPathField.setMaxLength(2048);
        this.addDrawableChild(importPathField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Import"), b -> importImageFromPath())
                .position(left + 530, y).size(80, 20).build());

        y += 26;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset Target 96,46-160,145"), b -> {
            scene.targetX = 96;
            scene.targetY = 46;
            scene.targetW = 160 - 96;
            scene.targetH = 145 - 46;
            status("Target reset.");
        }).position(left, y).size(260, 20).build());

        // Canvas sous l'UI (sinon les widgets bouffent les clics)
        int canvasTop = y + 30;
        scene.canvasX = 10;
        scene.canvasY = canvasTop;
        scene.canvasW = this.width - 20;
        scene.canvasH = this.height - canvasTop - 40;

        status("Ready. Click in canvas.");
    }

    private void status(String msg) {
        statusText = Text.literal(msg);
    }

    private void deleteSelected() {
        Scene scene = EditorState.get().scene;
        Element sel = scene.getSelected();
        if (sel == null) {
            status("Nothing selected.");
            return;
        }
        scene.elements.removeIf(e -> e.id.equals(sel.id));
        scene.selectedId = null;
        status("Deleted.");
    }

    private void exportScene() {
        String name = sceneNameField.getText();
        try {
            EditorIO.saveScene(name, EditorState.get().scene);
            status("Exported: " + EditorIO.scenePath(name));
        } catch (Exception e) {
            status("Export failed: " + e.getMessage());
        }
    }

    private void loadScene() {
        String name = sceneNameField.getText();
        try {
            Scene loaded = EditorIO.loadScene(name);
            EditorState.get().scene = loaded;
            TextureCache.clear();
            status("Loaded: " + name);

            // Re-apply canvas size based on current screen size
            Scene scene = EditorState.get().scene;
            int canvasTop = 10 + 26 + 26 + 26 + 26 + 30;
            scene.canvasX = 10;
            scene.canvasY = canvasTop;
            scene.canvasW = this.width - 20;
            scene.canvasH = this.height - canvasTop - 40;

        } catch (Exception e) {
            status("Load failed: " + e.getMessage());
        }
    }

    private void importImageFromPath() {
        String p = importPathField.getText();
        if (p == null) {
            status("Import failed: path is null");
            return;
        }

        // nettoyage: espaces, guillemets, underscore final (souvent le curseur visuel)
        p = p.trim();
        if ((p.startsWith("\"") && p.endsWith("\"")) || (p.startsWith("'") && p.endsWith("'"))) {
            p = p.substring(1, p.length() - 1).trim();
        }
        while (p.endsWith("_")) {
            p = p.substring(0, p.length() - 1).trim();
        }

        if (p.isBlank()) {
            status("Import failed: path is empty");
            return;
        }

        try {
            String relative = EditorIO.importImage(Path.of(p));
            status("Imported: " + relative + " (config/lenemon_gui_editor/images/)");
        } catch (Exception e) {
            status("Import failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // PAS de background de menu, PAS de voile plein écran -> pas de flou
        // On veut un overlay net au dessus du monde

        Scene scene = EditorState.get().scene;

        // Optionnel: juste un contour très léger du canvas (pas de fond)
        int cx = scene.canvasX;
        int cy = scene.canvasY;
        int cw = scene.canvasW;
        int ch = scene.canvasH;

        // contour blanc semi transparent
        int border = 0x80FFFFFF;
        ctx.fill(cx - 1, cy - 1, cx + cw + 1, cy, border);
        ctx.fill(cx - 1, cy + ch, cx + cw + 1, cy + ch + 1, border);
        ctx.fill(cx - 1, cy - 1, cx, cy + ch + 1, border);
        ctx.fill(cx + cw, cy - 1, cx + cw + 1, cy + ch + 1, border);

        // Target box (optionnel) juste en contour
        int tx = cx + scene.targetX;
        int ty = cy + scene.targetY;
        int tw = scene.targetW;
        int th = scene.targetH;

        int targetBorder = 0xA0FFFF00; // jaune semi visible
        ctx.fill(tx - 1, ty - 1, tx + tw + 1, ty, targetBorder);
        ctx.fill(tx - 1, ty + th, tx + tw + 1, ty + th + 1, targetBorder);
        ctx.fill(tx - 1, ty - 1, tx, ty + th + 1, targetBorder);
        ctx.fill(tx + tw, ty - 1, tx + tw + 1, ty + th + 1, targetBorder);

        // Dessine les éléments (au dessus)
        for (Element e : scene.elements) {
            drawElement(ctx, e);
        }

        // sélection (au dessus)
        Element sel = scene.getSelected();
        if (sel != null) {
            drawSelection(ctx, sel);
        }

        // Texte d'info en haut (au dessus)
        ctx.drawTextWithShadow(textRenderer, "Tool: " + tool.name(), 10, 4, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, statusText, 10, 16, 0xFFFFFF);

        // IMPORTANT: appeler super.render en dernier pour dessiner les widgets (boutons, fields) au dessus
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawElement(DrawContext ctx, Element e) {
        Scene scene = EditorState.get().scene;

        int x = scene.canvasX + e.x;
        int y = scene.canvasY + e.y;

        if (e.type == ElementType.RECT) {
            ctx.fill(x, y, x + e.w, y + e.h, e.argb);
            return;
        }

        if (e.type == ElementType.IMAGE) {
            if (e.imageFile == null) {
                ctx.fill(x, y, x + e.w, y + e.h, 0x55FF0000);
                ctx.drawTextWithShadow(textRenderer, "No image", x + 2, y + 2, 0xFFFFFF);
                return;
            }

            try {
                var entry = TextureCache.getOrLoad(e.imageFile);
                if (entry == null) return;

                // ICI la clé: texW/texH = taille réelle de l'image
                ctx.drawTexture(entry.id, x, y, 0, 0, e.w, e.h, entry.width, entry.height);
            } catch (IOException ex) {
                ctx.fill(x, y, x + e.w, y + e.h, 0x55FF0000);
                ctx.drawTextWithShadow(textRenderer, "Missing", x + 2, y + 2, 0xFFFFFF);
            }
        }
    }

    private void drawSelection(DrawContext ctx, Element e) {
        Scene scene = EditorState.get().scene;

        int x = scene.canvasX + e.x;
        int y = scene.canvasY + e.y;

        int c = 0xFFFFFFFF;
        ctx.fill(x - 1, y - 1, x + e.w + 1, y, c);
        ctx.fill(x - 1, y + e.h, x + e.w + 1, y + e.h + 1, c);
        ctx.fill(x - 1, y - 1, x, y + e.h + 1, c);
        ctx.fill(x + e.w, y - 1, x + e.w + 1, y + e.h + 1, c);

        drawHandle(ctx, x, y, ResizeHandle.TL);
        drawHandle(ctx, x + e.w, y, ResizeHandle.TR);
        drawHandle(ctx, x, y + e.h, ResizeHandle.BL);
        drawHandle(ctx, x + e.w, y + e.h, ResizeHandle.BR);
    }

    private void drawHandle(DrawContext ctx, int x, int y, ResizeHandle h) {
        int half = HANDLE_SIZE / 2;
        ctx.fill(x - half, y - half, x + half, y + half, 0xFF00FFFF);
    }

    private boolean isInsideCanvas(int mouseX, int mouseY) {
        Scene scene = EditorState.get().scene;
        int cx = scene.canvasX;
        int cy = scene.canvasY;
        return mouseX >= cx && mouseY >= cy && mouseX < cx + scene.canvasW && mouseY < cy + scene.canvasH;
    }

    private int toCanvasX(int mouseX) {
        return mouseX - EditorState.get().scene.canvasX;
    }

    private int toCanvasY(int mouseY) {
        return mouseY - EditorState.get().scene.canvasY;
    }

    @Override
    public boolean mouseClicked(double mouseXD, double mouseYD, int button) {
        int mouseX = (int) mouseXD;
        int mouseY = (int) mouseYD;

        // Important: laisser les widgets capter en premier
        if (super.mouseClicked(mouseXD, mouseYD, button)) {
            return true;
        }

        Scene scene = EditorState.get().scene;

        if (!isInsideCanvas(mouseX, mouseY)) {
            return false;
        }

        int cx = toCanvasX(mouseX);
        int cy = toCanvasY(mouseY);

        // Resize handles d'abord
        Element sel = scene.getSelected();
        if (sel != null) {
            ResizeHandle h = hitHandle(sel, mouseX, mouseY);
            if (h != ResizeHandle.NONE) {
                activeHandle = h;
                dragging = true;
                return true;
            }
        }

        if (tool == Tool.ADD_RECT) {
            Element e = new Element();
            e.type = ElementType.RECT;
            e.x = clamp(cx - 20);
            e.y = clamp(cy - 12);
            e.w = 40;
            e.h = 24;
            scene.elements.add(e);
            scene.select(e.id);
            status("Rect added.");
            return true;
        }

        if (tool == Tool.ADD_IMAGE) {
            Element e = new Element();
            e.type = ElementType.IMAGE;
            e.x = clamp(cx - 24);
            e.y = clamp(cy - 24);
            e.w = 48;
            e.h = 48;

            // Si un chemin est collé, on tente l'import auto
            String maybePath = importPathField != null ? importPathField.getText() : "";
            if (maybePath != null && !maybePath.isBlank()) {
                try {
                    String rel = EditorIO.importImage(Path.of(maybePath));
                    e.imageFile = rel;
                    status("Image imported and added: " + rel);
                } catch (Exception ex) {
                    status("Add image failed: " + ex.getMessage());
                }
            } else {
                status("Paste path then Import, then click canvas.");
            }

            scene.elements.add(e);
            scene.select(e.id);
            return true;
        }

        // SELECT tool
        Element hit = hitTest(scene, cx, cy);
        if (hit != null) {
            scene.select(hit.id);
            dragging = true;
            dragOffX = cx - hit.x;
            dragOffY = cy - hit.y;
            status("Selected: " + hit.type.name());
            return true;
        }

        scene.selectedId = null;
        status("Selection cleared.");
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseXD, double mouseYD, int button) {
        dragging = false;
        activeHandle = ResizeHandle.NONE;
        return super.mouseReleased(mouseXD, mouseYD, button);
    }

    @Override
    public boolean mouseDragged(double mouseXD, double mouseYD, int button, double deltaX, double deltaY) {
        int mouseX = (int) mouseXD;
        int mouseY = (int) mouseYD;

        Scene scene = EditorState.get().scene;
        Element sel = scene.getSelected();
        if (sel == null) return super.mouseDragged(mouseXD, mouseYD, button, deltaX, deltaY);
        if (!dragging) return super.mouseDragged(mouseXD, mouseYD, button, deltaX, deltaY);

        int cx = toCanvasX(mouseX);
        int cy = toCanvasY(mouseY);

        if (activeHandle != ResizeHandle.NONE) {
            resizeWithHandle(sel, activeHandle, cx, cy);
            return true;
        }

        sel.x = clamp(cx - dragOffX);
        sel.y = clamp(cy - dragOffY);
        return true;
    }

    private void resizeWithHandle(Element e, ResizeHandle h, int canvasMouseX, int canvasMouseY) {
        int minSize = 8;

        int x1 = e.x;
        int y1 = e.y;
        int x2 = e.x + e.w;
        int y2 = e.y + e.h;

        if (h == ResizeHandle.TL) {
            x1 = canvasMouseX;
            y1 = canvasMouseY;
        } else if (h == ResizeHandle.TR) {
            x2 = canvasMouseX;
            y1 = canvasMouseY;
        } else if (h == ResizeHandle.BL) {
            x1 = canvasMouseX;
            y2 = canvasMouseY;
        } else if (h == ResizeHandle.BR) {
            x2 = canvasMouseX;
            y2 = canvasMouseY;
        }

        int nx1 = Math.min(x1, x2);
        int nx2 = Math.max(x1, x2);
        int ny1 = Math.min(y1, y2);
        int ny2 = Math.max(y1, y2);

        int nw = Math.max(minSize, nx2 - nx1);
        int nh = Math.max(minSize, ny2 - ny1);

        e.x = clamp(nx1);
        e.y = clamp(ny1);
        e.w = nw;
        e.h = nh;
    }

    private ResizeHandle hitHandle(Element e, int mouseX, int mouseY) {
        Scene scene = EditorState.get().scene;
        int x = scene.canvasX + e.x;
        int y = scene.canvasY + e.y;

        if (hitPoint(mouseX, mouseY, x, y)) return ResizeHandle.TL;
        if (hitPoint(mouseX, mouseY, x + e.w, y)) return ResizeHandle.TR;
        if (hitPoint(mouseX, mouseY, x, y + e.h)) return ResizeHandle.BL;
        if (hitPoint(mouseX, mouseY, x + e.w, y + e.h)) return ResizeHandle.BR;

        return ResizeHandle.NONE;
    }

    private boolean hitPoint(int mx, int my, int px, int py) {
        int half = HANDLE_SIZE;
        return mx >= px - half && mx <= px + half && my >= py - half && my <= py + half;
    }

    private Element hitTest(Scene scene, int canvasX, int canvasY) {
        for (int i = scene.elements.size() - 1; i >= 0; i--) {
            Element e = scene.elements.get(i);
            if (canvasX >= e.x && canvasY >= e.y && canvasX < e.x + e.w && canvasY < e.y + e.h) {
                return e;
            }
        }
        return null;
    }

    private int clamp(int v) {
        return MathHelper.clamp(v, -9999, 9999);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // GLFW_KEY_DELETE = 261
        if (keyCode == 261) {
            deleteSelected();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}