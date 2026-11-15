package com.javasaurs.goosepatrol.Geese;

import java.util.Random;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.javasaurs.goosepatrol.Events.LongBoi;
import com.javasaurs.goosepatrol.ObstacleMap;

public abstract class BaseGoose {
    private static final float FRAME_DURATION = 0.12f;
    private static final float ATTACK_PAUSE_DURATION = 1f;
    private static final float PATH_REFRESH_DURATION = 0.4f;
    private static final int PLAYER_SIGHT_RANGE_TILES = 3;

    private final TextureRegion[] walkingFrames;
    private final TextureRegion[] attackFrames;
    private final float width;
    private final float height;
    private final ObstacleMap obstacles;
    private final LongBoi longBoi;
    private final Random random = new Random();
    private float x;
    private float y;
    private float direction;
    private float timeUntilTurn;
    private float animationTime;
    private float attackTimeRemaining;
    private float pathRefreshTime;
    private float seedAttractionTimeRemaining;
    private float seedTargetX;
    private float seedTargetY;
    private int currentFrame;
    private boolean followingPlayer;
    private boolean canAttack = true;
    private Vector2 pathWaypoint;

    /**
     * Stores the bird's animation frames and movement data, then chooses its initial heading.
     */
    protected BaseGoose(
        TextureRegion[] walkingFrames,
        TextureRegion[] attackFrames,
        float x,
        float y,
        float width,
        float height,
        ObstacleMap obstacles,
        LongBoi longBoi
    ) {
        this.walkingFrames = walkingFrames;
        this.attackFrames = attackFrames;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.obstacles = obstacles;
        this.longBoi = longBoi;
        chooseDirection();
    }

    /**
     * Returns the speed for this bird type so each kind can move at a different rate.
     */
    protected abstract float getMovementSpeed();

    /**
     * Acquires the player within sight range, follows a refreshed obstacle-aware route, and handles contact attacks.
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
        boolean attractedBySeeds = seedAttractionTimeRemaining > 0f;
        if (attractedBySeeds) {
            seedAttractionTimeRemaining = Math.max(0f, seedAttractionTimeRemaining - deltaTime);
        }

        float playerCenterX = playerX + playerWidth / 2f;
        float playerCenterY = playerY + playerHeight / 2f;
        boolean touchingPlayer = overlaps(
            x,
            y,
            width,
            height,
            playerX,
            playerY,
            playerWidth,
            playerHeight
        );

        if (!touchingPlayer) {
            canAttack = true;
        }
        if (touchingPlayer && canAttack && attackTimeRemaining <= 0f) {
            attackTimeRemaining = ATTACK_PAUSE_DURATION;
            animationTime = 0f;
            currentFrame = 0;
        }
        if (attackTimeRemaining > 0f) {
            updateAttackAnimation(deltaTime);
            attackTimeRemaining = Math.max(0f, attackTimeRemaining - deltaTime);
            if (attackTimeRemaining <= 0f) {
                canAttack = false;
            }
            return;
        }

        if (!followingPlayer && !attractedBySeeds
            && obstacles.hasLineOfSightWithinTiles(
                x + width / 2f,
                y + height / 2f,
                playerCenterX,
                playerCenterY,
                PLAYER_SIGHT_RANGE_TILES
            )) {
            followingPlayer = true;
            pathRefreshTime = 0f;
        }

        float targetX = x + width / 2f + (float) Math.cos(direction) * getMovementSpeed();
        float targetY = y + height / 2f + (float) Math.sin(direction) * getMovementSpeed();
        if (followingPlayer || attractedBySeeds) {
            pathRefreshTime -= deltaTime;
            if (pathRefreshTime <= 0f) {
                float destinationX = attractedBySeeds ? seedTargetX : playerCenterX;
                float destinationY = attractedBySeeds ? seedTargetY : playerCenterY;
                float targetLeft = destinationX - width / 2f;
                float targetBottom = destinationY - height / 2f;
                boolean canWalkStraightToPlayer = obstacles.hasClearLineOfSight(
                    x + width / 2f,
                    y + height / 2f,
                    destinationX,
                    destinationY
                ) && !isBlockedAt(targetLeft, targetBottom);
                pathWaypoint = canWalkStraightToPlayer
                    ? new Vector2(destinationX, destinationY)
                    : obstacles.findNextPathWaypoint(
                        x,
                        y,
                        destinationX,
                        destinationY,
                        width,
                        height
                    );
                pathRefreshTime = PATH_REFRESH_DURATION;
            }
            if (pathWaypoint == null) {
                return;
            }
            targetX = pathWaypoint.x;
            targetY = pathWaypoint.y;
        } else {
            timeUntilTurn -= deltaTime;
            if (timeUntilTurn <= 0f) {
                chooseDirection();
                targetX = x + width / 2f + (float) Math.cos(direction) * getMovementSpeed();
                targetY = y + height / 2f + (float) Math.sin(direction) * getMovementSpeed();
            }
        }

        moveToward(targetX, targetY, deltaTime, mapWidth, mapHeight);
        updateWalkingAnimation(deltaTime);
    }

    /**
     * Starts seed attraction when this bird follows the player or is within the seed's range.
     */
    public boolean attractToSeedsIfNearbyOrFollowing(
        float targetX,
        float targetY,
        float range,
        float duration
    ) {
        float deltaX = x + width / 2f - targetX;
        float deltaY = y + height / 2f - targetY;
        if (!followingPlayer && deltaX * deltaX + deltaY * deltaY > range * range) {
            return false;
        }

        seedTargetX = targetX;
        seedTargetY = targetY;
        seedAttractionTimeRemaining = duration;
        pathRefreshTime = 0f;
        return true;
    }

    /**
     * Moves toward the current target in short steps to prevent passing through obstacles.
     */
    private void moveToward(float targetX, float targetY, float deltaTime, int mapWidth, int mapHeight) {
        float deltaX = targetX - (x + width / 2f);
        float deltaY = targetY - (y + height / 2f);
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance < 0.001f) {
            return;
        }

        float travelDistance = Math.min(getMovementSpeed() * deltaTime, distance);
        int steps = Math.max(1, (int) Math.ceil(travelDistance / 2f));
        float stepX = deltaX / distance * travelDistance / steps;
        float stepY = deltaY / distance * travelDistance / steps;

        for (int step = 0; step < steps; step++) {
            float nextX = x + stepX;
            float nextY = y + stepY;
            if (nextX < 0f || nextX > mapWidth - width
                || nextY < 0f || nextY > mapHeight - height
                || isBlockedAt(nextX, nextY)) {
                if (!followingPlayer) {
                    chooseClearDirection();
                } else {
                    pathRefreshTime = 0f;
                }
                return;
            }
            x = nextX;
            y = nextY;
        }
    }

    /**
     * Advances the fly frames while the bird is stopped in its contact-attack pause.
     */
    private void updateAttackAnimation(float deltaTime) {
        animationTime += deltaTime;
        while (animationTime >= FRAME_DURATION) {
            animationTime -= FRAME_DURATION;
            currentFrame = (currentFrame + 1) % attackFrames.length;
        }
    }

    /**
     * Advances the walking frames while the bird is moving outside its attack pause.
     */
    private void updateWalkingAnimation(float deltaTime) {
        animationTime += deltaTime;
        while (animationTime >= FRAME_DURATION) {
            animationTime -= FRAME_DURATION;
            currentFrame = (currentFrame + 1) % walkingFrames.length;
        }
    }

    /**
     * Draws the current walking or attack frame at the bird's position.
     */
    public void render(SpriteBatch batch) {
        TextureRegion[] frames = attackTimeRemaining > 0f ? attackFrames : walkingFrames;
        int frame = currentFrame % frames.length;
        batch.draw(frames[frame], x, y, width, height);
    }

    /**
     * Picks a random walking direction and sets a timer before the bird changes course again.
     */
    private void chooseDirection() {
        direction = random.nextFloat() * (float) (Math.PI * 2);
        timeUntilTurn = 1f + random.nextFloat() * 2f;
    }

    /**
     * Tries alternative headings after a roaming bird encounters an obstacle.
     */
    private void chooseClearDirection() {
        for (int attempt = 0; attempt < 12; attempt++) {
            float candidateDirection = random.nextFloat() * (float) (Math.PI * 2);
            float candidateX = x + (float) Math.cos(candidateDirection) * 8f;
            float candidateY = y + (float) Math.sin(candidateDirection) * 8f;
            if (!isBlockedAt(candidateX, candidateY)) {
                direction = candidateDirection;
                timeUntilTurn = 1f + random.nextFloat() * 2f;
                return;
            }
        }
        chooseDirection();
    }

    /**
     * Checks a proposed bird position against map obstacles and active statue protection.
     */
    private boolean isBlockedAt(float candidateX, float candidateY) {
        return obstacles.collides(candidateX, candidateY, width, height)
            || longBoi.collidesForBirds(candidateX, candidateY, width, height, x, y);
    }

    /**
     * Checks whether two world-space rectangles overlap, which triggers one attack per contact.
     */
    private boolean overlaps(
        float firstX,
        float firstY,
        float firstWidth,
        float firstHeight,
        float secondX,
        float secondY,
        float secondWidth,
        float secondHeight
    ) {
        return firstX < secondX + secondWidth
            && firstX + firstWidth > secondX
            && firstY < secondY + secondHeight
            && firstY + firstHeight > secondY;
    }
}
