package com.saloed.bcity;

import com.saloed.bcity.game.Game;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

	private static Game game;

	@Override
	public void start(Stage primaryStage) {
		game = new Game(primaryStage);
		game.startGame();
	}

	@Override
	public void stop() {
		if (game != null) {
			game.stopGame();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}