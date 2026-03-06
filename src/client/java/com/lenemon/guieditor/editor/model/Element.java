// src/client/java/com/lenemon/guieditor/editor/model/Element.java
package com.lenemon.guieditor.editor.model;

import java.util.UUID;

/**
 * The type Element.
 */
public class Element {
    /**
     * The Id.
     */
    public UUID id = UUID.randomUUID();
    /**
     * The Type.
     */
    public ElementType type = ElementType.RECT;

    /**
     * The X.
     */
// coords relatives au canvas
    public int x;
    /**
     * The Y.
     */
    public int y;
    /**
     * The W.
     */
    public int w;
    /**
     * The H.
     */
    public int h;

    /**
     * The Argb.
     */
// RECT
    public int argb = 0xFF00FF00;

    /**
     * The Image file.
     */
// IMAGE
    public String imageFile = null; // fichier dans config/lenemon_gui_editor/images/
}