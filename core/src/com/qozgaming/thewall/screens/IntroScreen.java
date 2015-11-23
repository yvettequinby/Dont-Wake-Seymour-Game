package com.qozgaming.thewall.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
	private Texture backgroundTexture;
	private BitmapFont font;
	private TextureAtlas atlas;
	private TextureRegion wallTexture;
	private Animation bossAnimation;
	private Animation catAnimation;
	private float bossAnimationTime = 0f;
	private float catAnimationTime = 0f;
	private Sound meow;
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
		
		backgroundTexture = new Texture(Gdx.files.internal(Constants.ASSET_INTRO_IMAGE));
		font = new BitmapFont(Gdx.files.internal(Constants.ASSET_FONT));
		atlas = new TextureAtlas(Gdx.files.internal(Constants.ASSET_PACK));
		wallTexture = atlas.findRegion(Constants.ASSET_WALL_IMAGE);
		meow = Gdx.audio.newSound(Gdx.files.internal(Constants.ASSET_SOUND_MEOW));
		batch = new SpriteBatch();
		
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
	
	@Override
	public void render(float delta) {
		
		timeSinceStart = timeSinceStart + delta;
		bossAnimationTime = bossAnimationTime + delta;
		catAnimationTime = catAnimationTime + delta;
		
		Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		world.step(1 / 60f, 6, 2);
		
		float fontPositionY = Constants.VIRTUAL_HEIGHT * 0.65f;
		float fontPositionX = Constants.VIRTUAL_WIDTH * 0.3f;
		
		batch.setProjectionMatrix(fontViewport.getCamera().combined);
		batch.begin();
		batch.draw(backgroundTexture, 0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT);
		GlyphLayout gldws = new GlyphLayout();
		gldws.setText(font, "Don't wake SEYMOUR!");
		
		if(timeSinceStart>27f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, " tap to start ");
			font.draw(batch, gl, fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>25f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			font.draw(batch, gldws, fontPositionX, fontPositionY);
			makeSeymour();
		} else if(timeSinceStart>24f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "Don't wake ");
			font.draw(batch, gl, fontPositionX, fontPositionY);
		} else if(timeSinceStart>23f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "Don't");
			font.draw(batch, gl, fontPositionX, fontPositionY);
		} else if(timeSinceStart>21f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "my snoozing cat, Seymour!");
			font.draw(batch, gl, fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>19f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "But don't you disturb...");
			font.draw(batch, gl, fontPositionX, fontPositionY);
		} else if(timeSinceStart>17f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "ONE DOLLAR PER BRICK!");
			font.draw(batch, gl, fontPositionX + 200f, fontPositionY-100f);
		}  else if(timeSinceStart>15f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "I will pay you...");
			font.draw(batch, gl, fontPositionX, fontPositionY);
		}  else if(timeSinceStart>13f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "or you're FIRED!");
			font.draw(batch, gl, fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>11f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "at least one brick every 3 seconds...");
			font.draw(batch, gl, fontPositionX, fontPositionY);
		} else if(timeSinceStart>9f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "Do it quickly...");
			font.draw(batch, gl, fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>7f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "you must knock down the wall");
			font.draw(batch, gl, fontPositionX, fontPositionY);
		} else if(timeSinceStart>5f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "DO WHAT I SAY!");
			font.draw(batch, gl, fontPositionX + 200f, fontPositionY-100f);
		} else if(timeSinceStart>3f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "PAY ATTENTION TO ME!");
			font.draw(batch, gl, fontPositionX, fontPositionY);
		} else if(timeSinceStart>1f) {
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl = new GlyphLayout();
			gl.setText(font, "MYEH! I am the boss!");
			font.draw(batch, gl, fontPositionX + 200f, fontPositionY-100f);
		}
		batch.end();
		
		batch.setProjectionMatrix(viewport.getCamera().combined);
		batch.begin();
		
		// render floor
		float bodyWidth = ((Vector2)floorBody.getUserData()).x;
		float bodyHeight = ((Vector2)floorBody.getUserData()).y;
		float xPos = floorBody.getPosition().x - bodyWidth*0.5f;
		float yPos = floorBody.getPosition().y - bodyHeight*0.5f;
		batch.draw(wallTexture, 
				xPos, yPos,  
				bodyWidth*0.5f, bodyHeight*0.5f, 
				bodyWidth, bodyHeight,  
				1f, 1f, (float) Math.toDegrees(floorBody.getAngle()));
		
		// render boss
		TextureRegion bossTexture = bossAnimation.getKeyFrame(bossAnimationTime);
		if(timeSinceStart>23f) {
			bossAnimation.setFrameDuration(Constants.BOSS_ANIMATION_STEP*2f);
		}
		if(timeSinceStart<1f || timeSinceStart>25f) {
			bossTexture = atlas.findRegion(Constants.ASSET_BOSS_IMAGE_1); // stop animating after 19 secs
		}
		bodyWidth = ((Vector2)boss.getUserData()).x;
		bodyHeight = ((Vector2)boss.getUserData()).y;
		xPos = boss.getPosition().x - bodyWidth*0.5f;
		yPos = boss.getPosition().y - bodyHeight*0.5f;
		batch.draw(bossTexture, 
				xPos, yPos,  
				bodyWidth*0.5f, bodyHeight*0.5f, 
				bodyWidth, bodyHeight,  
				1f, 1f, (float) Math.toDegrees(boss.getAngle()));
		
		// render seymour
		if(seymour!=null) {
			TextureRegion seymourTexture = atlas.findRegion(Constants.ASSET_AWAKE_CAT_IMAGE_1);
			if(animateSeymour) {
				seymourTexture = catAnimation.getKeyFrame(catAnimationTime);
			}
			bodyWidth = ((Vector2)seymour.getUserData()).x;
			bodyHeight = ((Vector2)seymour.getUserData()).y;
			xPos = seymour.getPosition().x - bodyWidth*0.5f;
			yPos = seymour.getPosition().y - bodyHeight*0.5f;
			batch.draw(seymourTexture, 
					xPos, yPos,  
					bodyWidth*0.5f, bodyHeight*0.5f, 
					bodyWidth, bodyHeight,  
					1f, 1f, (float) Math.toDegrees(seymour.getAngle()));
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
		meow.dispose();
		atlas.dispose();
		font.dispose();
		backgroundTexture.dispose();
		world.dispose();
	}
}
