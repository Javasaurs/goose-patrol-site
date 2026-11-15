package com.javasaurs.goosepatrol.Geese;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.javasaurs.goosepatrol.Events.LongBoi;
import com.javasaurs.goosepatrol.ObstacleMap;

public class Goose extends BaseGoose {
    private static final float MOVEMENT_SPEED = 48f;

    /**
     * Creates a standard goose with the base sprite and movement rules provided by the parent class.
     */
    public Goose(
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
     * Returns the walking speed used for normal geese in the simulation.
     */
    @Override
    protected float getMovementSpeed() {
        return MOVEMENT_SPEED;
    }
}
