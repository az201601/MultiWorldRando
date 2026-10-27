package com.multiworldrando.core;

/**
 * User-selected randomizer options. One checkbox per feature.
 */
public final class Settings {

    private boolean randomizeStartingGold;
    private boolean randomizeDrawerMoney;
    private boolean shuffleSeedShopSlots;

    public boolean isRandomizeStartingGold() {
        return randomizeStartingGold;
    }

    public void setRandomizeStartingGold(boolean randomizeStartingGold) {
        this.randomizeStartingGold = randomizeStartingGold;
    }

    public boolean isRandomizeDrawerMoney() {
        return randomizeDrawerMoney;
    }

    public void setRandomizeDrawerMoney(boolean randomizeDrawerMoney) {
        this.randomizeDrawerMoney = randomizeDrawerMoney;
    }

    public boolean isShuffleSeedShopSlots() {
        return shuffleSeedShopSlots;
    }

    public void setShuffleSeedShopSlots(boolean shuffleSeedShopSlots) {
        this.shuffleSeedShopSlots = shuffleSeedShopSlots;
    }
}
