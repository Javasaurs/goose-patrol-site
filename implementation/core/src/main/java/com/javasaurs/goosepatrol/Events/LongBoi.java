package com.javasaurs.goosepatrol.Events;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;

public class LongBoi {
    private static final String COLLISION_LAYER_NAME = "Collision";
    private static final String STATUE_NAME = "Long Boi Statue";
    private static final int PROTECTION_RADIUS_TILES = 3;

    private final List<Rectangle> statues = new ArrayList<>();
    private final float protectionRadius;
    private boolean protectionActive;

    /**
     * Loads the statue collision bounds and sets the bird-protection radius from the map tile size.
     */
    public LongBoi(TiledMap map) {
        MapLayer collisionLayer = map.getLayers().get(COLLISION_LAYER_NAME);
        if (collisionLayer == null) {
            throw new IllegalStateException("Missing map layer: " + COLLISION_LAYER_NAME);
        }

        for (MapObject object : collisionLayer.getObjects()) {
            if (!STATUE_NAME.equals(object.getName())) {
                continue;
            }
            if (!(object instanceof RectangleMapObject)) {
                throw new IllegalStateException(STATUE_NAME + " must have rectangular collision bounds");
            }
            statues.add(((RectangleMapObject) object).getRectangle());
        }

        if (statues.isEmpty()) {
            throw new IllegalStateException("No " + STATUE_NAME + " objects found in the map");
        }

        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        protectionRadius = tileWidth * PROTECTION_RADIUS_TILES;
    }

    /**
     * Activates bird protection while the player is within three tiles of a statue.
     */
    public void update(float playerX, float playerY) {
        float radiusSquared = protectionRadius * protectionRadius;
        protectionActive = false;
        for (Rectangle statue : statues) {
            if (distanceSquaredToRectangle(playerX, playerY, statue) <= radiusSquared) {
                protectionActive = true;
                return;
            }
        }
    }

    /**
     * Checks a bird's next position against active statue protection, allowing trapped birds to move outward.
     */
    public boolean collidesForBirds(
        float x,
        float y,
        float width,
        float height,
        float currentX,
        float currentY
    ) {
        if (!protectionActive) {
            return false;
        }

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float currentCenterX = currentX + width / 2f;
        float currentCenterY = currentY + height / 2f;
        float radiusSquared = protectionRadius * protectionRadius;

        for (Rectangle statue : statues) {
            float nextDistanceSquared = distanceSquaredToRectangle(centerX, centerY, statue);
            if (nextDistanceSquared > radiusSquared) {
                continue;
            }

            float currentDistanceSquared = distanceSquaredToRectangle(
                currentCenterX,
                currentCenterY,
                statue
            );
            if (currentDistanceSquared > radiusSquared
                || nextDistanceSquared < currentDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    /**
     * Measures squared distance from a point to the nearest point on a rectangle.
     */
    private float distanceSquaredToRectangle(float x, float y, Rectangle rectangle) {
        float nearestX = Math.max(rectangle.x, Math.min(x, rectangle.x + rectangle.width));
        float nearestY = Math.max(rectangle.y, Math.min(y, rectangle.y + rectangle.height));
        float distanceX = x - nearestX;
        float distanceY = y - nearestY;
        return distanceX * distanceX + distanceY * distanceY;
    }
}
