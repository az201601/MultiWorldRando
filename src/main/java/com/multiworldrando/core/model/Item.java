package com.multiworldrando.core.model;

import java.util.Objects;

public class Item {
    private final String id;
    private final String name;
    private final String gameId;
    private final ItemClassification classification;
    private final int internalId; // Game-specific internal ID/code

    public Item(String id, String name, String gameId, ItemClassification classification, int internalId) {
        this.id = id;
        this.name = name;
        this.gameId = gameId;
        this.classification = classification;
        this.internalId = internalId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getGameId() { return gameId; }
    public ItemClassification getClassification() { return classification; }
    public int getInternalId() { return internalId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id) && Objects.equals(gameId, item.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gameId);
    }

    @Override
    public String toString() {
        return "[" + gameId + "] " + name + " (" + classification + ")";
    }
}
