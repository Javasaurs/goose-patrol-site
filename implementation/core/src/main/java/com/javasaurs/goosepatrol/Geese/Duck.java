package com.javasaurs.goosepatrol.Geese;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.javasaurs.goosepatrol.Events.LongBoi;
import com.javasaurs.goosepatrol.ObstacleMap;

public class Duck extends BaseGoose {
    private static final float MOVEMENT_SPEED = 68f; 

    /**
     * Creates a duck instance that uses the shared goose movement logic at a faster pace.
     */
    public Duck(
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
     * Returns the speed value used by ducks so they move quicker than other bird types.
     */
    @Override
    protected float getMovementSpeed() {
        return MOVEMENT_SPEED;
    }
}
