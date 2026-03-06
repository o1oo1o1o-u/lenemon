// src/client/java/com/lenemon/guieditor/editor/EditorState.java
package com.lenemon.guieditor.editor;

import com.lenemon.guieditor.editor.model.Scene;

/**
 * The type Editor state.
 */
public final class EditorState {
    private static final EditorState INSTANCE = new EditorState();

    /**
     * The Scene.
     */
    public Scene scene = new Scene();

    private EditorState() {}

    /**
     * Get editor state.
     *
     * @return the editor state
     */
    public static EditorState get() {
        return INSTANCE;
    }
}