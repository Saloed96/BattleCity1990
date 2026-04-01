package com.saloed.bcity.game;

public enum TileType {
    EMPTY(0, false, false),
    BRICK(1, true, true),
    STEEL(2, true, false),
    WATER(3, true, false),
    GRASS(4, false, false),
    ICE(5, false, false),
    EAGLE(6, true, true),
    EAGLE_DEAD(7, false, false);

    private final int id;
    private final boolean solid;
    private final boolean destructible;

    TileType(int id, boolean solid, boolean destructible) {
        this.id = id;
        this.solid = solid;
        this.destructible = destructible;
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isDestructible() {
        return destructible;
    }

    public static TileType fromId(int id) {
        for (TileType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return EMPTY;
    }
}
