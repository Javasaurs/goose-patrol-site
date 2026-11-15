package com.javasaurs.goosepatrol.Events;

import com.javasaurs.goosepatrol.ObstacleMap;

public class Lake {
    private static final int ACTIVATION_RADIUS_TILES = 2;
    private static final float COOLDOWN_SECONDS = 15f;

    private float cooldownRemaining;
    private boolean readyForActivation = true;

    /**
     * Returns true once when the player enters the lake radius and the cooldown has elapsed.
     */
    public boolean update(float deltaTime, float playerX, float playerY, ObstacleMap obstacles) {
        cooldownRemaining = Math.max(0f, cooldownRemaining - deltaTime);
        boolean withinRadius = obstacles.isWithinTilesOfWater(
            playerX,
            playerY,
            ACTIVATION_RADIUS_TILES
        );

        if (!withinRadius) {
            if (cooldownRemaining == 0f) {
                readyForActivation = true;
            }
            return false;
        }

        if (!readyForActivation || cooldownRemaining > 0f) {
            return false;
        }

        readyForActivation = false;
        cooldownRemaining = COOLDOWN_SECONDS;
        return true;
    }
}
