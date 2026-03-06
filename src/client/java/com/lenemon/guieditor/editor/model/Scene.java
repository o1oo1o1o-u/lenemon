// src/client/java/com/lenemon/guieditor/editor/model/Scene.java
package com.lenemon.guieditor.editor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The type Scene.
 */
public class Scene {
    /**
     * The Canvas x.
     */
// canvas de travail (sera repositionné automatiquement dans le Screen)
    public int canvasX = 10;
    /**
     * The Canvas y.
     */
    public int canvasY = 110;
    /**
     * The Canvas w.
     */
    public int canvasW = 300;
    /**
     * The Canvas h.
     */
    public int canvasH = 200;

    /**
     * The Target x.
     */
// zone de référence "ton vrai GUI"
    public int targetX = 96;
    /**
     * The Target y.
     */
    public int targetY = 46;
    /**
     * The Target w.
     */
    public int targetW = 160 - 96;
    /**
     * The Target h.
     */
    public int targetH = 145 - 46;

    /**
     * The Elements.
     */
    public List<Element> elements = new ArrayList<>();
    /**
     * The Selected id.
     */
    public UUID selectedId = null;

    /**
     * Gets selected.
     *
     * @return the selected
     */
    public Element getSelected() {
        if (selectedId == null) return null;
        for (Element e : elements) {
            if (selectedId.equals(e.id)) return e;
        }
        return null;
    }

    /**
     * Select.
     *
     * @param id the id
     */
    public void select(UUID id) {
        selectedId = id;
    }
}