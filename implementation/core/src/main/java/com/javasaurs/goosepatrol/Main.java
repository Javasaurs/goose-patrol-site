package com.javasaurs.goosepatrol;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.javasaurs.goosepatrol.Events.Lake;
import com.javasaurs.goosepatrol.Events.LongBoi;
import com.javasaurs.goosepatrol.Geese.GooseController;
import com.javasaurs.goosepatrol.Player.PlayerController;
import com.javasaurs.goosepatrol.UI.GameCounters;
import com.javasaurs.goosepatrol.UI.Inventory;
import com.javasaurs.goosepatrol.UI.Timer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private SpriteBatch spriteBatch;
    private PlayerController player;
    private GooseController geese;
    private Lake lake;
    private LongBoi longBoi;
    private ObstacleMap obstacles;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private OrthographicCamera uiCamera;
    private FitViewport uiViewport;
    private Inventory inventory;
    private Timer timer;
    private GameCounters gameCounters;
    private int mapWidth;
    private int mapHeight;

    /**
     * Sets up the map, camera, UI, player, and geese before the game loop starts running.
     */
    @Override
    public void create() {
        map = new TmxMapLoader().load("Map/york_campus.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        spriteBatch = new SpriteBatch();

        mapWidth = map.getProperties().get("width", Integer.class)
            * map.getProperties().get("tilewidth", Integer.class);
        mapHeight = map.getProperties().get("height", Integer.class)
            * map.getProperties().get("tileheight", Integer.class);

        camera = new OrthographicCamera();
        viewport = new FitViewport(1280f, 720f, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(1280f, 720f, uiCamera);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        inventory = new Inventory();
        timer = new Timer();
        gameCounters = new GameCounters();

        obstacles = new ObstacleMap(map);
        // Spawn the player at a random clear path location instead of the map centre.
        Vector2 playerSpawn = obstacles.getRandomPathPosition(32f, 48f, 16f, 8f);
        player = new PlayerController(playerSpawn.x, playerSpawn.y, obstacles);
        longBoi = new LongBoi(map);
        geese = new GooseController(mapWidth, mapHeight, obstacles, longBoi);
        lake = new Lake();
        updateCamera();
    }

    /**
     * Updates scene state each frame, then renders the map, creatures, and UI overlay.
     */
    @Override
    public void render() {
        float deltaSeconds = Gdx.graphics.getDeltaTime();
        inventory.update(deltaSeconds);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) && inventory.useSeedPacket()) {
            geese.useSeeds(player.getCenterX(), player.getCenterY());
            gameCounters.incrementItemUseCount();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) && inventory.useEnergyDrink()) {
            player.useEnergyDrink();
            gameCounters.incrementItemUseCount();
        }
        timer.update(deltaSeconds);
        player.update(deltaSeconds, mapWidth, mapHeight);
        longBoi.update(player.getCenterX(), player.getCenterY());
        if (lake.update(deltaSeconds, player.getCenterX(), player.getCenterY(), obstacles)) {
            geese.spawnFlock(player.getCenterX(), player.getCenterY(), mapWidth, mapHeight);
            gameCounters.incrementEventCount();
        }
        geese.update(
            deltaSeconds,
            mapWidth,
            mapHeight,
            player.getX(),
            player.getY(),
            player.getWidth(),
            player.getHeight()
        );
        updateCamera();

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        mapRenderer.setView(camera);
        mapRenderer.render();

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        geese.render(spriteBatch);
        player.render(spriteBatch);
        spriteBatch.end();

        uiViewport.apply();
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        inventory.render(spriteBatch);
        timer.render(spriteBatch);
        gameCounters.render(spriteBatch);
        spriteBatch.end();
    }

    /**
     * Keeps the camera centred on the player while preventing it from moving past the map boundaries.
     */
    private void updateCamera() {
        float halfViewportWidth = viewport.getWorldWidth() / 2f;
        float halfViewportHeight = viewport.getWorldHeight() / 2f;
        float cameraX = player.getCenterX();
        float cameraY = player.getCenterY();

        camera.position.set(
            Math.max(halfViewportWidth, Math.min(cameraX, mapWidth - halfViewportWidth)),
            Math.max(halfViewportHeight, Math.min(cameraY, mapHeight - halfViewportHeight)),
            0f
        );
        camera.update();
    }

    /**
     * Resizes both the world and UI viewports whenever the window changes size.
     */
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiViewport.update(width, height);
    }

    /**
     * Frees the textures and renderers used by the game scene when it is closing.
     */
    @Override
    public void dispose() {
        inventory.dispose();
        timer.dispose();
        gameCounters.dispose();
        geese.dispose();
        player.dispose();
        spriteBatch.dispose();
        mapRenderer.dispose();
        map.dispose();
    }
}
