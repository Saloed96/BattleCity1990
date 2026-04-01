package com.saloed.bcity.game;

import com.saloed.bcity.graphics.TextureAtlas;
import javafx.scene.canvas.GraphicsContext;

public class Level {

    public static final int TILES_X = 16;
    public static final int TILES_Y = 15;
    public static final float TILE_SCALE = 1.0f;

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
        
        // Border walls (brick)
        for (int x = 0; x < TILES_X; x++) {
            level[0][x] = 1;  // Top border
            level[TILES_Y - 1][x] = 1;  // Bottom border
        }
        for (int y = 0; y < TILES_Y; y++) {
            level[y][0] = 1;  // Left border
            level[y][TILES_X - 1] = 1;  // Right border
        }
        
        // Add brick wall patterns
        for (int i = 2; i < 14; i += 4) {
            for (int j = 2; j < 6; j += 2) {
                level[j][i] = 1;
                level[j][i+1] = 1;
            }
        }
        
        // Add steel walls
        level[8][7] = 2;
        level[8][8] = 2;
        
        // Add water
        for (int x = 11; x < 15; x++) {
            level[10][x] = 3;
        }
        
        // Add grass
        for (int x = 11; x < 15; x++) {
            for (int y = 3; y < 6; y++) {
                level[y][x] = 4;
            }
        }
        
        // Add ice patches
        for (int x = 2; x < 5; x++) {
            for (int y = 10; y < 12; y++) {
                level[y][x] = 5;
            }
        }
        
        // Eagle base at bottom center (2x2 tiles)
        int eagleX = TILES_X / 2 - 1;
        int eagleY = TILES_Y - 3;
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
        this.tiles = new Tile[TILES_Y][TILES_X];
        this.eagleDestroyed = false;
        loadLevel(DEFAULT_LEVEL);
    }

    private void loadLevel(int[][] layout) {
        for (int y = 0; y < TILES_Y; y++) {
            for (int x = 0; x < TILES_X; x++) {
                TileType type = TileType.fromId(layout[y][x]);
                float px = x * Tile.TILE_SIZE * TILE_SCALE;
                float py = y * Tile.TILE_SIZE * TILE_SCALE;
                tiles[y][x] = new Tile(type, px, py, atlas);
            }
        }
    }

    public void render(GraphicsContext g) {
        // Render non-grass tiles first
        for (int y = 0; y < TILES_Y; y++) {
            for (int x = 0; x < TILES_X; x++) {
                Tile tile = tiles[y][x];
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
                Tile tile = tiles[y][x];
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
            return tiles[ty][tx];
        }
        return null;
    }

    public boolean checkCollision(float x, float y, float width, float height) {
        // Check all tiles that the rectangle overlaps
        int startX = (int) (x / (Tile.TILE_SIZE * TILE_SCALE));
        int startY = (int) (y / (Tile.TILE_SIZE * TILE_SCALE));
        int endX = (int) ((x + width - 0.01f) / (Tile.TILE_SIZE * TILE_SCALE));
        int endY = (int) ((y + height - 0.01f) / (Tile.TILE_SIZE * TILE_SCALE));

        for (int ty = startY; ty <= endY; ty++) {
            for (int tx = startX; tx <= endX; tx++) {
                if (tx >= 0 && tx < TILES_X && ty >= 0 && ty < TILES_Y) {
                    if (tiles[ty][tx].isSolid()) {
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
        int endX = (int) ((x + width - 0.01f) / (Tile.TILE_SIZE * TILE_SCALE));
        int endY = (int) ((y + height - 0.01f) / (Tile.TILE_SIZE * TILE_SCALE));

        for (int ty = startY; ty <= endY; ty++) {
            for (int tx = startX; tx <= endX; tx++) {
                if (tx >= 0 && tx < TILES_X && ty >= 0 && ty < TILES_Y) {
                    Tile tile = tiles[ty][tx];
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

    public float getWidth() {
        return TILES_X * Tile.TILE_SIZE * TILE_SCALE;
    }

    public float getHeight() {
        return TILES_Y * Tile.TILE_SIZE * TILE_SCALE;
    }
}
