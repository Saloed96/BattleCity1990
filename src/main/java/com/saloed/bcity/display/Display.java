package com.saloed.bcity.display;

import com.saloed.bcity.io.Input;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Display {

	private static boolean created = false;
	private static Stage stage;
	private static Canvas canvas;
	private static GraphicsContext graphicsContext;
	private static Color clearColor;
	private static Input input;

	public static void create(Stage primaryStage, int width, int height, String title, int _clearColor, int numBuffers) {
		if (created)
			return;

		stage = primaryStage;
		stage.setTitle(title);
		stage.setResizable(true);

		canvas = new Canvas(width, height);
		graphicsContext = canvas.getGraphicsContext2D();

		StackPane root = new StackPane(canvas);
		Scene scene = new Scene(root, width, height);

		// Setup input handling
		input = new Input();
		scene.setOnKeyPressed(e -> input.setKeyPressed(e.getCode(), true));
		scene.setOnKeyReleased(e -> input.setKeyPressed(e.getCode(), false));

		stage.setScene(scene);
		stage.centerOnScreen();

		clearColor = Color.web(String.format("#%06X", _clearColor & 0xFFFFFF));

		created = true;
	}

	public static void show() {
		if (stage != null) {
			stage.show();
		}
	}

	public static void clear() {
		graphicsContext.setFill(clearColor);
		graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	public static void swapBuffers() {
		// In JavaFX, rendering is immediate, no buffer swap needed
	}

	public static GraphicsContext getGraphics() {
		return graphicsContext;
	}

	public static void destroy() {
		if (!created)
			return;

		if (stage != null) {
			stage.close();
		}
		created = false;
	}

	public static void setTitle(String title) {
		if (stage != null) {
			stage.setTitle(title);
		}
	}

	public static Input getInput() {
		return input;
	}

	public static boolean isCreated() {
		return created;
	}

}
