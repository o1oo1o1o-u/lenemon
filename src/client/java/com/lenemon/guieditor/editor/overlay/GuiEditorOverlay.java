package com.lenemon.guieditor.overlay;

import com.lenemon.guieditor.editor.EditorIO;
import com.lenemon.guieditor.editor.EditorState;
import com.lenemon.guieditor.editor.TextureCache;
import com.lenemon.guieditor.editor.model.Element;
import com.lenemon.guieditor.editor.model.ElementType;
import com.lenemon.guieditor.editor.model.Scene;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

/**
 * The type Gui editor overlay.
 */
public final class GuiEditorOverlay {

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
        ADD_IMAGE }
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
        BR }

    private static boolean enabled = false;

    private static Tool tool = Tool.SELECT;
    private static String importPath = "";
    private static String status = "Disabled";

    private static boolean prevLeftDown = false;
    private static boolean dragging = false;
    private static int dragOffX, dragOffY;
    private static ResizeHandle activeHandle = ResizeHandle.NONE;

    private static final int HANDLE_SIZE = 6;

    private GuiEditorOverlay() {}

    /**
     * Init.
     */
    public static void init() {
        // Render overlay
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            if (!enabled) return;
            renderHud(ctx);
        });

        // Input/update
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled) return;
            tickInput(client);
        });
    }

    /**
     * Toggle.
     */
    public static void toggle() {
        enabled = !enabled;
        status = enabled ? "Enabled" : "Disabled";
        dragging = false;
        activeHandle = ResizeHandle.NONE;
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        Scene scene = EditorState.get().scene;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();

        // UI top bar buttons (simple)
        int x = 200, y = 10;
        drawButton(ctx, x, y, 80, 20, "Select", tool == Tool.SELECT); x += 90;
        drawButton(ctx, x, y, 80, 20, "AddRect", tool == Tool.ADD_RECT); x += 90;
        drawButton(ctx, x, y, 80, 20, "AddImg", tool == Tool.ADD_IMAGE);

        x = 10; y += 26;
        drawButton(ctx, x, y, 80, 20, "Delete", false); x += 90;
        drawButton(ctx, x, y, 80, 20, "Import", false);

        // Status + path (display only, editing the path can be done by paste keybind later)
        ctx.drawTextWithShadow(client.textRenderer, "Tool: " + tool + " | " + status, 10, y + 26, 0xFFFFFF);
        ctx.drawTextWithShadow(client.textRenderer, "Path: " + importPath, 10, y + 38, 0xFFFFFF);

        // Canvas: full screen work area under toolbar
        scene.canvasX = 10;
        scene.canvasY = y + 60;
        scene.canvasW = w - 20;
        scene.canvasH = h - scene.canvasY - 10;

        int cx = scene.canvasX, cy = scene.canvasY, cw = scene.canvasW, ch = scene.canvasH;

        // Border only (no overlay)
        int border = 0x80FFFFFF;
        ctx.fill(cx - 1, cy - 1, cx + cw + 1, cy, border);
        ctx.fill(cx - 1, cy + ch, cx + cw + 1, cy + ch + 1, border);
        ctx.fill(cx - 1, cy - 1, cx, cy + ch + 1, border);
        ctx.fill(cx + cw, cy - 1, cx + cw + 1, cy + ch + 1, border);

        // Draw elements
        for (Element e : scene.elements) {
            drawElement(ctx, e);
        }

        // Selection overlay
        Element sel = scene.getSelected();
        if (sel != null) drawSelection(ctx, sel);
    }

    private static void tickInput(MinecraftClient client) {
        double mxD = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
        double myD = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
        int mx = (int)mxD;
        int my = (int)myD;

        boolean leftDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        boolean leftPressed = leftDown && !prevLeftDown;
        boolean leftReleased = !leftDown && prevLeftDown;
        prevLeftDown = leftDown;

        // Paste quick: Ctrl+V into importPath (simple)
        // (Optionnel: tu peux enlever si tu veux)
        // Paste quick: Ctrl+V
        if (InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)
                && InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_V)) {

            String clip = client.keyboard.getClipboard();
            if (clip != null && !clip.isBlank()) {
                importPath = clip.trim();
                while (importPath.endsWith("_")) {
                    importPath = importPath.substring(0, importPath.length() - 1).trim();
                }
                status = "Pasted path";
            }
        }

        // Click buttons
        if (leftPressed) {
            int bx = 10, by = 10;

            if (hit(mx, my, bx, by, 80, 20)) { tool = Tool.SELECT; status = "Tool SELECT"; return; }
            bx += 90;
            if (hit(mx, my, bx, by, 80, 20)) { tool = Tool.ADD_RECT; status = "Tool ADD_RECT"; return; }
            bx += 90;
            if (hit(mx, my, bx, by, 80, 20)) { tool = Tool.ADD_IMAGE; status = "Tool ADD_IMAGE"; return; }

            bx = 10; by += 26;
            if (hit(mx, my, bx, by, 80, 20)) { deleteSelected(); return; }
            bx += 90;
            if (hit(mx, my, bx, by, 80, 20)) { doImport(); return; }
        }

        Scene scene = EditorState.get().scene;

        // Canvas interaction
        if (!isInsideCanvas(scene, mx, my)) {
            if (leftReleased) { dragging = false; activeHandle = ResizeHandle.NONE; }
            return;
        }

        int cx = mx - scene.canvasX;
        int cy = my - scene.canvasY;

        if (leftPressed) {
            Element sel = scene.getSelected();
            if (sel != null) {
                ResizeHandle h = hitHandle(scene, sel, mx, my);
                if (h != ResizeHandle.NONE) {
                    activeHandle = h;
                    dragging = true;
                    return;
                }
            }

            if (tool == Tool.ADD_RECT) {
                Element e = new Element();
                e.type = ElementType.RECT;
                e.argb = 0xFF00FF00;
                e.x = clamp(cx - 20);
                e.y = clamp(cy - 12);
                e.w = 40;
                e.h = 24;
                scene.elements.add(e);
                scene.select(e.id);
                status = "Rect added";
                return;
            }

            if (tool == Tool.ADD_IMAGE) {
                Element e = new Element();
                e.type = ElementType.IMAGE;
                e.x = clamp(cx - 24);
                e.y = clamp(cy - 24);
                e.w = 48;
                e.h = 48;

                // If last import succeeded, we store it in status like "Imported: file.png"
                // Here, simplest: if importPath points to a file in config images, user should click Import first.
                // We'll try auto import if it's a real local path.
                try {
                    if (importPath != null && !importPath.isBlank()) {
                        String rel = EditorIO.importImage(Path.of(importPath));
                        e.imageFile = rel;
                        status = "Image added: " + rel;
                    } else {
                        status = "Paste path then click Import";
                    }
                } catch (Exception ex) {
                    status = "Add image failed: " + ex.getMessage();
                }

                scene.elements.add(e);
                scene.select(e.id);
                return;
            }

            // SELECT
            Element hit = hitTest(scene, cx, cy);
            if (hit != null) {
                scene.select(hit.id);
                dragging = true;
                dragOffX = cx - hit.x;
                dragOffY = cy - hit.y;
                status = "Selected";
                return;
            } else {
                scene.selectedId = null;
                status = "Selection cleared";
                return;
            }
        }

        if (leftDown && dragging) {
            Element sel = scene.getSelected();
            if (sel == null) return;

            if (activeHandle != ResizeHandle.NONE) {
                resizeWithHandle(sel, activeHandle, cx, cy);
            } else {
                sel.x = clamp(cx - dragOffX);
                sel.y = clamp(cy - dragOffY);
            }
        }

        if (leftReleased) {
            dragging = false;
            activeHandle = ResizeHandle.NONE;
        }
    }

    private static void doImport() {
        if (importPath == null || importPath.isBlank()) {
            status = "Import failed: empty path";
            return;
        }
        try {
            String rel = EditorIO.importImage(Path.of(importPath));
            status = "Imported: " + rel;
        } catch (Exception e) {
            status = "Import failed: " + e.getMessage();
        }
    }

    private static void deleteSelected() {
        Scene scene = EditorState.get().scene;
        Element sel = scene.getSelected();
        if (sel == null) { status = "Nothing selected"; return; }
        scene.elements.removeIf(e -> e.id.equals(sel.id));
        scene.selectedId = null;
        status = "Deleted";
    }

    private static void drawElement(DrawContext ctx, Element e) {
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
                return;
            }
            try {
                var entry = TextureCache.getOrLoad(e.imageFile);
                ctx.drawTexture(entry.id, x, y, 0, 0, e.w, e.h, entry.width, entry.height);
            } catch (Exception ex) {
                ctx.fill(x, y, x + e.w, y + e.h, 0x55FF0000);
            }
        }
    }

    private static void drawSelection(DrawContext ctx, Element e) {
        Scene scene = EditorState.get().scene;
        int x = scene.canvasX + e.x;
        int y = scene.canvasY + e.y;

        int c = 0xFFFFFFFF;
        ctx.fill(x - 1, y - 1, x + e.w + 1, y, c);
        ctx.fill(x - 1, y + e.h, x + e.w + 1, y + e.h + 1, c);
        ctx.fill(x - 1, y - 1, x, y + e.h + 1, c);
        ctx.fill(x + e.w, y - 1, x + e.w + 1, y + e.h + 1, c);

        drawHandle(ctx, x, y);
        drawHandle(ctx, x + e.w, y);
        drawHandle(ctx, x, y + e.h);
        drawHandle(ctx, x + e.w, y + e.h);
    }

    private static void drawHandle(DrawContext ctx, int x, int y) {
        int half = HANDLE_SIZE / 2;
        ctx.fill(x - half, y - half, x + half, y + half, 0xFF00FFFF);
    }

    private static boolean isInsideCanvas(Scene scene, int mx, int my) {
        return mx >= scene.canvasX && my >= scene.canvasY && mx < scene.canvasX + scene.canvasW && my < scene.canvasY + scene.canvasH;
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static void drawButton(DrawContext ctx, int x, int y, int w, int h, String label, boolean active) {
        int bg = active ? 0xFF444444 : 0xFF222222;
        int bd = 0xFFFFFFFF;
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + w, y + 1, bd);
        ctx.fill(x, y + h - 1, x + w, y + h, bd);
        ctx.fill(x, y, x + 1, y + h, bd);
        ctx.fill(x + w - 1, y, x + w, y + h, bd);
        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, x + 6, y + 6, 0xFFFFFF);
    }

    private static Element hitTest(Scene scene, int cx, int cy) {
        for (int i = scene.elements.size() - 1; i >= 0; i--) {
            Element e = scene.elements.get(i);
            if (cx >= e.x && cy >= e.y && cx < e.x + e.w && cy < e.y + e.h) return e;
        }
        return null;
    }

    private static ResizeHandle hitHandle(Scene scene, Element e, int mx, int my) {
        int x = scene.canvasX + e.x;
        int y = scene.canvasY + e.y;
        if (hitPoint(mx, my, x, y)) return ResizeHandle.TL;
        if (hitPoint(mx, my, x + e.w, y)) return ResizeHandle.TR;
        if (hitPoint(mx, my, x, y + e.h)) return ResizeHandle.BL;
        if (hitPoint(mx, my, x + e.w, y + e.h)) return ResizeHandle.BR;
        return ResizeHandle.NONE;
    }

    private static boolean hitPoint(int mx, int my, int px, int py) {
        int half = HANDLE_SIZE;
        return mx >= px - half && mx <= px + half && my >= py - half && my <= py + half;
    }

    private static void resizeWithHandle(Element e, ResizeHandle h, int cx, int cy) {
        int minSize = 8;

        int x1 = e.x;
        int y1 = e.y;
        int x2 = e.x + e.w;
        int y2 = e.y + e.h;

        if (h == ResizeHandle.TL) { x1 = cx; y1 = cy; }
        if (h == ResizeHandle.TR) { x2 = cx; y1 = cy; }
        if (h == ResizeHandle.BL) { x1 = cx; y2 = cy; }
        if (h == ResizeHandle.BR) { x2 = cx; y2 = cy; }

        int nx1 = Math.min(x1, x2);
        int nx2 = Math.max(x1, x2);
        int ny1 = Math.min(y1, y2);
        int ny2 = Math.max(y1, y2);

        e.x = clamp(nx1);
        e.y = clamp(ny1);
        e.w = Math.max(minSize, nx2 - nx1);
        e.h = Math.max(minSize, ny2 - ny1);
    }

    private static int clamp(int v) {
        return MathHelper.clamp(v, -9999, 9999);
    }
}