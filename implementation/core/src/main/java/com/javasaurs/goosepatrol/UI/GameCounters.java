package com.javasaurs.goosepatrol.UI;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GameCounters {
    private static final float LEFT_MARGIN = 12f;
    private static final float TOP_MARGIN = 12f;
    private static final float LINE_HEIGHT = 24f;
    private static final float VIEWPORT_HEIGHT = 720f;

    private final BitmapFont font = new BitmapFont();
    private int itemsUsedCount;
    private int eventCount;

    /**
     * Creates the counter font and scales it for the game's UI.
     */
    public GameCounters() {
        font.getData().setScale(1.5f);
    }

    /**
     * Increases the number of items used by one.
     */
    public void incrementItemUseCount() {
        itemsUsedCount++;
    }

    /**
     * Increases the number of triggered events by one.
     */
    public void incrementEventCount() {
        eventCount++;
    }

    /**
     * Draws the current item and event totals in the upper-left corner.
     */
    public void render(SpriteBatch batch) {
        float firstLineY = VIEWPORT_HEIGHT - TOP_MARGIN - font.getLineHeight();
        font.draw(batch, "Items used: " + itemsUsedCount, LEFT_MARGIN, firstLineY);
        font.draw(batch, "Events: " + eventCount, LEFT_MARGIN, firstLineY - LINE_HEIGHT);
    }

    /**
     * Releases the font resources used to display the counters.
     */
    public void dispose() {
        font.dispose();
    }
}
