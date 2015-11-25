package com.qozgaming.thewall.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.qozgaming.thewall.TheWallGame;
import com.qozgaming.thewall.utils.Constants;

public class SplashScreen extends InputAdapter implements Screen {
	
	
	private final TheWallGame game;
	private OrthographicCamera camera;
	private Viewport viewport;
	private SpriteBatch batch;
	private AssetManager assetManager;
	private TextureAtlas textureAtlas;
	private TextureRegion backgroundTexture;
	
	
	public SplashScreen(TheWallGame game) {
		this.game = game;
		camera = new OrthographicCamera();
		camera.position.set(Constants.VIRTUAL_WIDTH * 0.5f, Constants.VIRTUAL_HEIGHT * 0.5f, 0.0f);
		viewport = new FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT, camera);
		batch = new SpriteBatch();
		assetManager = new AssetManager();
		assetManager.load(Constants.ASSET_PACK, TextureAtlas.class);
		assetManager.finishLoading(); // Blocks until all resources are loaded into memory
		textureAtlas = assetManager.get(Constants.ASSET_PACK);
		backgroundTexture = textureAtlas.findRegion(Constants.ASSET_SPLASH_IMAGE);
	}

	
	@Override
	public void show() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(float delta) {
		
		Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(camera.combined);

		batch.begin();

		batch.draw(backgroundTexture, 0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT);
		
		batch.end();
		
		if(Gdx.input.justTouched()) {
            game.showIntroScreen();
		}
	}

	
	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, false);
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		assetManager.dispose();
		batch.dispose();
	}
	
	
}
