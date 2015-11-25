package com.qozgaming.thewall.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.qozgaming.thewall.TheWallGame;
import com.qozgaming.thewall.utils.Constants;

public class IntroScreen extends InputAdapter implements Screen, ContactListener {
	
	
	private final TheWallGame game;
	private Viewport viewport;
	private OrthographicCamera fontCamera;
	private Viewport fontViewport;
	private World world;
	private SpriteBatch batch;
	
	private AssetManager assetManager;
	private TextureAtlas atlas;
	private TextureRegion backgroundTexture;
	private TextureRegion wallTexture;
	private BitmapFont font;
	private Sound meow;
	
	private Animation bossAnimation;
	private Animation catAnimation;
	private float bossAnimationTime = 0f;
	private float catAnimationTime = 0f;
	
	private Body boss;
	private Body seymour = null;
	private Body floorBody;
	private float timeSinceStart = 0f;
	private boolean animateSeymour = false;
	
	public IntroScreen(TheWallGame game) {
		
		this.game = game;
		viewport = new FitViewport(Constants.SCENE_WIDTH, Constants.SCENE_HEIGHT);
		viewport.getCamera().position.set(viewport.getCamera().position.x + Constants.SCENE_WIDTH * 0.5f, viewport.getCamera().position.y + Constants.SCENE_HEIGHT * 0.5f, 0);
		viewport.getCamera().update();
		
		fontCamera = new OrthographicCamera();
		fontCamera.position.set(Constants.VIRTUAL_WIDTH * 0.5f, Constants.VIRTUAL_HEIGHT * 0.5f, 0.0f);
		fontViewport = new FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT, fontCamera);
		
		assetManager = new AssetManager();
		assetManager.load(Constants.ASSET_PACK, TextureAtlas.class);
		assetManager.load(Constants.ASSET_FONT, BitmapFont.class);
		assetManager.load(Constants.ASSET_SOUND_MEOW, Sound.class);
		assetManager.finishLoading(); // Blocks until all resources are loaded into memory
		
		atlas = assetManager.get(Constants.ASSET_PACK);
		backgroundTexture = atlas.findRegion(Constants.ASSET_INTRO_IMAGE);
		wallTexture = atlas.findRegion(Constants.ASSET_WALL_IMAGE);
		font = assetManager.get(Constants.ASSET_FONT);
		meow = assetManager.get(Constants.ASSET_SOUND_MEOW);
		font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
		
		Array<AtlasRegion> bosses = new Array<AtlasRegion>();
		bosses.add(atlas.findRegion(Constants.ASSET_BOSS_IMAGE_1));
		bosses.add(atlas.findRegion(Constants.ASSET_BOSS_IMAGE_2));
		bosses.add(atlas.findRegion(Constants.ASSET_BOSS_IMAGE_3));
		bosses.add(atlas.findRegion(Constants.ASSET_BOSS_IMAGE_4));
		bossAnimation = new Animation(Constants.BOSS_ANIMATION_STEP, bosses, PlayMode.LOOP);
		
		Array<AtlasRegion> cats = new Array<AtlasRegion>();
		cats.add(atlas.findRegion(Constants.ASSET_AWAKE_CAT_IMAGE_1));
		cats.add(atlas.findRegion(Constants.ASSET_AWAKE_CAT_IMAGE_2));
		catAnimation = new Animation(Constants.CAT_ANIMATION_STEP, cats, PlayMode.LOOP);
		
		batch = new SpriteBatch();
		
		Gdx.input.setInputProcessor(this);

		// Create Physics World
		world = new World(new Vector2(0, -9.8f), true);
		world.setContactListener(this);
		
		makeCharacters();
		
	}
	
	
	private void makeCharacters() {
		
		// floor
		BodyDef floorBodyDef = new BodyDef();
		floorBodyDef.type = BodyType.StaticBody;
		floorBodyDef.position.set(Constants.SCENE_WIDTH*0.5f, Constants.BLOCK_HEIGHT*0.5f);

		PolygonShape floorShape = new PolygonShape();
		floorShape.setAsBox((Constants.SCENE_WIDTH)*0.5f, Constants.BLOCK_HEIGHT*0.5f);
				
		FixtureDef floorFixtureDef = new FixtureDef();
		floorFixtureDef.shape = floorShape;
		floorFixtureDef.friction = 0.0f;
		floorFixtureDef.restitution = 0f;
		
		floorBody = world.createBody(floorBodyDef);
		floorBody.createFixture(floorFixtureDef);
		floorBody.setUserData(new Vector2(Constants.SCENE_WIDTH, Constants.BLOCK_HEIGHT));
		floorShape.dispose();
		
		// Boss
		BodyDef bossBodyDef = new BodyDef();
		bossBodyDef.type = BodyType.DynamicBody;
		float bossYPos = Constants.BOSS_HEIGHT;
		bossBodyDef.position.set(Constants.SCENE_WIDTH*0.3f, bossYPos);
		
		PolygonShape bossShape = new PolygonShape();
		bossShape.setAsBox(Constants.BOSS_WIDTH*0.5f, Constants.BOSS_HEIGHT*0.5f);
		
		FixtureDef bossFixtureDef = new FixtureDef();
		bossFixtureDef.shape = bossShape;
		bossFixtureDef.friction = 0.5f;
		bossFixtureDef.restitution = 0.1f;
		
		boss = world.createBody(bossBodyDef);
		boss.createFixture(bossFixtureDef);
		boss.setUserData(new Vector2(Constants.BOSS_WIDTH, Constants.BOSS_HEIGHT));
		bossShape.dispose();
		
	}
	
	private void makeSeymour() {
		if(seymour==null) {
			BodyDef seymourBodyDef = new BodyDef();
			seymourBodyDef.type = BodyType.DynamicBody;
			float seymourYPos = Constants.SCENE_HEIGHT;
			seymourBodyDef.position.set(Constants.SCENE_WIDTH*0.7f, seymourYPos);
			
			PolygonShape seymourShape = new PolygonShape();
			seymourShape.setAsBox(Constants.CAT_WIDTH*0.5f, Constants.CAT_HEIGHT*0.5f);
			
			FixtureDef seymourFixtureDef = new FixtureDef();
			seymourFixtureDef.shape = seymourShape;
			seymourFixtureDef.friction = 0.5f;
			seymourFixtureDef.restitution = 0.1f;
			
			seymour = world.createBody(seymourBodyDef);
			seymour.createFixture(seymourFixtureDef);
			seymour.setUserData(new Vector2(Constants.CAT_WIDTH, Constants.CAT_HEIGHT));
			seymourShape.dispose();
			
			meow.play();
		}
	}
	

	@Override
	public void beginContact(Contact contact) {
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();
		if(seymour!=null) {
			if(bodyA.equals(seymour) && bodyB.equals(floorBody)) {
				animateSeymour = true;
				bodyA.applyLinearImpulse(1f, 0f, bodyA.getWorldCenter().x, bodyA.getWorldCenter().y, true);
			} else if(bodyB.equals(seymour) && bodyA.equals(floorBody)) {
				animateSeymour = true;
				bodyB.applyLinearImpulse(1f, 0f, bodyB.getWorldCenter().x, bodyB.getWorldCenter().y, true);
			} 
		}
	}

	@Override
	public void endContact(Contact contact) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
		
	}
	
	
	private void writeText(String text, float xPos, float yPos) {
		GlyphLayout gl = new GlyphLayout();
		gl.setText(font, text);
		font.draw(batch, gl, xPos, yPos);
	}
	
	
	private void renderBody(Body body, TextureRegion textureRegion) {
		if(body!=null) {
			float bodyWidth = ((Vector2)body.getUserData()).x;
			float bodyHeight = ((Vector2)body.getUserData()).y;
			float xPos = body.getPosition().x - bodyWidth*0.5f;
			float yPos = body.getPosition().y - bodyHeight*0.5f;
			batch.draw(textureRegion, 
					xPos, yPos,  
					bodyWidth*0.5f, bodyHeight*0.5f, 
					bodyWidth, bodyHeight,  
					1f, 1f, (float) Math.toDegrees(body.getAngle()));
		}
	}
	
	
	@Override
	public void render(float delta) {
		
		world.step(1 / 60f, 6, 2);
		
		timeSinceStart = timeSinceStart + delta;
		bossAnimationTime = bossAnimationTime + delta;
		catAnimationTime = catAnimationTime + delta;
		
		Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		float fontPositionY = Constants.VIRTUAL_HEIGHT * 0.65f;
		float fontPositionX = Constants.VIRTUAL_WIDTH * 0.3f;
		
		batch.setProjectionMatrix(fontViewport.getCamera().combined);
		batch.begin();
		batch.draw(backgroundTexture, 0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT);
		GlyphLayout gldws = new GlyphLayout();
		gldws.setText(font, "Don't wake SEYMOUR!");
		
		if(timeSinceStart>27f) {
			writeText(" tap to start ", fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>25f) {
			font.draw(batch, gldws, fontPositionX, fontPositionY);
			makeSeymour();
		} else if(timeSinceStart>24f) {
			writeText("Don't wake", fontPositionX, fontPositionY);
		} else if(timeSinceStart>23f) {
			writeText("Don't", fontPositionX, fontPositionY);
		} else if(timeSinceStart>21f) {
			writeText("my snoozing cat, Seymour!", fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>19f) {
			writeText("But don't you disturb...", fontPositionX, fontPositionY);
		} else if(timeSinceStart>17f) {
			writeText("ONE DOLLAR PER BRICK!", fontPositionX + 200f, fontPositionY-100f);
		}  else if(timeSinceStart>15f) {
			writeText("I will pay you...", fontPositionX, fontPositionY);
		}  else if(timeSinceStart>13f) {
			writeText("or you're FIRED!", fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>11f) {
			writeText("at least one brick every 3 seconds...", fontPositionX, fontPositionY);
		} else if(timeSinceStart>9f) {
			writeText("Do it quickly...", fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>7f) {
			writeText("you must knock down the wall", fontPositionX, fontPositionY);
		} else if(timeSinceStart>5f) {
			writeText("DO WHAT I SAY!", fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>3f) {
			writeText("PAY ATTENTION TO ME!", fontPositionX, fontPositionY);
		} else if(timeSinceStart>1f) {
			writeText("MYEH! I am the boss!", fontPositionX + 200f, fontPositionY-100f);
		}
		batch.end();
		
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();
		
		// render floor
		renderBody(floorBody, wallTexture);
		
		// render boss
		TextureRegion bossTexture = bossAnimation.getKeyFrame(bossAnimationTime);
		if(timeSinceStart>23f) {
			bossAnimation.setFrameDuration(Constants.BOSS_ANIMATION_STEP*2f);
		}
		if(timeSinceStart<1f || timeSinceStart>25f) {
			bossTexture = atlas.findRegion(Constants.ASSET_BOSS_IMAGE_1); // stop animating after 19 secs
		}
		renderBody(boss, bossTexture);
		
		// render seymour
		if(seymour!=null) {
			TextureRegion seymourTexture = atlas.findRegion(Constants.ASSET_AWAKE_CAT_IMAGE_1);
			if(animateSeymour) {
				seymourTexture = catAnimation.getKeyFrame(catAnimationTime);
			}
			renderBody(seymour, seymourTexture);
		}
		
		
		batch.end();
		
		if(Gdx.input.justTouched()) {
			game.showGameScreen();
		}
		
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		fontViewport.update(width, height);
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
		world.dispose();
		batch.dispose();
	}
}
