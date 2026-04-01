package com.saloed.bcity.graphics;

import com.saloed.bcity.utils.ResourceLoader;
import javafx.scene.image.Image;

public class SpriteSheet {

	private Image sheet;
	private int spriteCount;
	private int scale;
	private int spritesInWidth;
	private int currentSpriteIndex = 0; // Current animation frame

	public SpriteSheet(Image sheet, int spriteCount, int scale) {
		this.sheet = sheet;
		this.spriteCount = spriteCount;
		this.scale = scale;

		this.spritesInWidth = (int) (sheet.getWidth() / scale);

	}

	public Image getSprite(int index) {
		// Use the currentSpriteIndex if provided index is just a frame counter
		int actualIndex = (currentSpriteIndex < spriteCount) ? currentSpriteIndex : (index % spriteCount);
		actualIndex = actualIndex % spriteCount;

		int x = actualIndex % spritesInWidth * scale;
		int y = actualIndex / spritesInWidth * scale;

		return ResourceLoader.cutImage(sheet, x, y, scale, scale);
	}

	public void setSpriteIndex(int index) {
		this.currentSpriteIndex = index % spriteCount;
	}

	public int getCurrentSpriteIndex() {
		return currentSpriteIndex;
	}

}
