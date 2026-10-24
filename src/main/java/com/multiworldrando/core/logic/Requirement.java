package com.multiworldrando.core.logic;

import java.util.Set;

@FunctionalInterface
public interface Requirement {
    boolean canAccess(Set<String> collectedItemIds);

    static Requirement none() {
        return items -> true;
    }

    static Requirement has(String itemId) {
        return items -> items.contains(itemId);
    }

    static Requirement hasAll(String... itemIds) {
        return items -> {
            for (String id : itemIds) {
                if (!items.contains(id)) return false;
            }
            return true;
        };
    }

    static Requirement hasAny(String... itemIds) {
        return items -> {
            for (String id : itemIds) {
                if (items.contains(id)) return true;
            }
            return false;
        };
    }
}
