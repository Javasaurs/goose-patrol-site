package com.javasaurs.goosepatrol.Geese;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.javasaurs.goosepatrol.Events.LongBoi;
import com.javasaurs.goosepatrol.ObstacleMap;

public class Swan extends BaseGoose {
    private static final float MOVEMENT_SPEED = 28f;

    /**
     * Creates a swan instance using the generic goose movement system with slower movement than the other birds.
     */
    public Swan(
        TextureRegion[] walkingFrames,
        TextureRegion[] attackFrames,
        float x,
        float y,
        float width,
        float height,
        ObstacleMap obstacles,
        LongBoi longBoi
    ) {
        super(walkingFrames, attackFrames, x, y, width, height, obstacles, longBoi);
    }

    /**
     * Returns the swan's slower movement speed so it drifts across the map more deliberately.
     */
    @Override
    protected float getMovementSpeed() {
        return MOVEMENT_SPEED;
    }
}
