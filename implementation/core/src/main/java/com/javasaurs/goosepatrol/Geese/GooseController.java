package com.javasaurs.goosepatrol.Geese;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.javasaurs.goosepatrol.Events.LongBoi;
import com.javasaurs.goosepatrol.ObstacleMap;

public class GooseController {
    // Variables for the number of geese and their frame dimensions

    private static final int GOOSE_COUNT = 12;
    private static final int EVENT_FLOCK_COUNT = 4;
    private static final int MAX_FRAME_SIZE = 36;
    private static final int DUCK_FRAME_WIDTH = 26;
    private static final int DUCK_FRAME_HEIGHT = 18;
    private static final int GOOSE_FRAME_WIDTH = 36;
    private static final int GOOSE_FRAME_HEIGHT = 24;
    private static final int SWAN_FRAME_WIDTH = 36;
    private static final int SWAN_FRAME_HEIGHT = 36;
    private static final int DUCK_FLIGHT_FRAME_START = 5;
    private static final int GOOSE_FLIGHT_FRAME_START = 5;
    private static final int SWAN_FLIGHT_FRAME_START = 6;
    private static final float TILE_SIZE = 32f;
    private static final float SEED_RANGE_TILES = 5f;
    private static final float SEED_ATTRACTION_DURATION = 5f;

    // Textures for different types of geese

    private final Texture duckTexture;
    private final Texture gooseTexture;
    private final Texture swanTexture;
    private final ObstacleMap obstacles;
    private final LongBoi longBoi;
    private final List<BaseGoose> geese = new ArrayList<>();
    private final Random random = new Random();

    /**
     * Creates the bird textures and spawns the requested number of geese across the map.
     */
    public GooseController(int mapWidth, int mapHeight, ObstacleMap obstacles, LongBoi longBoi) {
        this.obstacles = obstacles;
        this.longBoi = longBoi;
        duckTexture = loadTexture("Geese/duck_spritesheet.png");
        gooseTexture = loadTexture("Geese/goose_variants_spritesheet.png");
        swanTexture = loadTexture("Geese/swan_spritesheet.png");

        for (int i = 0; i < GOOSE_COUNT; i++) {
            addRandomGoose(mapWidth, mapHeight);
        }
    }

    /**
     * Loads a sprite sheet and sharpens it so the birds stay crisp at small sizes.
     */
    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    /**
     * Picks a bird type, chooses a random valid map position, and adds it to the world.
     */
    private void addRandomGoose(int mapWidth, int mapHeight) {
        GooseType type = GooseType.values()[random.nextInt(GooseType.values().length)];
        float x = 0f;
        float y = 0f;
        boolean foundPosition = false;
        for (int attempt = 0; attempt < 1000; attempt++) {
            x = random.nextFloat() * Math.max(0, mapWidth - MAX_FRAME_SIZE);
            y = random.nextFloat() * Math.max(0, mapHeight - MAX_FRAME_SIZE);
            if (!isBlockedForBird(x, y, MAX_FRAME_SIZE, MAX_FRAME_SIZE, x, y)) {
                foundPosition = true;
                break;
            }
        }
        if (!foundPosition) {
            throw new IllegalStateException("Could not find a walkable position for a goose");
        }
        addGoose(type, x, y);
    }

    /**
     * Adds a small flock around the player when the lake event is activated.
     */
    public void spawnFlock(float centerX, float centerY, int mapWidth, int mapHeight) {
        for (int i = 0; i < EVENT_FLOCK_COUNT; i++) {
            float x = 0f;
            float y = 0f;
            boolean foundPosition = false;
            for (int attempt = 0; attempt < 1000; attempt++) {
                float angle = random.nextFloat() * (float) (Math.PI * 2);
                float distance = (2f + random.nextFloat() * 3f) * 32f;
                x = centerX + (float) Math.cos(angle) * distance - MAX_FRAME_SIZE / 2f;
                y = centerY + (float) Math.sin(angle) * distance - MAX_FRAME_SIZE / 2f;
                if (x >= 0f && y >= 0f
                    && x <= mapWidth - MAX_FRAME_SIZE && y <= mapHeight - MAX_FRAME_SIZE
                    && !isBlockedForBird(x, y, MAX_FRAME_SIZE, MAX_FRAME_SIZE, x, y)) {
                    foundPosition = true;
                    break;
                }
            }
            if (!foundPosition) {
                throw new IllegalStateException("Could not find a walkable position for the lake flock");
            }
            addGoose(GooseType.GOOSE, x, y);
        }
    }

    /**
     * Selects the requested bird's sprite and creates an instance at the supplied position.
     */
    private void addGoose(GooseType type, float x, float y) {
        Texture texture;
        int frameWidth;
        int frameHeight;
        int flightFrameStart;

        switch (type) {
            case DUCK:
                texture = duckTexture;
                frameWidth = DUCK_FRAME_WIDTH;
                frameHeight = DUCK_FRAME_HEIGHT;
                flightFrameStart = DUCK_FLIGHT_FRAME_START;
                break;
            case GOOSE:
                texture = gooseTexture;
                frameWidth = GOOSE_FRAME_WIDTH;
                frameHeight = GOOSE_FRAME_HEIGHT;
                flightFrameStart = GOOSE_FLIGHT_FRAME_START;
                break;
            case SWAN:
                texture = swanTexture;
                frameWidth = SWAN_FRAME_WIDTH;
                frameHeight = SWAN_FRAME_HEIGHT;
                flightFrameStart = SWAN_FLIGHT_FRAME_START;
                break;
            default:
                throw new IllegalStateException("Unsupported goose type: " + type);
        }

        int variantCount = texture.getHeight() / frameHeight;
        int variantRow = random.nextInt(variantCount);
        int frameCount = texture.getWidth() / frameWidth;
        TextureRegion[] walkingFrames = new TextureRegion[flightFrameStart];
        TextureRegion[] attackFrames = new TextureRegion[frameCount - flightFrameStart];
        for (int frame = 0; frame < frameCount; frame++) {
            TextureRegion region = new TextureRegion(
                texture,
                frame * frameWidth,
                variantRow * frameHeight,
                frameWidth,
                frameHeight
            );
            if (frame < flightFrameStart) {
                walkingFrames[frame] = region;
            } else {
                attackFrames[frame - flightFrameStart] = region;
            }
        }

        switch (type) {
            case DUCK:
                geese.add(new Duck(
                    walkingFrames,
                    attackFrames,
                    x,
                    y,
                    frameWidth,
                    frameHeight,
                    obstacles,
                    longBoi
                ));
                break;
            case GOOSE:
                geese.add(new Goose(
                    walkingFrames,
                    attackFrames,
                    x,
                    y,
                    frameWidth,
                    frameHeight,
                    obstacles,
                    longBoi
                ));
                break;
            case SWAN:
                geese.add(new Swan(
                    walkingFrames,
                    attackFrames,
                    x,
                    y,
                    frameWidth,
                    frameHeight,
                    obstacles,
                    longBoi
                ));
                break;
            default:
                throw new IllegalStateException("Unsupported goose type: " + type);
        }
    }

    /**
     * Updates every active bird so they can move independently each frame.
     */
    public void update(
        float deltaTime,
        int mapWidth,
        int mapHeight,
        float playerX,
        float playerY,
        float playerWidth,
        float playerHeight
    ) {
        for (BaseGoose goose : geese) {
            goose.update(
                deltaTime,
                mapWidth,
                mapHeight,
                playerX,
                playerY,
                playerWidth,
                playerHeight
            );
        }
    }

    /**
     * Attracts nearby or player-following birds to the centre of the player's current tile.
     */
    public void useSeeds(float playerCenterX, float playerCenterY) {
        float targetX = (float) Math.floor(playerCenterX / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2f;
        float targetY = (float) Math.floor(playerCenterY / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2f;
        float range = SEED_RANGE_TILES * TILE_SIZE;
        for (BaseGoose goose : geese) {
            goose.attractToSeedsIfNearbyOrFollowing(
                targetX,
                targetY,
                range,
                SEED_ATTRACTION_DURATION
            );
        }
    }

    /**
     * Checks a proposed bird rectangle against map obstacles and statue protection.
     */
    private boolean isBlockedForBird(
        float x,
        float y,
        float width,
        float height,
        float currentX,
        float currentY
    ) {
        return obstacles.collides(x, y, width, height)
            || longBoi.collidesForBirds(x, y, width, height, currentX, currentY);
    }

    /**
     * Draws all birds in the scene in their current positions.
     */
    public void render(SpriteBatch batch) {
        for (BaseGoose goose : geese) {
            goose.render(batch);
        }
    }

    /**
     * Releases the texture memory used by the bird sprite sheets when the game closes.
     */
    public void dispose() {
        duckTexture.dispose();
        gooseTexture.dispose();
        swanTexture.dispose();
    }

    private enum GooseType {
        DUCK,
        GOOSE,
        SWAN
    }

}
