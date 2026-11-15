package com.javasaurs.goosepatrol.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Inventory {
    private static final int SLOT_COUNT = 5;
    private static final float SLOT_SIZE = 48f;
    private static final float SLOT_GAP = 8f;
    private static final float BOTTOM_MARGIN = 12f;
    private static final float VIEWPORT_WIDTH = 1280f;
    private static final float ITEM_SIZE = 30f;
    private static final float ITEM_COOLDOWN_SECONDS = 30f;
    private static final int FRAME_X = 512;
    private static final int FRAME_Y = 256;
    private static final int FRAME_SIZE = 512;

    private final Texture texture;
    private final Texture seedPacketTexture;
    private final Texture energyDrinkTexture;
    private final TextureRegion slotFrame;
    private float seedCooldownRemaining;
    private float energyDrinkCooldownRemaining;

    /**
     * Loads the inventory frame sprite and crops the single slot image used for each item slot.
     */
    public Inventory() {
        texture = new Texture(Gdx.files.internal("UI/Pixel-Art Inventory Slot Frame.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        slotFrame = new TextureRegion(texture, FRAME_X, FRAME_Y, FRAME_SIZE, FRAME_SIZE);
        seedPacketTexture = loadItemTexture("UI/Pixel-Art Seed Packet Icon.png");
        energyDrinkTexture = loadItemTexture("UI/Pixel Art Energy Drink Bottle.png");
    }

    /**
     * Loads an item texture from the game's assets and configures it for pixel-art rendering.
     */
    private Texture loadItemTexture(String path) {
        Texture itemTexture = new Texture(Gdx.files.internal(path));
        itemTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return itemTexture;
    }

    /**
     * Draws the full inventory bar across the lower part of the screen with evenly spaced slots.
     */
    public void render(SpriteBatch batch) {
        float totalWidth = SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * SLOT_GAP;
        float startX = (VIEWPORT_WIDTH - totalWidth) / 2f;

        for (int i = 0; i < SLOT_COUNT; i++) {
            float x = startX + i * (SLOT_SIZE + SLOT_GAP);
            batch.draw(slotFrame, x, BOTTOM_MARGIN, SLOT_SIZE, SLOT_SIZE);
            if (i == 0) {
                batch.draw(
                    seedPacketTexture,
                    x + (SLOT_SIZE - ITEM_SIZE) / 2f,
                    BOTTOM_MARGIN + (SLOT_SIZE - ITEM_SIZE) / 2f,
                    ITEM_SIZE,
                    ITEM_SIZE
                );
            } else if (i == 1) {
                batch.draw(
                    energyDrinkTexture,
                    x + (SLOT_SIZE - ITEM_SIZE) / 2f,
                    BOTTOM_MARGIN + (SLOT_SIZE - ITEM_SIZE) / 2f,
                    ITEM_SIZE,
                    ITEM_SIZE
                );
            }
        }
    }

    /**
     * Advances the cooldowns for the inventory items.
     */
    public void update(float deltaSeconds) {
        seedCooldownRemaining = Math.max(0f, seedCooldownRemaining - deltaSeconds);
        energyDrinkCooldownRemaining = Math.max(0f, energyDrinkCooldownRemaining - deltaSeconds);
    }

    /**
     * Uses the seed packet if its cooldown has elapsed.
     */
    public boolean useSeedPacket() {
        if (seedCooldownRemaining > 0f) {
            return false;
        }
        seedCooldownRemaining = ITEM_COOLDOWN_SECONDS;
        return true;
    }

    /**
     * Uses the energy drink if its cooldown has elapsed.
     */
    public boolean useEnergyDrink() {
        if (energyDrinkCooldownRemaining > 0f) {
            return false;
        }
        energyDrinkCooldownRemaining = ITEM_COOLDOWN_SECONDS;
        return true;
    }

    /**
     * Releases the texture data used to draw the inventory slots.
     */
    public void dispose() {
        texture.dispose();
        seedPacketTexture.dispose();
        energyDrinkTexture.dispose();
    }
}
