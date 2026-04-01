package com.saloed.bcity.game;

import com.saloed.bcity.io.Input;
import javafx.scene.canvas.GraphicsContext;

public abstract class Entity {

	public final EntityType type;

	protected float x;
	protected float y;

	protected Entity(EntityType type, float x, float y) {
		this.type = type;
		this.x = x;
		this.y = y;
	}

	public abstract void update(Input input);

	public abstract void render(GraphicsContext g);

	// Check collision with another entity
	public boolean intersects(Entity other) {
		float thisWidth = getWidth();
		float thisHeight = getHeight();
		float otherWidth = other.getWidth();
		float otherHeight = other.getHeight();

		return x < other.x + otherWidth &&
		       x + thisWidth > other.x &&
		       y < other.y + otherHeight &&
		       y + thisHeight > other.y;
	}

	public float getWidth() {
		return 32; // Default size (16 * 2 scale)
	}

	public float getHeight() {
		return 32;
	}

}
