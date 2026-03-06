package com.lenemon.shop;

import java.util.List;

/**
 * The type Shop registry.
 */
public class ShopRegistry {

    private static List<ShopCategory> categories;

    /**
     * Sets categories.
     *
     * @param list the list
     */
    public static void setCategories(List<ShopCategory> list) {
        categories = list;
    }

    /**
     * Gets categories.
     *
     * @return the categories
     */
    public static List<ShopCategory> getCategories() {
        return categories;
    }
}