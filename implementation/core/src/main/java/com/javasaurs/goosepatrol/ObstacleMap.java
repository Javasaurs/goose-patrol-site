package com.javasaurs.goosepatrol;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class ObstacleMap {
    private static final String TERRAIN_LAYER_NAME = "Terrain_and_Paths";
    private static final String OBSTACLE_LAYER_NAME = "Buildings_Trees_and_Details";
    // Terrain-layer IDs for water and walkable paths in the campus map tileset.
    private static final int WATER_TILE_ID_START = 9;
    private static final int WATER_TILE_ID_END = 10;
    private static final int PATH_TILE_ID_START = 7;
    private static final int PATH_TILE_ID_END = 8;

    // A tile is blocked by either water terrain or a solid tile in the obstacle layer.
    private final boolean[][] blockedTiles;
    private final boolean[][] waterTiles;
    private final boolean[][] pathTiles;
    private final int tileWidth;
    private final int tileHeight;
    private final Random random = new Random();

    /**
     * Reads terrain and obstacle layers so objects can test movement and path placement.
     */
    public ObstacleMap(TiledMap map) {
        TiledMapTileLayer terrainLayer = getTileLayer(map, TERRAIN_LAYER_NAME);
        TiledMapTileLayer obstacleLayer = getTileLayer(map, OBSTACLE_LAYER_NAME);
        tileWidth = (int) terrainLayer.getTileWidth();
        tileHeight = (int) terrainLayer.getTileHeight();
        blockedTiles = new boolean[terrainLayer.getWidth()][terrainLayer.getHeight()];
        waterTiles = new boolean[terrainLayer.getWidth()][terrainLayer.getHeight()];
        pathTiles = new boolean[terrainLayer.getWidth()][terrainLayer.getHeight()];

        if (obstacleLayer.getWidth() != terrainLayer.getWidth()
            || obstacleLayer.getHeight() != terrainLayer.getHeight()) {
            throw new IllegalStateException("Terrain and obstacle layers must have matching dimensions");
        }

        // Combine terrain and obstacle data so the existing collision check handles both.
        for (int tileX = 0; tileX < terrainLayer.getWidth(); tileX++) {
            for (int tileY = 0; tileY < terrainLayer.getHeight(); tileY++) {
                TiledMapTileLayer.Cell terrainCell = terrainLayer.getCell(tileX, tileY);
                TiledMapTileLayer.Cell obstacleCell = obstacleLayer.getCell(tileX, tileY);
                if (terrainCell != null && terrainCell.getTile() != null) {
                    int terrainTileId = terrainCell.getTile().getId();
                    pathTiles[tileX][tileY] = isPathTile(terrainTileId);
                    waterTiles[tileX][tileY] = isWaterTile(terrainTileId);
                    blockedTiles[tileX][tileY] = waterTiles[tileX][tileY];
                }
                if (obstacleCell != null && obstacleCell.getTile() != null
                    && isBlockingTile(obstacleCell.getTile().getId())) {
                    blockedTiles[tileX][tileY] = true;
                }
            }
        }
    }

    /**
     * Finds the named map layer and verifies that it contains tile data.
     */
    private TiledMapTileLayer getTileLayer(TiledMap map, String layerName) {
        MapLayer mapLayer = map.getLayers().get(layerName);
        if (!(mapLayer instanceof TiledMapTileLayer)) {
            throw new IllegalStateException("Missing tiled map layer: " + layerName);
        }
        return (TiledMapTileLayer) mapLayer;
    }

    /**
     * Selects a uniformly random player centre whose full footprint lies on clear path tiles.
     */
    public Vector2 getRandomPathPosition(float width, float height) {
        return getRandomPathPosition(width, height, width, height);
    }

    /**
     * Selects a random player centre whose collision footprint lies on clear path tiles.
     */
    public Vector2 getRandomPathPosition(
        float width,
        float height,
        float collisionWidth,
        float collisionHeight
    ) {
        Vector2 position = null;
        int validPositionCount = 0;

        for (int tileX = 0; tileX < pathTiles.length; tileX++) {
            for (int tileY = 0; tileY < pathTiles[tileX].length; tileY++) {
                // PlayerController expects its constructor coordinates to be the sprite centre.
                float centerX = tileX * tileWidth + tileWidth / 2f;
                float centerY = tileY * tileHeight + tileHeight / 2f;
                float x = centerX - width / 2f;
                float y = centerY - height / 2f;
                float collisionX = x + (width - collisionWidth) / 2f;

                if (!isPathPosition(collisionX, y, collisionWidth, collisionHeight)
                    || collides(collisionX, y, collisionWidth, collisionHeight)) {
                    continue;
                }

                validPositionCount++;
                // Reservoir sampling chooses among all valid tiles without storing them in a list.
                if (random.nextInt(validPositionCount) == 0) {
                    position = new Vector2(centerX, centerY);
                }
            }
        }

        if (position == null) {
            throw new IllegalStateException("Could not find a clear path position for the player");
        }
        return position;
    }

    /**
     * Returns whether every tile overlapped by the supplied rectangle is a walkable path tile.
     */
    private boolean isPathPosition(float x, float y, float width, float height) {
        if (x < 0f || y < 0f
            || x + width > pathTiles.length * tileWidth
            || y + height > pathTiles[0].length * tileHeight) {
            return false;
        }

        // Check every tile touched by the entity, not just the tile containing its centre.
        int minTileX = (int) Math.floor(x / tileWidth);
        int minTileY = (int) Math.floor(y / tileHeight);
        int maxTileX = (int) Math.floor((x + width - 0.001f) / tileWidth);
        int maxTileY = (int) Math.floor((y + height - 0.001f) / tileHeight);

        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                if (!pathTiles[tileX][tileY]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks whether the supplied rectangle intersects any blocked tile on the map.
     */
    public boolean collides(float x, float y, float width, float height) {
        int minTileX = Math.max(0, (int) Math.floor(x / tileWidth));
        int minTileY = Math.max(0, (int) Math.floor(y / tileHeight));
        int maxTileX = Math.min(
            blockedTiles.length - 1,
            (int) Math.floor((x + width - 0.001f) / tileWidth)
        );
        int maxTileY = Math.min(
            blockedTiles[0].length - 1,
            (int) Math.floor((y + height - 0.001f) / tileHeight)
        );

        // Test every tile the entity overlaps to detect collisions with world obstacles.
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                if (blockedTiles[tileX][tileY]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether a bird can see a target within a circular tile range without a blocked tile between them.
     */
    public boolean hasLineOfSightWithinTiles(
        float startX,
        float startY,
        float endX,
        float endY,
        int rangeInTiles
    ) {
        float distanceX = (endX - startX) / tileWidth;
        float distanceY = (endY - startY) / tileHeight;
        if (distanceX * distanceX + distanceY * distanceY > rangeInTiles * rangeInTiles) {
            return false;
        }
        return hasClearLineOfSight(startX, startY, endX, endY);
    }

    /**
     * Checks a segment at half-tile intervals so narrow walls and water crossings block visibility.
     */
    public boolean hasClearLineOfSight(float startX, float startY, float endX, float endY) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        float sampleSpacing = Math.min(tileWidth, tileHeight) / 2f;
        int sampleCount = Math.max(1, (int) Math.ceil(distance / sampleSpacing));

        for (int sample = 1; sample < sampleCount; sample++) {
            float progress = (float) sample / sampleCount;
            float sampleX = startX + deltaX * progress;
            float sampleY = startY + deltaY * progress;
            if (collides(sampleX, sampleY, 1f, 1f)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Uses A* over walkable tile centres to return the next waypoint for a bird-sized entity.
     */
    public Vector2 findNextPathWaypoint(
        float startX,
        float startY,
        float targetX,
        float targetY,
        float entityWidth,
        float entityHeight
    ) {
        int startTileX = (int) Math.floor((startX + entityWidth / 2f) / tileWidth);
        int startTileY = (int) Math.floor((startY + entityHeight / 2f) / tileHeight);
        int targetTileX = (int) Math.floor(targetX / tileWidth);
        int targetTileY = (int) Math.floor(targetY / tileHeight);
        int mapTileWidth = blockedTiles.length;
        int mapTileHeight = blockedTiles[0].length;

        if (!isWalkableTile(startTileX, startTileY, entityWidth, entityHeight)) {
            return null;
        }
        boolean[][] goalTiles = new boolean[mapTileWidth][mapTileHeight];
        if (isWalkableTile(targetTileX, targetTileY, entityWidth, entityHeight)) {
            goalTiles[targetTileX][targetTileY] = true;
        } else {
            // A bird can be wider than the player's foot collision box, so accept any reachable nearby tile.
            boolean foundNearbyGoal = false;
            for (int offsetX = -3; offsetX <= 3; offsetX++) {
                for (int offsetY = -3; offsetY <= 3; offsetY++) {
                    int candidateX = targetTileX + offsetX;
                    int candidateY = targetTileY + offsetY;
                    if (isWalkableTile(candidateX, candidateY, entityWidth, entityHeight)) {
                        goalTiles[candidateX][candidateY] = true;
                        foundNearbyGoal = true;
                    }
                }
            }
            if (!foundNearbyGoal) {
                return null;
            }
        }
        if (goalTiles[startTileX][startTileY]) {
            return new Vector2(targetX, targetY);
        }

        float[][] pathCosts = new float[mapTileWidth][mapTileHeight];
        int[][] parentX = new int[mapTileWidth][mapTileHeight];
        int[][] parentY = new int[mapTileWidth][mapTileHeight];
        boolean[][] closed = new boolean[mapTileWidth][mapTileHeight];
        for (int tileX = 0; tileX < mapTileWidth; tileX++) {
            for (int tileY = 0; tileY < mapTileHeight; tileY++) {
                pathCosts[tileX][tileY] = Float.POSITIVE_INFINITY;
                parentX[tileX][tileY] = -1;
                parentY[tileX][tileY] = -1;
            }
        }

        // A* prioritizes the lowest estimated total cost, using diagonal distance for an 8-way grid.
        PriorityQueue<PathNode> openNodes = new PriorityQueue<>(
            Comparator.comparingDouble(node -> node.estimatedTotalCost)
        );
        pathCosts[startTileX][startTileY] = 0f;
        openNodes.add(new PathNode(
            startTileX,
            startTileY,
            heuristic(startTileX, startTileY, targetTileX, targetTileY)
        ));

        while (!openNodes.isEmpty()) {
            PathNode current = openNodes.remove();
            if (closed[current.x][current.y]) {
                continue;
            }
            if (goalTiles[current.x][current.y]) {
                int waypointX = current.x;
                int waypointY = current.y;
                while (parentX[waypointX][waypointY] != startTileX
                    || parentY[waypointX][waypointY] != startTileY) {
                    int previousX = parentX[waypointX][waypointY];
                    int previousY = parentY[waypointX][waypointY];
                    if (previousX < 0 || previousY < 0) {
                        return null;
                    }
                    waypointX = previousX;
                    waypointY = previousY;
                }
                return new Vector2(
                    (waypointX + 0.5f) * tileWidth,
                    (waypointY + 0.5f) * tileHeight
                );
            }
            closed[current.x][current.y] = true;

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    if (offsetX == 0 && offsetY == 0) {
                        continue;
                    }
                    int nextX = current.x + offsetX;
                    int nextY = current.y + offsetY;
                    if (!isWalkableTile(nextX, nextY, entityWidth, entityHeight)
                        || closed[nextX][nextY]) {
                        continue;
                    }
                    if (offsetX != 0 && offsetY != 0
                        && (!isWalkableTile(current.x + offsetX, current.y, entityWidth, entityHeight)
                            || !isWalkableTile(current.x, current.y + offsetY, entityWidth, entityHeight))) {
                        continue;
                    }

                    float movementCost = offsetX != 0 && offsetY != 0 ? 1.41421356f : 1f;
                    float nextCost = pathCosts[current.x][current.y] + movementCost;
                    if (nextCost >= pathCosts[nextX][nextY]) {
                        continue;
                    }
                    pathCosts[nextX][nextY] = nextCost;
                    parentX[nextX][nextY] = current.x;
                    parentY[nextX][nextY] = current.y;
                    openNodes.add(new PathNode(
                        nextX,
                        nextY,
                        nextCost + heuristic(nextX, nextY, targetTileX, targetTileY)
                    ));
                }
            }
        }
        return null;
    }

    /**
     * Checks whether an entity's full footprint fits inside a map tile without overlapping a blocked tile.
     */
    private boolean isWalkableTile(int tileX, int tileY, float entityWidth, float entityHeight) {
        if (tileX < 0 || tileY < 0 || tileX >= blockedTiles.length || tileY >= blockedTiles[0].length) {
            return false;
        }
        float centerX = (tileX + 0.5f) * tileWidth;
        float centerY = (tileY + 0.5f) * tileHeight;
        return !collides(
            centerX - entityWidth / 2f,
            centerY - entityHeight / 2f,
            entityWidth,
            entityHeight
        );
    }

    /**
     * Estimates remaining 8-way grid distance without overestimating the shortest path.
     */
    private float heuristic(int startX, int startY, int targetX, int targetY) {
        int distanceX = Math.abs(targetX - startX);
        int distanceY = Math.abs(targetY - startY);
        return Math.max(distanceX, distanceY) + (1.41421356f - 1f) * Math.min(distanceX, distanceY);
    }

    /**
     * Stores a tile and its estimated total A* cost for the priority queue.
     */
    private static class PathNode {
        private final int x;
        private final int y;
        private final float estimatedTotalCost;

        /**
         * Creates a path-search node for a tile and its estimated total route cost.
         */
        private PathNode(int x, int y, float estimatedTotalCost) {
            this.x = x;
            this.y = y;
            this.estimatedTotalCost = estimatedTotalCost;
        }
    }

    /**
     * Checks whether a world position is within the requested tile distance of any water tile.
     */
    public boolean isWithinTilesOfWater(float x, float y, int tileRadius) {
        int searchRadius = tileRadius + 1;
        int centerTileX = (int) Math.floor(x / tileWidth);
        int centerTileY = (int) Math.floor(y / tileHeight);
        int minTileX = Math.max(0, centerTileX - searchRadius);
        int maxTileX = Math.min(waterTiles.length - 1, centerTileX + searchRadius);
        int minTileY = Math.max(0, centerTileY - searchRadius);
        int maxTileY = Math.min(waterTiles[0].length - 1, centerTileY + searchRadius);
        float radiusSquared = tileRadius * tileRadius;

        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                if (!waterTiles[tileX][tileY]) {
                    continue;
                }

                float tileLeft = tileX * tileWidth;
                float tileBottom = tileY * tileHeight;
                float distanceX = Math.max(
                    0f,
                    Math.max(tileLeft - x, x - (tileLeft + tileWidth))
                ) / tileWidth;
                float distanceY = Math.max(
                    0f,
                    Math.max(tileBottom - y, y - (tileBottom + tileHeight))
                ) / tileHeight;
                if (distanceX * distanceX + distanceY * distanceY <= radiusSquared) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Identifies tile IDs that represent water terrain.
     */
    private boolean isWaterTile(int tileId) {
        return tileId >= WATER_TILE_ID_START && tileId <= WATER_TILE_ID_END;
    }

    /**
     * Identifies tile IDs that represent walkable paths.
     */
    private boolean isPathTile(int tileId) {
        return tileId >= PATH_TILE_ID_START && tileId <= PATH_TILE_ID_END;
    }

    /**
     * Identifies which map tile IDs are treated as solid structures that should block movement.
     */
    private boolean isBlockingTile(int tileId) {
        switch (tileId) {
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 19:
            case 20:
            case 21:
            case 22:
            case 30:
                return true;
            default:
                return false;
        }
    }
}
