package com.qozgaming.thewall;

import com.badlogic.gdx.Game;
import com.qozgaming.thewall.screens.GameScreen;
import com.qozgaming.thewall.screens.IntroScreen;
import com.qozgaming.thewall.screens.SplashScreen;

public class TheWallGame extends Game {
	
	private SplashScreen splashScreen;
	
	public TheWallGame() {
		super();
	}
	
	@Override
	public void create() {
		this.splashScreen = new SplashScreen(this);
		setScreen(new SplashScreen(this));
	}
	
	public void showSplashScreen() {
		setScreen(splashScreen);
	}
	
	public void showIntroScreen() {
		setScreen(new IntroScreen(this));
	}
	
	public void showGameScreen() {
		setScreen(new GameScreen(this));
	}
	
	
	
}
