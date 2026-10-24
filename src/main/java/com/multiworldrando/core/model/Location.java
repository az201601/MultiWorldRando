package com.multiworldrando.core.model;

import com.multiworldrando.core.logic.Requirement;
import java.util.Objects;

public class Location {
    private final String id;
    private final String name;
    private final String gameId;
    private final Requirement accessRule;
    private final Item defaultItem;

    public Location(String id, String name, String gameId, Requirement accessRule, Item defaultItem) {
        this.id = id;
        this.name = name;
        this.gameId = gameId;
        this.accessRule = accessRule != null ? accessRule : Requirement.none();
        this.defaultItem = defaultItem;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getGameId() { return gameId; }
    public Requirement getAccessRule() { return accessRule; }
    public Item getDefaultItem() { return defaultItem; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(id, location.id) && Objects.equals(gameId, location.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gameId);
    }

    @Override
    public String toString() {
        return "[" + gameId + "] " + name;
    }
}
