package com.saloed.bcity.game;

import com.saloed.bcity.io.Input;
import com.saloed.bcity.graphics.Sprite;
import com.saloed.bcity.graphics.SpriteSheet;
import com.saloed.bcity.graphics.TextureAtlas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.saloed.bcity.game.Level.TILE_SCALE;

public class Enemy extends Entity {

    public static final int SPRITE_SCALE = 16;
    public static final int SPRITES_PER_HEADING = 2;
    private static final int COOLDOWN_TIME = 60;
    private static final float ENEMY_SPEED = 1.5f;

    public enum Type {
        BASIC,
        FAST,
        POWER,
        ARMOR
    }

    public enum Heading {
        NORTH(8 * SPRITE_SCALE, 0),     // Tiles 9-10 (x=128)
        EAST(14 * SPRITE_SCALE, 0),     // Tiles 15-16 (x=224)
        SOUTH(12 * SPRITE_SCALE, 0),    // Tiles 13-14 (x=192)
        WEST(10 * SPRITE_SCALE, 0);     // Tiles 11-12 (x=160)

        private int x, y;

        Heading(int x, int y) {
            this.x = x;
            this.y = y;
        }

        protected Image texture(TextureAtlas atlas, Type type) {
            // Different enemy types are on different rows in the atlas
            int rowY = switch (type) {
                case BASIC -> 4 * SPRITE_SCALE;  // Row 5 -> y=64
                case FAST -> 5 * SPRITE_SCALE;   // Row 6 -> y=80
                case POWER -> 6 * SPRITE_SCALE;  // Row 7 -> y=96
                case ARMOR -> 7 * SPRITE_SCALE;  // Row 8 -> y=112
            };
            // Each direction has 2 frames (32px wide)
            return atlas.cut(x, rowY, 2 * SPRITE_SCALE, SPRITE_SCALE);
        }

        public Bullet.Direction toBulletDirection() {
            return switch (this) {
                case NORTH -> Bullet.Direction.UP;
                case EAST -> Bullet.Direction.RIGHT;
                case SOUTH -> Bullet.Direction.DOWN;
                case WEST -> Bullet.Direction.LEFT;
            };
        }

        public static Heading random(Random rand) {
            return values()[rand.nextInt(values().length)];
        }
    }

    private Heading heading;
    private Map<Heading, Sprite> spriteMap;
    private float scale;
    private float speed;
    private TextureAtlas atlas;
    private int cooldown;
    private boolean alive;
    private Type type;
    private Random random;
    private int moveTimer;
    private int animationFrame;
    private boolean spawning = false; // True until spawn animation finishes

    public Enemy(float x, float y, Type type, TextureAtlas atlas) {
        super(EntityType.Enemy, x, y);

        this.heading = Heading.SOUTH;
        this.spriteMap = new HashMap<>();
        this.scale = TILE_SCALE;
        this.speed = type == Type.FAST ? ENEMY_SPEED * 1.5f : ENEMY_SPEED;
        this.atlas = atlas;
        this.type = type;
        this.cooldown = 0;
        this.alive = false; // Start invisible until spawn animation finishes
        this.random = new Random();
        this.moveTimer = 0;
        this.animationFrame = 0;
        this.spawning = true; // Mark as spawning

        loadSprites();
    }

    private void loadSprites() {
        for (Heading h : Heading.values()) {
            SpriteSheet sheet = new SpriteSheet(h.texture(atlas, type), SPRITES_PER_HEADING, SPRITE_SCALE);
            Sprite sprite = new Sprite(sheet, scale);
            spriteMap.put(h, sprite);
        }
    }

    @Override
    public void update(Input input) {
        if (!alive) return;

        moveTimer++;
        if (moveTimer > 60 + random.nextInt(60)) {
            heading = Heading.random(random);
            moveTimer = 0;
        }

        float newX = x;
        float newY = y;

        switch (heading) {
            case NORTH -> newY -= speed;
            case EAST -> newX += speed;
            case SOUTH -> newY += speed;
            case WEST -> newX -= speed;
        }

        // Screen bounds
        if (newX < 0) {
            newX = 0;
            heading = Heading.random(random);
        } else if (newX > Game.WIDTH - SPRITE_SCALE * scale) {
            newX = Game.WIDTH - SPRITE_SCALE * scale;
            heading = Heading.random(random);
        }

        if (newY < 0) {
            newY = 0;
            heading = Heading.random(random);
        } else if (newY > Game.HEIGHT - SPRITE_SCALE * scale) {
            newY = Game.HEIGHT - SPRITE_SCALE * scale;
            heading = Heading.random(random);
        }

        x = newX;
        y = newY;

        if (cooldown > 0) cooldown--;

        // Animate tracks (only when moving)
        if (newX != x || newY != y) {
            animationFrame++;
            spriteMap.get(heading).getSheet().setSpriteIndex((animationFrame / 8) % SPRITES_PER_HEADING);
        }
    }

    public Bullet tryShoot() {
        if (cooldown > 0 || random.nextInt(100) > 2) return null;

        cooldown = COOLDOWN_TIME;

        float tankSize = SPRITE_SCALE * scale; // 64 pixels at 4x scale
        float tankCenterX = x + tankSize / 2;
        float tankCenterY = y + tankSize / 2;
        
        // Bullet sizes scaled
        float bulletX, bulletY;
        
        switch (heading) {
            case NORTH -> {
                bulletX = tankCenterX - 12; // centered horizontally
                bulletY = y - 16; // spawn at top edge
            }
            case SOUTH -> {
                bulletX = tankCenterX - 12; // centered horizontally
                bulletY = y + tankSize - 16; // spawn at bottom edge
            }
            case EAST -> {
                bulletX = x + tankSize - 16; // spawn at right edge
                bulletY = tankCenterY - 12; // centered vertically
            }
            case WEST -> {
                bulletX = x - 16; // spawn at left edge
                bulletY = tankCenterY - 12; // centered vertically
            }
            default -> {
                bulletX = tankCenterX - 12;
                bulletY = y - 16;
            }
        }

        return new Bullet(bulletX, bulletY, heading.toBulletDirection(), EntityType.Enemy, atlas);
    }

    @Override
    public void render(GraphicsContext g) {
        if (alive && !spawning) { // Don't render during spawn animation
            spriteMap.get(heading).render(g, x, y);
        }
    }

    public float getScale() {
        return scale;
    }

    public float getWidth() {
        return SPRITE_SCALE * scale;
    }

    public float getHeight() {
        return SPRITE_SCALE * scale;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }

    public Type getType() {
        return type;
    }

    public void finishSpawning() {
        this.spawning = false;
        this.alive = true; // Now tank is visible
    }

    public boolean isSpawning() {
        return spawning;
    }
}
