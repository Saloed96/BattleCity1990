package com.saloed.bcity.graphics;

import com.saloed.bcity.utils.ResourceLoader;
import javafx.scene.image.Image;

public class TextureAtlas {

	private Image image;

	public TextureAtlas(String imageName) {
		image = ResourceLoader.loadImage(imageName);
	}

	public Image cut(int x, int y, int w, int h) {
		return ResourceLoader.cutImage(image, x, y, w, h);
	}

	public Image getImage() {
		return image;
	}

}
