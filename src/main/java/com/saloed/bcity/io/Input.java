package com.saloed.bcity.io;

import javafx.scene.input.KeyCode;

import java.util.HashMap;
import java.util.Map;

public class Input {

	private Map<KeyCode, Boolean> keyMap;

	public Input() {
		keyMap = new HashMap<>();
	}

	public void setKeyPressed(KeyCode code, boolean pressed) {
		keyMap.put(code, pressed);
	}

	public boolean getKey(KeyCode keyCode) {
		return keyMap.getOrDefault(keyCode, false);
	}

	public boolean getKey(int keyCode) {
		// Legacy support for integer key codes - map common ones
		KeyCode code = mapIntToKeyCode(keyCode);
		return code != null && getKey(code);
	}

	private KeyCode mapIntToKeyCode(int keyCode) {
		return switch (keyCode) {
			case 38 -> KeyCode.UP;
			case 40 -> KeyCode.DOWN;
			case 37 -> KeyCode.LEFT;
			case 39 -> KeyCode.RIGHT;
			case 32 -> KeyCode.SPACE;
			case 10 -> KeyCode.ENTER;
			case 27 -> KeyCode.ESCAPE;
			default -> null;
		};
	}

}
