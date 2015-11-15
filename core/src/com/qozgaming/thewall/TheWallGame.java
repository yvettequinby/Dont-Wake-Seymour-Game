package com.qozgaming.thewall;

import com.badlogic.gdx.Game;
import com.qozgaming.thewall.screens.GameScreen;

public class TheWallGame extends Game {
	
	public TheWallGame() {
		super();
	}
	
	@Override
	public void create() {
		setScreen(new GameScreen(this));
		//setScreen(new TestScreen());
	}
	
	public void restart() {
		setScreen(new GameScreen(this));
	}
	
	
}
