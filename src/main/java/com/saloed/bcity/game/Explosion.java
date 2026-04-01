package com.saloed.bcity.game;

import com.saloed.bcity.io.Input;
import com.saloed.bcity.graphics.Sprite;
import com.saloed.bcity.graphics.SpriteSheet;
import com.saloed.bcity.graphics.TextureAtlas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Explosion extends Entity {

    public enum Type {
        SMALL,   // Bullet hit - 3 frames (16x16)
        BIG,     // Tank destroyed - complex animation with 32x32 frames
        SPAWN    // Tank spawn - 13 frames sequence (16x16)
    }

    // Small explosion: Tiles 17,18,19 Row 9 (16x16 each)
    private static final int SMALL_X = 16 * 16;  // Tile 17 -> x=256
    private static final int SMALL_Y = 8 * 16;   // Row 9 -> y=128
    
    // Big explosion: starts with small anim, then two 32x32 tiles, ends with last small frame
    // Tile 20 Row 9 (32x32), Tile 22 Row 9 (32x32)
    private static final int BIG_FRAME_1_X = 19 * 16;  // Tile 20 -> x=304 (32px wide)
    private static final int BIG_FRAME_2_X = 21 * 16;  // Tile 22 -> x=336 (32px wide)
    private static final int BIG_Y = 8 * 16;           // Row 9 -> y=128
    
    // Spawn explosion: Tiles 17-20 Row 7, sequence: 4,3,2,1,2,3,4,3,2,1,2,3,4
    private static final int SPAWN_X = 16 * 16;  // Tile 17 -> x=256
    private static final int SPAWN_Y = 6 * 16;   // Row 7 -> y=96

    private static final int SMALL_FRAMES = 3;
    private static final int BIG_FRAMES = 6;     // 3 small + 2 big + 1 final small
    private static final int SPAWN_FRAMES = 13;  // Custom sequence

    private Image[] frames;  // Array of animation frames
    private int currentFrame;
    private int maxFrames;
    private int frameDelay;
    private int frameCounter;
    private boolean active;
    private Type type;
    private Entity spawningEntity; // Reference to entity that is spawning
    private boolean spawnCallbackTriggered = false; // Ensure callback only fires once

    public Explosion(float x, float y, Type type, TextureAtlas atlas) {
        super(EntityType.Bullet, x, y);
        this.type = type;
        this.active = true;
        this.currentFrame = 0;
        this.frameCounter = 0;
        this.frames = new Image[getMaxFramesForType(type)];
        this.spawningEntity = null;

        switch (type) {
            case SMALL -> createSmallExplosion(atlas);
            case BIG -> createBigExplosion(atlas);
            case SPAWN -> createSpawnExplosion(atlas);
        }
    }

    public Explosion(float x, float y, Type type, TextureAtlas atlas, Entity spawningEntity) {
        super(EntityType.Bullet, x, y);
        this.type = type;
        this.active = true;
        this.currentFrame = 0;
        this.frameCounter = 0;
        this.frames = new Image[getMaxFramesForType(type)];
        this.spawningEntity = spawningEntity;

        switch (type) {
            case SMALL -> createSmallExplosion(atlas);
            case BIG -> createBigExplosion(atlas);
            case SPAWN -> createSpawnExplosion(atlas);
        }
    }

    private int getMaxFramesForType(Type type) {
        return switch (type) {
            case SMALL -> SMALL_FRAMES;
            case BIG -> BIG_FRAMES;
            case SPAWN -> SPAWN_FRAMES;
        };
    }

    private void createSmallExplosion(TextureAtlas atlas) {
        // Extract 3 frames: Tiles 17,18,19 Row 9
        for (int i = 0; i < 3; i++) {
            frames[i] = atlas.cut(SMALL_X + i * 16, SMALL_Y, 16, 16);
        }
        this.maxFrames = SMALL_FRAMES;
        this.frameDelay = 3;
    }

    private void createBigExplosion(TextureAtlas atlas) {
        // First 3 frames: same as small explosion (Tiles 17,18,19 Row 9)
        for (int i = 0; i < 3; i++) {
            frames[i] = atlas.cut(SMALL_X + i * 16, SMALL_Y, 16, 16);
        }
        // Frame 4: Tile 20 Row 9 (32x32)
        frames[3] = atlas.cut(BIG_FRAME_1_X, BIG_Y, 32, 32);
        // Frame 5: Tile 22 Row 9 (32x32)
        frames[4] = atlas.cut(BIG_FRAME_2_X, BIG_Y, 32, 32);
        // Frame 6: Last frame from small animation (Tile 19 Row 9)
        frames[5] = atlas.cut(SMALL_X + 2 * 16, SMALL_Y, 16, 16);
        
        this.maxFrames = BIG_FRAMES;
        this.frameDelay = 4;
    }

    private void createSpawnExplosion(TextureAtlas atlas) {
        // Sequence: 4,3,2,1,2,3,4,3,2,1,2,3,4
        // Where 1=Tile17, 2=Tile18, 3=Tile19, 4=Tile20
        int[] sequence = {4,3,2,1,2,3,4,3,2,1,2,3,4};
        for (int i = 0; i < sequence.length; i++) {
            int tileIndex = sequence[i] - 1; // Convert to 0-based (0=Tile17, 1=Tile18, etc.)
            frames[i] = atlas.cut(SPAWN_X + tileIndex * 16, SPAWN_Y, 16, 16);
        }
        this.maxFrames = SPAWN_FRAMES;
        this.frameDelay = 6;
    }

    @Override
    public void update(Input input) {
        if (!active) return;

        frameCounter++;
        if (frameCounter >= frameDelay) {
            frameCounter = 0;
            currentFrame++;
            
            // Check if we just reached or passed the last frame
            if (type == Type.SPAWN && spawningEntity != null && !spawnCallbackTriggered && currentFrame >= maxFrames - 1) {
                spawnCallbackTriggered = true;
                // Make the entity visible immediately AFTER showing the last frame
                if (spawningEntity instanceof Player playerRef) {
                    playerRef.setInvulnerable(false);
                    playerRef.finishSpawning();
                } else if (spawningEntity instanceof Enemy enemyRef) {
                    enemyRef.finishSpawning();
                }
            }
            
            if (currentFrame >= maxFrames) {
                active = false;
            }
        }
    }

    @Override
    public void render(GraphicsContext g) {
        if (active && currentFrame < frames.length && frames[currentFrame] != null) {
            Image frameImage = frames[currentFrame];
            // Scale 4x and center at (x, y)
            double w = frameImage.getWidth() * 4;
            double h = frameImage.getHeight() * 4;
            g.drawImage(frameImage, x - w / 2, y - h / 2, w, h);
        }
    }

    public boolean isActive() {
        return active;
    }

    public Type getType() {
        return type;
    }

    public boolean isSpawnFinished() {
        // Return true when we've displayed the last frame and callback was triggered
        return spawnCallbackTriggered;
    }

    public Entity getSpawningEntity() {
        return spawningEntity;
    }
}
