package com.multiworldrando.core;

/**
 * User-selected randomizer options. One checkbox per feature.
 */
public final class Settings {

    private boolean randomizeStartingGold;

    public boolean isRandomizeStartingGold() {
        return randomizeStartingGold;
    }

    public void setRandomizeStartingGold(boolean randomizeStartingGold) {
        this.randomizeStartingGold = randomizeStartingGold;
    }
}
