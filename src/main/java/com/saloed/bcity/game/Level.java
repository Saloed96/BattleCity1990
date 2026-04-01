package com.saloed.bcity.game;

import com.saloed.bcity.graphics.TextureAtlas;
import javafx.scene.canvas.GraphicsContext;

public class Level {

    public static final int TILES_X = 50;
    public static final int TILES_Y = 37;
    public static final int TILE_SCALE = 2;

    private Tile[][] tiles;
    private TextureAtlas atlas;
    private boolean eagleDestroyed;

    // Default level layout (1 = brick, 2 = steel, 3 = water, 4 = grass, 5 = ice, 6 = eagle)
    private static final int[][] DEFAULT_LEVEL = generateLevel();

    private static int[][] generateLevel() {
        int[][] level = new int[TILES_Y][TILES_X];
        
        // Fill with empty
        for (int y = 0; y < TILES_Y; y++) {
            for (int x = 0; x < TILES_X; x++) {
                level[y][x] = 0;
            }
        }
        
        // Add some brick walls patterns
        for (int i = 5; i < 45; i += 8) {
            for (int j = 5; j < 15; j += 4) {
                level[j][i] = 1;
                level[j][i+1] = 1;
            }
        }
        
        // Add steel walls
        for (int i = 10; i < 40; i += 20) {
            level[20][i] = 2;
            level[20][i+1] = 2;
        }
        
        // Add water
        for (int x = 15; x < 25; x++) {
            level[25][x] = 3;
        }
        
        // Add grass
        for (int x = 30; x < 40; x++) {
            for (int y = 10; y < 15; y++) {
                level[y][x] = 4;
            }
        }
        
        // Add ice patches (slippery surface)
        for (int x = 5; x < 15; x++) {
            for (int y = 20; y < 24; y++) {
                level[y][x] = 5;
            }
        }
        
        // More ice near spawn areas
        for (int x = 35; x < 45; x++) {
            for (int y = 5; y < 8; y++) {
                level[y][x] = 5;
            }
        }
        
        // Eagle base at bottom center (single 2x2 tile represented as 4 tiles)
        int eagleX = TILES_X / 2 - 1;
        int eagleY = TILES_Y - 4;
        level[eagleY][eagleX] = 6;
        level[eagleY][eagleX+1] = 6;
        level[eagleY+1][eagleX] = 6;
        level[eagleY+1][eagleX+1] = 6;
        
        // Protect eagle with bricks
        level[eagleY-1][eagleX-1] = 1;
        level[eagleY-1][eagleX] = 1;
        level[eagleY-1][eagleX+1] = 1;
        level[eagleY-1][eagleX+2] = 1;
        level[eagleY][eagleX-1] = 1;
        level[eagleY][eagleX+2] = 1;
        level[eagleY+1][eagleX-1] = 1;
        level[eagleY+1][eagleX+2] = 1;
        
        return level;
    }

    public Level(TextureAtlas atlas) {
        this.atlas = atlas;
        this.tiles = new Tile[TILES_X][TILES_Y];
        this.eagleDestroyed = false;
        loadLevel(DEFAULT_LEVEL);
    }

    private void loadLevel(int[][] layout) {
        for (int y = 0; y < TILES_Y; y++) {
            for (int x = 0; x < TILES_X; x++) {
                TileType type = TileType.fromId(layout[y][x]);
                float px = x * Tile.TILE_SIZE * TILE_SCALE;
                float py = y * Tile.TILE_SIZE * TILE_SCALE;
                tiles[x][y] = new Tile(type, px, py, atlas);
            }
        }
    }

    public void render(GraphicsContext g) {
        // Render non-grass tiles first
        for (int y = 0; y < TILES_Y; y++) {
            for (int x = 0; x < TILES_X; x++) {
                Tile tile = tiles[x][y];
                if (tile.getType() != TileType.GRASS) {
                    tile.render(g);
                }
            }
        }
    }

    public void renderGrass(GraphicsContext g) {
        // Render grass on top (semi-transparent effect)
        for (int y = 0; y < TILES_Y; y++) {
            for (int x = 0; x < TILES_X; x++) {
                Tile tile = tiles[x][y];
                if (tile.getType() == TileType.GRASS) {
                    tile.render(g);
                }
            }
        }
    }

    public Tile getTileAt(float x, float y) {
        int tx = (int) (x / (Tile.TILE_SIZE * TILE_SCALE));
        int ty = (int) (y / (Tile.TILE_SIZE * TILE_SCALE));
        if (tx >= 0 && tx < TILES_X && ty >= 0 && ty < TILES_Y) {
            return tiles[tx][ty];
        }
        return null;
    }

    public boolean checkCollision(float x, float y, float width, float height) {
        // Check all tiles that the rectangle overlaps
        int startX = (int) (x / (Tile.TILE_SIZE * TILE_SCALE));
        int startY = (int) (y / (Tile.TILE_SIZE * TILE_SCALE));
        int endX = (int) ((x + width) / (Tile.TILE_SIZE * TILE_SCALE));
        int endY = (int) ((y + height) / (Tile.TILE_SIZE * TILE_SCALE));

        for (int ty = startY; ty <= endY; ty++) {
            for (int tx = startX; tx <= endX; tx++) {
                if (tx >= 0 && tx < TILES_X && ty >= 0 && ty < TILES_Y) {
                    if (tiles[tx][ty].isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean damageTileAt(float x, float y) {
        Tile tile = getTileAt(x, y);
        if (tile != null && tile.getType().isDestructible()) {
            boolean wasEagle = tile.getType() == TileType.EAGLE;
            tile.destroy();
            if (wasEagle) {
                eagleDestroyed = true;
            }
            return true;
        }
        return false;
    }

    public boolean checkBulletCollision(float x, float y, float width, float height) {
        // Check all tiles that the rectangle overlaps (bullets pass through water and grass)
        int startX = (int) (x / (Tile.TILE_SIZE * TILE_SCALE));
        int startY = (int) (y / (Tile.TILE_SIZE * TILE_SCALE));
        int endX = (int) ((x + width) / (Tile.TILE_SIZE * TILE_SCALE));
        int endY = (int) ((y + height) / (Tile.TILE_SIZE * TILE_SCALE));

        for (int ty = startY; ty <= endY; ty++) {
            for (int tx = startX; tx <= endX; tx++) {
                if (tx >= 0 && tx < TILES_X && ty >= 0 && ty < TILES_Y) {
                    Tile tile = tiles[tx][ty];
                    // Bullets pass through water and grass
                    if (tile.isSolid() && tile.getType() != TileType.WATER && tile.getType() != TileType.GRASS) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isEagleDestroyed() {
        return eagleDestroyed;
    }

    public int getWidth() {
        return TILES_X * Tile.TILE_SIZE * TILE_SCALE;
    }

    public int getHeight() {
        return TILES_Y * Tile.TILE_SIZE * TILE_SCALE;
    }
}
