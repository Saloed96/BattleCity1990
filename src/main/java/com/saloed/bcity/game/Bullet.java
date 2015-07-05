package com.saloed.bcity.game;

import com.saloed.bcity.io.Input;
import com.saloed.bcity.graphics.Sprite;
import com.saloed.bcity.graphics.SpriteSheet;
import com.saloed.bcity.graphics.TextureAtlas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Bullet extends Entity {

    public static final int BULLET_SIZE = 8;
    public static final float BULLET_SPEED = 12.0f; // Faster at 4x scale

    // Bullet sizes vary by direction (larger for 4x scale)
    private static final int BULLET_W_UP = 6;
    private static final int BULLET_H_UP = 8;
    private static final int BULLET_W_LEFT = 8;
    private static final int BULLET_H_LEFT = 6;
    private static final int BULLET_W_DOWN = 6;
    private static final int BULLET_H_DOWN = 8;
    private static final int BULLET_W_RIGHT = 8;
    private static final int BULLET_H_RIGHT = 6;

    public enum Direction {
        UP(0, -1),
        RIGHT(1, 0),
        DOWN(0, 1),
        LEFT(-1, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    private Direction direction;
    private Image bulletImage; // Direct image reference instead of Sprite
    private boolean active;
    private EntityType owner;
    private float width;
    private float height;
    private float scale;

    // Bullet sprites in bottom half of tiles 21 and 22, row 7
    // Tile 21: x=320, Tile 22: x=336, Row 7: y=96
    private static final int BULLET_UP_X = 20 * 16 + 3;        // Tile 21 + 3px
    private static final int BULLET_UP_Y = 6 * 16 + 6;         // Row 7 + 6px
    private static final int BULLET_LEFT_X = 20 * 16 + 10;     // Tile 21 + 10px
    private static final int BULLET_LEFT_Y = 6 * 16 + 6;       // Row 7 + 6px
    private static final int BULLET_DOWN_X = 21 * 16 + 3;      // Tile 22 + 3px
    private static final int BULLET_DOWN_Y = 6 * 16 + 6;       // Row 7 + 6px
    private static final int BULLET_RIGHT_X = 21 * 16 + 10;    // Tile 22 + 10px
    private static final int BULLET_RIGHT_Y = 6 * 16 + 6;      // Row 7 + 6px

    public Bullet(float x, float y, Direction direction, EntityType owner, TextureAtlas atlas) {
        super(EntityType.Bullet, x, y);
        this.direction = direction;
        this.owner = owner;
        this.active = true;
        this.scale = 4.0f;

        // Get the correct bullet sprite based on direction (with proper size)
        int sx, sy, sw, sh;
        switch (direction) {
            case UP -> { 
                sx = BULLET_UP_X; sy = BULLET_UP_Y; 
                sw = BULLET_W_UP; sh = BULLET_H_UP;
            }
            case LEFT -> { 
                sx = BULLET_LEFT_X; sy = BULLET_LEFT_Y;
                sw = BULLET_W_LEFT; sh = BULLET_H_LEFT;
            }
            case DOWN -> { 
                sx = BULLET_DOWN_X; sy = BULLET_DOWN_Y;
                sw = BULLET_W_DOWN; sh = BULLET_H_DOWN;
            }
            case RIGHT -> { 
                sx = BULLET_RIGHT_X; sy = BULLET_RIGHT_Y;
                sw = BULLET_W_RIGHT; sh = BULLET_H_RIGHT;
            }
            default -> { 
                sx = BULLET_UP_X; sy = BULLET_UP_Y;
                sw = BULLET_W_UP; sh = BULLET_H_UP;
            }
        }

        this.bulletImage = atlas.cut(sx, sy, sw, sh);
        
        // Store actual rendered size (sprite size * scale)
        this.width = sw * scale;
        this.height = sh * scale;
    }

    @Override
    public void update(Input input) {
        if (!active) return;

        x += direction.dx * BULLET_SPEED;
        y += direction.dy * BULLET_SPEED;

        // Check bounds
        if (x < 0 || x > Game.WIDTH || y < 0 || y > Game.HEIGHT) {
            active = false;
        }
    }

    @Override
    public void render(GraphicsContext g) {
        if (active && bulletImage != null) {
            g.drawImage(bulletImage, x, y, width, height);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public EntityType getOwner() {
        return owner;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getScale() {
        return scale;
    }

    public boolean intersects(Entity other) {
        if (!active) return false;

        float otherX = other.x;
        float otherY = other.y;
        float otherW;
        float otherH;

        if (other instanceof Player) {
            otherW = Player.SPRITE_SCALE * ((Player) other).getScale();
            otherH = Player.SPRITE_SCALE * ((Player) other).getScale();
        } else if (other instanceof Enemy) {
            otherW = Enemy.SPRITE_SCALE * ((Enemy) other).getScale();
            otherH = Enemy.SPRITE_SCALE * ((Enemy) other).getScale();
        } else if (other instanceof Bullet) {
            otherW = ((Bullet) other).getWidth();
            otherH = ((Bullet) other).getHeight();
        } else {
            otherW = 64; // Default at 4x scale
            otherH = 64;
        }

        return x < otherX + otherW &&
               x + width > otherX &&
               y < otherY + otherH &&
               y + height > otherY;
    }
}
