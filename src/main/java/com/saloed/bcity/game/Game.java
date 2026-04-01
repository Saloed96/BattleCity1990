package com.saloed.bcity.game;

import com.saloed.bcity.io.Input;
import com.saloed.bcity.display.Display;
import com.saloed.bcity.graphics.TextureAtlas;
import com.saloed.bcity.utils.Time;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Game extends AnimationTimer {

    public static final int WIDTH = 1600;  // 50 tiles * 32 pixels
    public static final int HEIGHT = 1184; // 37 tiles * 32 pixels
    public static final String TITLE = "Battle City";
    public static final int CLEAR_COLOR = 0xff000000;
    public static final int NUM_BUFFERS = 3;

    public static final float UPDATE_RATE = 60.0f;
    public static final float UPDATE_INTERVAL = Time.SECOND / UPDATE_RATE;

    public static final String ATLAS_FILE_NAME = "texture_atlas.png";

    private boolean running;
    private GraphicsContext graphics;
    private Input input;
    private TextureAtlas atlas;
    private Level level;
    private Player player;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private List<Explosion> explosions;
    private Random random;

    private int fps = 0;
    private int upd = 0;
    private long lastFpsTime = 0;
    private float delta = 0;
    private long lastTime = 0;
    private int enemySpawnTimer = 0;
    private int maxEnemies = 4;
    private int playerLives = 3;
    private int playerRespawnTimer = 0;
    private static final int PLAYER_RESPAWN_DELAY = 120; // 2 seconds at 60fps

    public Game(Stage stage) {
        running = false;
        Display.create(stage, WIDTH, HEIGHT, TITLE, CLEAR_COLOR, NUM_BUFFERS);
        graphics = Display.getGraphics();
        input = Display.getInput();
        atlas = new TextureAtlas(ATLAS_FILE_NAME);

        level = new Level(atlas);
        player = new Player(800, 1100, 2, 3, atlas);
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        explosions = new ArrayList<>();
        random = new Random();
    }

    public void startGame() {
        if (running)
            return;

        running = true;
        lastTime = Time.get();
        lastFpsTime = lastTime;
        Display.show();
        start();
    }

    public void stopGame() {
        if (!running)
            return;

        running = false;
        stop();
        cleanUp();
    }

    private void update() {
        // Update tile animations (water, etc.)
        Tile.updateAnimation();

        // Player shooting
        if (input.getKey(KeyCode.SPACE)) {
            Bullet bullet = player.tryShoot();
            if (bullet != null) {
                bullets.add(bullet);
            }
        }

        // Update player with collision
        float oldX = player.x;
        float oldY = player.y;
        player.update(input);

        // Check player collision with level
        if (level.checkCollision(player.x, player.y, player.getWidth(), player.getHeight())) {
            player.x = oldX;
            player.y = oldY;
        }

        // Check player collision with enemies
        if (player.isAlive()) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && player.intersects(enemy)) {
                    player.x = oldX;
                    player.y = oldY;
                    break;
                }
            }
        }

        // Spawn enemies
        enemySpawnTimer++;
        if (enemies.size() < maxEnemies && enemySpawnTimer > 180) {
            spawnEnemy();
            enemySpawnTimer = 0;
        }

        // Update enemies
        for (Enemy enemy : enemies) {
            float eOldX = enemy.x;
            float eOldY = enemy.y;
            enemy.update(input);

            // Check enemy collision with level
            if (level.checkCollision(enemy.x, enemy.y, enemy.getWidth(), enemy.getHeight())) {
                enemy.x = eOldX;
                enemy.y = eOldY;
            }

            // Check enemy collision with player
            if (enemy.isAlive() && player.isAlive() && enemy.intersects(player)) {
                enemy.x = eOldX;
                enemy.y = eOldY;
            }

            // Check enemy collision with other enemies
            for (Enemy other : enemies) {
                if (other != enemy && other.isAlive() && enemy.intersects(other)) {
                    enemy.x = eOldX;
                    enemy.y = eOldY;
                    break;
                }
            }

            // Enemy shooting
            Bullet bullet = enemy.tryShoot();
            if (bullet != null) {
                bullets.add(bullet);
            }
        }

        // Update bullets
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            bullet.update(input);

            if (!bullet.isActive()) {
                bulletIter.remove();
                continue;
            }

            // Check bullet collision with other bullets (enemy bullet hits player bullet)
            if (bullet.getOwner() == EntityType.Enemy) {
                for (Bullet otherBullet : bullets) {
                    if (otherBullet != bullet && otherBullet.getOwner() == EntityType.Player && 
                        bullet.intersects(otherBullet)) {
                        // Both bullets destroy each other
                        explosions.add(new Explosion(bullet.x, bullet.y, Explosion.Type.SMALL, atlas));
                        bullet.deactivate();
                        otherBullet.deactivate();
                        bulletIter.remove();
                        break;
                    }
                }
                // Skip further checks if bullet was destroyed by another bullet
                if (!bullet.isActive()) continue;
            }

            // Check bullet collision with level (bullets pass through water and grass)
            if (level.checkBulletCollision(bullet.x, bullet.y, bullet.getWidth(), bullet.getHeight())) {
                level.damageTileAt(bullet.x + bullet.getWidth()/2, bullet.y + bullet.getHeight()/2);
                explosions.add(new Explosion(bullet.x, bullet.y, Explosion.Type.SMALL, atlas));
                bullet.deactivate();
                bulletIter.remove();
                continue;
            }

            // Check bullet collision with entities
            if (bullet.getOwner() == EntityType.Player) {
                // Player bullet hits enemies
                for (Enemy enemy : enemies) {
                    if (enemy.isAlive() && bullet.intersects(enemy)) {
                        enemy.kill();
                        bullet.deactivate();
                        explosions.add(new Explosion(enemy.x, enemy.y, Explosion.Type.BIG, atlas));
                        bulletIter.remove();
                        break;
                    }
                }
            } else {
                // Enemy bullet hits player (check if not invulnerable)
                if (player.isAlive() && !player.isInvulnerable() && bullet.intersects(player)) {
                    player.kill();
                    bullet.deactivate();
                    explosions.add(new Explosion(player.x, player.y, Explosion.Type.BIG, atlas));
                    bulletIter.remove();
                }
            }
        }

        // Update explosions
        explosions.removeIf(explosion -> {
            explosion.update(input);
            return !explosion.isActive();
        });

        // Remove dead enemies (but not spawning ones)
        enemies.removeIf(enemy -> !enemy.isAlive() && !enemy.isSpawning());

        // Handle player respawn
        if (!player.isAlive() && playerLives > 0) {
            playerRespawnTimer++;
            if (playerRespawnTimer >= PLAYER_RESPAWN_DELAY) {
                playerLives--;
                playerRespawnTimer = 0;
                if (playerLives > 0) {
                    player.respawn(800, 1100);
                    explosions.add(new Explosion(800, 1100, Explosion.Type.SPAWN, atlas, player));
                }
            }
        }

        // Check game over conditions
        if (level.isEagleDestroyed() || (playerLives <= 0 && !player.isAlive())) {
            // Game over - could add state management here
        }
    }

    private void spawnEnemy() {
        // Spawn positions (top of screen)
        int[] spawnX = {32, 384, 736};
        int x = spawnX[random.nextInt(spawnX.length)];

        Enemy.Type type = Enemy.Type.values()[random.nextInt(Enemy.Type.values().length)];
        Enemy enemy = new Enemy(x, 32, type, atlas);
        enemies.add(enemy);
        explosions.add(new Explosion(x, 32, Explosion.Type.SPAWN, atlas, enemy));
    }

    private void render() {
        Display.clear();

        // Render level
        level.render(graphics);

        // Render entities
        for (Enemy enemy : enemies) {
            enemy.render(graphics);
        }

        player.render(graphics);

        // Render bullets
        for (Bullet bullet : bullets) {
            bullet.render(graphics);
        }

        // Render explosions
        for (Explosion explosion : explosions) {
            explosion.render(graphics);
        }

        // Render grass on top
        level.renderGrass(graphics);
    }

    @Override
    public void handle(long now) {
        if (!running)
            return;

        long elapsedTime = now - lastTime;
        lastTime = now;

        delta += (elapsedTime / UPDATE_INTERVAL);
        while (delta > 1) {
            update();
            upd++;
            delta--;
        }

        render();
        fps++;

        // Update FPS counter every second
        if (now - lastFpsTime >= Time.SECOND) {
            Display.setTitle(TITLE + " || Fps: " + fps + " | Lives: " + (playerLives + (player.isAlive() ? 1 : 0)) + " | Enemies: " + enemies.size());
            upd = 0;
            fps = 0;
            lastFpsTime = now;
        }
    }

    private void cleanUp() {
        Display.destroy();
    }

}
