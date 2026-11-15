package com.javasaurs.goosepatrol.Player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.javasaurs.goosepatrol.ObstacleMap;

public class PlayerController {
    // Constants for player movement speed, animation frame duration, and dimensions

    private static final float SPEED = 120f;
    private static final float ENERGY_DRINK_DURATION = 5f;
    private static final float ENERGY_DRINK_SPEED_MULTIPLIER = 1.5f;
    private static final float FRAME_DURATION = 0.12f;
    private static final float WIDTH = 32f;
    private static final float HEIGHT = 48f;
    private static final float FOOT_WIDTH = 16f;
    private static final float FOOT_HEIGHT = 8f;
    private static final int FRAME_WIDTH = 96;
    private static final int FRAME_HEIGHT = 192;

    // Texture and animation frames for the player character

    private final ObstacleMap obstacles;
    private final Texture texture;
    private final TextureRegion[][] frames = new TextureRegion[4][4];
    private float x;
    private float y;
    private float animationTime;
    private float energyDrinkTimeRemaining;
    private int direction;
    private int walkFrame;

    /**
     * Creates the player sprite sheet, extracts each walking frame, and positions the character at the provided start point.
     */
    public PlayerController(float x, float y, ObstacleMap obstacles) {
        this.x = x - WIDTH / 2f;
        this.y = y - HEIGHT / 2f;
        this.obstacles = obstacles;
        texture = new Texture(Gdx.files.internal("Player/Student Character Pixel Sprite Sheet.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        for (int row = 0; row < frames.length; row++) {
            int sourceY = 800 - row * 256;
            for (int column = 0; column < frames[row].length; column++) {
                int sourceX = 464 + column * 192;
                frames[row][column] = new TextureRegion(texture, sourceX, sourceY, FRAME_WIDTH, FRAME_HEIGHT);
            }
        }
    }

    /**
     * Reads the keyboard input, moves the player while respecting obstacles, and cycles the walk animation.
     */
    public void update(float deltaTime, int mapWidth, int mapHeight) {
        float moveX = 0f;
        float moveY = 0f;
        float movementSpeed = energyDrinkTimeRemaining > 0f
            ? SPEED * ENERGY_DRINK_SPEED_MULTIPLIER
            : SPEED;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            moveX -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            moveX += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveY -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveY += 1f;
        }

        boolean moving = moveX != 0f || moveY != 0f;
        if (moving) {
            float movementLength = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= movementLength;
            moveY /= movementLength;

            float distanceX = moveX * movementSpeed * deltaTime;
            float distanceY = moveY * movementSpeed * deltaTime;
            int steps = Math.max(
                1,
                (int) Math.ceil(Math.max(Math.abs(distanceX), Math.abs(distanceY)) / 2f)
            );
            float stepX = distanceX / steps;
            float stepY = distanceY / steps;

            // Move in smaller sub-steps so the player can slide cleanly around obstacles without clipping through walls.
            for (int step = 0; step < steps; step++) {
                float nextX = x + stepX;
                if (!collidesAt(nextX, y)) {
                    x = nextX;
                }
                float nextY = y + stepY;
                if (!collidesAt(x, nextY)) {
                    y = nextY;
                }
            }

            if (Math.abs(moveX) > Math.abs(moveY)) {
                direction = moveX < 0f ? 2 : 1;
            } else {
                direction = moveY < 0f ? 3 : 0;
            }

            animationTime += deltaTime;
            if (animationTime >= FRAME_DURATION) {
                animationTime %= FRAME_DURATION;
                walkFrame = walkFrame % 3 + 1;
            }
        } else {
            walkFrame = 0;
            animationTime = 0f;
        }

        x = Math.max(0f, Math.min(x, mapWidth - WIDTH));
        y = Math.max(0f, Math.min(y, mapHeight - HEIGHT));
        energyDrinkTimeRemaining = Math.max(0f, energyDrinkTimeRemaining - deltaTime);
    }

    /**
     * Activates the energy drink's temporary movement-speed boost.
     */
    public void useEnergyDrink() {
        energyDrinkTimeRemaining = ENERGY_DRINK_DURATION;
    }

    /**
     * Checks the player's foot-sized collision area at a proposed sprite position.
     */
    private boolean collidesAt(float spriteX, float spriteY) {
        return obstacles.collides(
            spriteX + (WIDTH - FOOT_WIDTH) / 2f,
            spriteY,
            FOOT_WIDTH,
            FOOT_HEIGHT
        );
    }

    /**
     * Draws the player's current animation frame at their position in the world.
     */
    public void render(SpriteBatch batch) {
        batch.draw(frames[direction][walkFrame], x, y, WIDTH, HEIGHT);
    }

    /**
     * Returns the player's centre point on the X axis for camera tracking.
     */
    public float getCenterX() {
        return x + WIDTH / 2f;
    }

    /**
     * Returns the player's centre point on the Y axis for camera tracking.
     */
    public float getCenterY() {
        return y + HEIGHT / 2f;
    }

    /**
     * Returns the player's left edge for bird contact checks.
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the player's bottom edge for bird contact checks.
     */
    public float getY() {
        return y;
    }

    /**
     * Returns the rendered player width for bird contact checks.
     */
    public float getWidth() {
        return WIDTH;
    }

    /**
     * Returns the rendered player height for bird contact checks.
     */
    public float getHeight() {
        return HEIGHT;
    }

    /**
     * Releases the texture used for the player sprites when the game is closed.
     */
    public void dispose() {
        texture.dispose();
    }
}
