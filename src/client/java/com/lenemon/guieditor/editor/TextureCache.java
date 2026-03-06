package com.lenemon.guieditor.editor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Texture cache.
 */
public final class TextureCache {

    /**
     * The type Entry.
     */
    public static final class Entry {
        /**
         * The Id.
         */
        public final Identifier id;
        /**
         * The Width.
         */
        public final int width;
        /**
         * The Height.
         */
        public final int height;

        private Entry(Identifier id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }

    private static final Map<String, Entry> CACHE = new HashMap<>();

    private TextureCache() {}

    /**
     * Gets or load.
     *
     * @param relativeFileName the relative file name
     * @return the or load
     * @throws IOException the io exception
     */
    public static Entry getOrLoad(String relativeFileName) throws IOException {
        if (relativeFileName == null) return null;

        Entry cached = CACHE.get(relativeFileName);
        if (cached != null) return cached;

        Path p = EditorIO.imagesDir().resolve(relativeFileName);
        if (!Files.exists(p)) throw new IOException("Missing image: " + p);

        NativeImage img;
        try (InputStream in = Files.newInputStream(p)) {
            img = NativeImage.read(in);
        }

        int w = img.getWidth();
        int h = img.getHeight();

        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);

        String safeKey = relativeFileName.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
        Identifier id = Identifier.of("lenemon_gui_editor", "local/" + safeKey);

        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);

        Entry entry = new Entry(id, w, h);
        CACHE.put(relativeFileName, entry);
        return entry;
    }

    /**
     * Clear.
     */
    public static void clear() {
        CACHE.clear();
    }
}