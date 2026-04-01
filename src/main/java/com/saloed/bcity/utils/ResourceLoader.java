package com.saloed.bcity.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class ResourceLoader {

	public static final String PATH = "/";

	public static Image loadImage(String fileName) {
		try {
			// Load image with transparency support (preserve alpha channel)
			return new Image(ResourceLoader.class.getResourceAsStream(PATH + fileName));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Image cutImage(Image source, int x, int y, int w, int h) {
		PixelReader reader = source.getPixelReader();
		WritableImage dest = new WritableImage(w, h);
		PixelWriter writer = dest.getPixelWriter();
		
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				int argb = reader.getArgb(x + i, y + j);
				// Extract color components
				int alpha = (argb >> 24) & 0xFF;
				int red = (argb >> 16) & 0xFF;
				int green = (argb >> 8) & 0xFF;
				int blue = argb & 0xFF;
				
				// If pixel is fully opaque and very dark (black or near-black), make it transparent
				// This handles sprites that have black background instead of transparency
				if (alpha > 128 && red < 30 && green < 30 && blue < 30) {
					argb = 0x00000000; // Fully transparent
				}
				writer.setArgb(i, j, argb);
			}
		}
		return dest;
	}

}
