package com.saloed.bcity.game;

import com.saloed.bcity.graphics.Sprite;
import com.saloed.bcity.graphics.SpriteSheet;
import com.saloed.bcity.graphics.TextureAtlas;
import javafx.scene.canvas.GraphicsContext;

public class Tile {

    public static final int TILE_SIZE = 16;

    private TileType type;
    private float x;
    private float y;
    private Sprite sprite;
    private boolean destroyed;
    private static int globalAnimationFrame = 0; // Shared across all tiles
    private static final int WATER_ANIMATION_SPEED = 6; // Update every 6 frames (~10 FPS at 60 FPS)

    // Tile atlas coordinates (based on texture_atlas.png layout)
    // Each tile is 16x16 pixels
    // Tile numbers are 1-based (as counted visually), converted to 0-based for pixel coords
    private static final int GRASS_X = 17 * 16;      // Tile 18, Row 3 -> x=272, y=32
    private static final int GRASS_Y = 2 * 16;
    private static final int BRICK_X = 16 * 16;      // Tile 17, Row 1 -> x=256, y=0
    private static final int BRICK_Y = 0;
    private static final int STEEL_X = 16 * 16;      // Tile 17, Row 2 -> x=256, y=16
    private static final int STEEL_Y = 16;
    private static final int WATER_X = 16 * 16;      // Tile 17, Row 3 (animated) -> x=256, y=32
    private static final int WATER_Y = 2 * 16;
    // Water animation frames are all on the same row (row 3)
    // Frame 1: Tile 17 (x=256), Frame 2: Tile 17 next row (x=256, y=48), Frame 3: Tile 18 next row (x=272, y=48)
    private static final int ICE_X = 18 * 16;        // Tile 19, Row 3 -> x=288, y=32
    private static final int ICE_Y = 2 * 16;
    private static final int EAGLE_X = 19 * 16;      // Tile 20, Row 3 -> x=304, y=32
    private static final int EAGLE_Y = 2 * 16;
    private static final int EAGLE_DEAD_X = 20 * 16; // Tile 21, Row 3 -> x=320, y=32
    private static final int EAGLE_DEAD_Y = 2 * 16;

    public Tile(TileType type, float x, float y, TextureAtlas atlas) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.destroyed = false;
        this.sprite = createSprite(type, atlas);
    }

    private Sprite createSprite(TileType type, TextureAtlas atlas) {
        if (type == TileType.EMPTY) return null;

        int sx = 0, sy = 0;
        switch (type) {
            case BRICK -> { sx = BRICK_X; sy = BRICK_Y; }
            case STEEL -> { sx = STEEL_X; sy = STEEL_Y; }
            case WATER -> { sx = WATER_X; sy = WATER_Y; }
            case GRASS -> { sx = GRASS_X; sy = GRASS_Y; }
            case ICE -> { sx = ICE_X; sy = ICE_Y; }
            case EAGLE -> { sx = EAGLE_X; sy = EAGLE_Y; }
            case EAGLE_DEAD -> { sx = EAGLE_DEAD_X; sy = EAGLE_DEAD_Y; }
        }

        // Water has 3 animation frames arranged in L-shape: (17,3), (17,4), (18,4)
        // We need to create a custom sprite sheet by combining these frames
        if (type == TileType.WATER) {
            // Create a combined image: 48x16 (3 frames * 16px each, single row)
            var frame1 = atlas.cut(16 * 16, 2 * 16, TILE_SIZE, TILE_SIZE);      // Tile 17, Row 3
            var frame2 = atlas.cut(16 * 16, 3 * 16, TILE_SIZE, TILE_SIZE);      // Tile 17, Row 4
            var frame3 = atlas.cut(17 * 16, 3 * 16, TILE_SIZE, TILE_SIZE);      // Tile 18, Row 4
            
            // Combine frames horizontally into one image
            javafx.scene.image.WritableImage combined = new javafx.scene.image.WritableImage(
                TILE_SIZE * 3, TILE_SIZE);
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(TILE_SIZE * 3, TILE_SIZE);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            
            gc.drawImage(frame1, 0, 0);
            gc.drawImage(frame2, TILE_SIZE, 0);
            gc.drawImage(frame3, TILE_SIZE * 2, 0);
            
            combined = canvas.snapshot(null, null);
            var sheet = new SpriteSheet(combined, 3, TILE_SIZE);
            return new Sprite(sheet, 4.0f);
        } else {
            var sheet = new SpriteSheet(atlas.cut(sx, sy, TILE_SIZE, TILE_SIZE), 1, TILE_SIZE);
            return new Sprite(sheet, 4.0f); // Scale 4x
        }
    }

    public void render(GraphicsContext g) {
        if (sprite != null && !destroyed) {
            // Update animation frame for water (slower speed)
            if (type == TileType.WATER) {
                sprite.getSheet().setSpriteIndex((globalAnimationFrame / WATER_ANIMATION_SPEED) % 3);
            }
            sprite.render(g, x, y);
        }
    }

    public static void updateAnimation() {
        globalAnimationFrame++;
    }

    public void destroy() {
        if (type.isDestructible()) {
            destroyed = true;
            if (type == TileType.EAGLE) {
                type = TileType.EAGLE_DEAD;
            } else {
                type = TileType.EMPTY;
            }
        }
    }

    public boolean isSolid() {
        return type.isSolid() && !destroyed;
    }

    public TileType getType() {
        return type;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return TILE_SIZE * 4;
    }

    public float getHeight() {
        return TILE_SIZE * 4;
    }
}
