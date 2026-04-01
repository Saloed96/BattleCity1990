package com.saloed.bcity.graphics;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Sprite {

	private SpriteSheet sheet;
	private float scale;

	public Sprite(SpriteSheet sheet, float scale) {
		this.scale = scale;
		this.sheet = sheet;
	}

	public void render(GraphicsContext g, float x, float y) {
		// Get current sprite index from sheet (for animation)
		Image image = sheet.getSprite(sheet.getCurrentSpriteIndex());
		g.drawImage(image, x, y, (float) (image.getWidth() * scale), (float) (image.getHeight() * scale));
	}

	public SpriteSheet getSheet() {
		return sheet;
	}

	public float getScale() {
		return scale;
	}

}
