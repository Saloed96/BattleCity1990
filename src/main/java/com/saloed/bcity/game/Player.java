package com.saloed.bcity.game;

import com.saloed.bcity.io.Input;
import com.saloed.bcity.graphics.Sprite;
import com.saloed.bcity.graphics.SpriteSheet;
import com.saloed.bcity.graphics.TextureAtlas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;

import java.util.HashMap;
import java.util.Map;

public class Player extends Entity {

    public static final int SPRITE_SCALE = 16;
    public static final int SPRITES_PER_HEADING = 2; // 2 animation frames per direction
    private static final int COOLDOWN_TIME = 15; // Frames between shots

    public enum Heading {
        NORTH(0 * SPRITE_SCALE, 0 * SPRITE_SCALE, 2 * SPRITE_SCALE, 1 * SPRITE_SCALE),   // Tiles 1,2 Row 1
        EAST(6 * SPRITE_SCALE, 0 * SPRITE_SCALE, 2 * SPRITE_SCALE, 1 * SPRITE_SCALE),    // Tiles 7,8 Row 1
        SOUTH(4 * SPRITE_SCALE, 0 * SPRITE_SCALE, 2 * SPRITE_SCALE, 1 * SPRITE_SCALE),   // Tiles 5,6 Row 1
        WEST(2 * SPRITE_SCALE, 0 * SPRITE_SCALE, 2 * SPRITE_SCALE, 1 * SPRITE_SCALE);    // Tiles 3,4 Row 1

        private int x, y, h, w;

        Heading(int x, int y, int h, int w) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        protected Image texture(TextureAtlas atlas) {
            return atlas.cut(x, y, w, h);
        }

        public Bullet.Direction toBulletDirection() {
            return switch (this) {
                case NORTH -> Bullet.Direction.UP;
                case EAST -> Bullet.Direction.RIGHT;
                case SOUTH -> Bullet.Direction.DOWN;
                case WEST -> Bullet.Direction.LEFT;
            };
        }
    }

    private Heading heading;
    private Map<Heading, Sprite> spriteMap;
    private float scale;
    private float speed;
    private TextureAtlas atlas;
    private int cooldown;
    private boolean alive;
    private int animationFrame = 0; // Animation frame counter
    private boolean invulnerable = false; // Invulnerable during spawn
    private boolean spawning = false; // True until spawn animation finishes

    public Player(float x, float y, float scale, float speed, TextureAtlas atlas) {
        super(EntityType.Player, x, y);

        this.heading = Heading.NORTH;
        this.spriteMap = new HashMap<>();
        this.scale = scale;
        this.speed = speed;
        this.atlas = atlas;
        this.cooldown = 0;
        this.alive = true;

        for (Heading h : Heading.values()) {
            SpriteSheet sheet = new SpriteSheet(h.texture(atlas), SPRITES_PER_HEADING, SPRITE_SCALE);
            Sprite sprite = new Sprite(sheet, scale);
            spriteMap.put(h, sprite);
        }
    }

    @Override
    public void update(Input input) {
        if (!alive) return;

        float newX = x;
        float newY = y;

        if (input.getKey(KeyCode.UP)) {
            newY -= speed;
            heading = Heading.NORTH;
        } else if (input.getKey(KeyCode.RIGHT)) {
            newX += speed;
            heading = Heading.EAST;
        } else if (input.getKey(KeyCode.DOWN)) {
            newY += speed;
            heading = Heading.SOUTH;
        } else if (input.getKey(KeyCode.LEFT)) {
            newX -= speed;
            heading = Heading.WEST;
        }

        // Screen bounds
        if (newX < 0) newX = 0;
        else if (newX > Game.WIDTH - SPRITE_SCALE * scale) newX = Game.WIDTH - SPRITE_SCALE * scale;

        if (newY < 0) newY = 0;
        else if (newY > Game.HEIGHT - SPRITE_SCALE * scale) newY = Game.HEIGHT - SPRITE_SCALE * scale;

        x = newX;
        y = newY;

        if (cooldown > 0) cooldown--;

        // Update animation frame (only when moving)
        if (newX != x || newY != y) {
            animationFrame++;
            spriteMap.get(heading).getSheet().setSpriteIndex((animationFrame / 8) % SPRITES_PER_HEADING);
        }
    }

    public Bullet tryShoot() {
        if (cooldown > 0) return null;

        cooldown = COOLDOWN_TIME;

        float tankSize = SPRITE_SCALE * scale; // 64 pixels at 4x scale
        float tankCenterX = x + tankSize / 2;
        float tankCenterY = y + tankSize / 2;
        
        // Bullet sizes scaled to 24x32 at 4x
        float bulletX, bulletY;
        
        switch (heading) {
            case NORTH -> {
                bulletX = tankCenterX - 12; // centered horizontally
                bulletY = y - 16; // spawn just in front of tank
            }
            case SOUTH -> {
                bulletX = tankCenterX - 12; // centered horizontally
                bulletY = y + tankSize; // spawn below tank
            }
            case EAST -> {
                bulletX = x + tankSize; // spawn to the right of tank
                bulletY = tankCenterY - 12; // centered vertically
            }
            case WEST -> {
                bulletX = x - 16; // spawn just to the left of tank
                bulletY = tankCenterY - 12; // centered vertically
            }
            default -> {
                bulletX = tankCenterX - 12;
                bulletY = y - 16;
            }
        }

        return new Bullet(bulletX, bulletY, heading.toBulletDirection(), EntityType.Player, atlas);
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

    public Heading getHeading() {
        return heading;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
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

    public void respawn(float x, float y) {
        this.x = x;
        this.y = y;
        this.alive = false; // Keep invisible until spawn animation finishes
        this.heading = Heading.NORTH;
        this.invulnerable = true; // Start invulnerable during spawn animation
        this.spawning = true; // Mark as spawning
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void finishSpawning() {
        this.spawning = false;
        this.alive = true; // Now tank is visible and can be hit
    }

    public boolean isSpawning() {
        return spawning;
    }
}
