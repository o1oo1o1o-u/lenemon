// src/client/java/com/lenemon/guieditor/editor/EditorIO.java
package com.lenemon.guieditor.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lenemon.guieditor.GuiEditorClient;
import com.lenemon.guieditor.editor.model.Scene;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.*;

/**
 * The type Editor io.
 */
public final class EditorIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path baseDir;
    private static Path imagesDir;
    private static Path scenesDir;

    private EditorIO() {}

    /**
     * Init folders.
     */
    public static void initFolders() {
        baseDir = FabricLoader.getInstance().getConfigDir().resolve(GuiEditorClient.MOD_ID);
        imagesDir = baseDir.resolve("images");
        scenesDir = baseDir.resolve("scenes");
        try {
            Files.createDirectories(imagesDir);
            Files.createDirectories(scenesDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create GUI editor folders: " + e.getMessage(), e);
        }
    }

    /**
     * Images dir path.
     *
     * @return the path
     */
    public static Path imagesDir() {
        return imagesDir;
    }

    /**
     * Scene path string.
     *
     * @param name the name
     * @return the string
     */
    public static String scenePath(String name) {
        return scenesDir.resolve(safeName(name) + ".json").toString();
    }

    /**
     * Save scene.
     *
     * @param name  the name
     * @param scene the scene
     * @throws IOException the io exception
     */
    public static void saveScene(String name, Scene scene) throws IOException {
        Path p = scenesDir.resolve(safeName(name) + ".json");
        try (Writer w = Files.newBufferedWriter(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(scene, w);
        }
    }

    /**
     * Load scene scene.
     *
     * @param name the name
     * @return the scene
     * @throws IOException the io exception
     */
    public static Scene loadScene(String name) throws IOException {
        Path p = scenesDir.resolve(safeName(name) + ".json");
        try (Reader r = Files.newBufferedReader(p)) {
            Scene s = GSON.fromJson(r, Scene.class);
            if (s == null) throw new IOException("Scene is empty");
            return s;
        }
    }

    /**
     * Import image string.
     *
     * @param sourceFile the source file
     * @return the string
     * @throws IOException the io exception
     */
    public static String importImage(Path sourceFile) throws IOException {
        if (!Files.exists(sourceFile)) throw new IOException("File not found: " + sourceFile);
        String fileName = sourceFile.getFileName().toString();
        String lower = fileName.toLowerCase();
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
            throw new IOException("Only PNG/JPG supported");
        }

        String clean = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = imagesDir.resolve(clean);

        int i = 1;
        while (Files.exists(target)) {
            String base = clean;
            String ext = "";
            int dot = clean.lastIndexOf('.');
            if (dot >= 0) {
                base = clean.substring(0, dot);
                ext = clean.substring(dot);
            }
            target = imagesDir.resolve(base + "_" + i + ext);
            i++;
        }

        Files.copy(sourceFile, target, StandardCopyOption.COPY_ATTRIBUTES);
        return target.getFileName().toString();
    }

    private static String safeName(String s) {
        if (s == null || s.isBlank()) return "scene";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}