package com.javasaurs.goosepatrol.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Timer {
    private static final float START_TIME_SECONDS = 5f * 60f;
    private static final float DIGIT_HEIGHT = 40f;
    private static final float DIGIT_GAP = 3f;
    private static final float RIGHT_MARGIN = 12f;
    private static final float TOP_MARGIN = 12f;
    private static final float VIEWPORT_WIDTH = 1280f;
    private static final float VIEWPORT_HEIGHT = 720f;
    private static final int[][] DIGIT_BOUNDS = {
        {43, 239, 211, 485},
        {245, 239, 384, 485},
        {416, 239, 584, 485},
        {617, 239, 781, 485},
        {809, 239, 996, 485},
        {1020, 239, 1187, 485},
        {1223, 239, 1392, 485},
        {1420, 239, 1594, 485},
        {1615, 239, 1795, 485},
        {1821, 239, 1991, 485}
    };
    private static final int[] COLON_BOUNDS = {2043, 263, 2129, 469};

    private final Texture texture;
    private final TextureRegion[] digits = new TextureRegion[10];
    private final TextureRegion colon;
    private float remainingSeconds = START_TIME_SECONDS;

    /**
     * Loads the timer sprite sheet, slices each digit image, and initializes the colon separator.
     */
    public Timer() {
        texture = new Texture(Gdx.files.internal("UI/Retro Pixel Timer Digits Sprite Sheet.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        for (int digit = 0; digit < DIGIT_BOUNDS.length; digit++) {
            int[] bounds = DIGIT_BOUNDS[digit];
            digits[digit] = new TextureRegion(
                texture,
                bounds[0],
                bounds[1],
                bounds[2] - bounds[0],
                bounds[3] - bounds[1]
            );
        }

        colon = new TextureRegion(
            texture,
            COLON_BOUNDS[0],
            COLON_BOUNDS[1],
            COLON_BOUNDS[2] - COLON_BOUNDS[0],
            COLON_BOUNDS[3] - COLON_BOUNDS[1]
        );
    }

    /**
     * Counts down the timer by the time passed in the last frame and never lets the value go below zero.
     */
    public void update(float deltaSeconds) {
        remainingSeconds = Math.max(0f, remainingSeconds - deltaSeconds);
    }

    /**
     * Draws the countdown time on screen as a minutes:seconds display using the sprite-sheet digits.
     */
    public void render(SpriteBatch batch) {
        int totalSeconds = (int) Math.ceil(remainingSeconds);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        int[] displayedDigits = {minutes / 10, minutes % 10, seconds / 10, seconds % 10};

        float[] widths = new float[displayedDigits.length];
        float totalWidth = 0f;
        for (int i = 0; i < displayedDigits.length; i++) {
            widths[i] = getDigitWidth(displayedDigits[i]);
            totalWidth += widths[i];
        }

        float colonHeight = DIGIT_HEIGHT * colon.getRegionHeight()
            / (DIGIT_BOUNDS[0][3] - DIGIT_BOUNDS[0][1]);
        float colonWidth = colonHeight * colon.getRegionWidth() / colon.getRegionHeight();
        totalWidth += colonWidth + DIGIT_GAP * 4f;

        float x = VIEWPORT_WIDTH - RIGHT_MARGIN - totalWidth;
        float y = VIEWPORT_HEIGHT - TOP_MARGIN - DIGIT_HEIGHT;
        for (int i = 0; i < 2; i++) {
            drawDigit(batch, displayedDigits[i], x, y, widths[i]);
            x += widths[i] + DIGIT_GAP;
        }

        // Draw the colon separator between the minute and second digits.
        batch.draw(colon, x, y + (DIGIT_HEIGHT - colonHeight) / 2f, colonWidth, colonHeight);
        x += colonWidth + DIGIT_GAP;

        for (int i = 2; i < displayedDigits.length; i++) {
            drawDigit(batch, displayedDigits[i], x, y, widths[i]);
            x += widths[i] + DIGIT_GAP;
        }
    }

    /**
     * Calculates how wide a numeric sprite should be when scaled to the timer's fixed digit height.
     */
    private float getDigitWidth(int digit) {
        return DIGIT_HEIGHT * digits[digit].getRegionWidth()
            / digits[digit].getRegionHeight();
    }

    /**
     * Renders a single digit sprite at the requested position and width.
     */
    private void drawDigit(SpriteBatch batch, int digit, float x, float y, float width) {
        batch.draw(digits[digit], x, y, width, DIGIT_HEIGHT);
    }

    /**
     * Releases the timer texture when the UI is being cleaned up.
     */
    public void dispose() {
        texture.dispose();
    }
}
