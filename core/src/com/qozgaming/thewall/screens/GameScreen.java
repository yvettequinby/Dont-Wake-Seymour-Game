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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.qozgaming.thewall.TheWallGame;
import com.qozgaming.thewall.utils.BodyRole;
import com.qozgaming.thewall.utils.Constants;
import com.qozgaming.thewall.utils.GeneralUtil;
import com.qozgaming.thewall.utils.UserData;

public class GameScreen extends InputAdapter implements Screen, ContactListener {
	
	private final TheWallGame game;
	
	private SpriteBatch batch;
	private TextureRegion backgroundTexture;
	private BitmapFont font;
	
	private AssetManager assetManager;
	private TextureAtlas atlas;
	private TextureRegion sleepingCatTexture;
	private TextureRegion wallTexture;
	private TextureRegion brickTexture;
	private Animation bossAnimation;
	private Animation catAnimation;
	private boolean animateSeymour;
	
	private Body cat;
	
	private boolean moneyShot = true;
	private Sound meow;
	private Sound money;
	private Sound knock;
	
	private boolean gameOverSequenceBegun = false;
	private boolean hasMeowed = false;
	private boolean wokeCat = false;
	private boolean start = false;
	private float countDownToStart = 3f;
	private float timePerBrick = 3f;
	private int bricksRemoved = 0;
	private float timeSinceLastBrick = 0f;
	private float timeSinceGameOver = 0f;
	private boolean isGameOver = false;
	
	private Viewport viewport;
	private OrthographicCamera fontCamera;
	private Viewport fontViewport;
	private Vector3 point = new Vector3();
	private World world;
	
	public GameScreen(final TheWallGame game) {
		
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
		assetManager.load(Constants.ASSET_SOUND_MONEY, Sound.class);
		assetManager.load(Constants.ASSET_SOUND_BRICK, Sound.class);
		assetManager.finishLoading(); // Blocks until all resources are loaded into memory
		
		atlas = assetManager.get(Constants.ASSET_PACK);
		backgroundTexture = atlas.findRegion(Constants.ASSET_INTRO_IMAGE);
		wallTexture = atlas.findRegion(Constants.ASSET_WALL_IMAGE);
		sleepingCatTexture = atlas.findRegion(Constants.ASSET_SLEEPING_CAT_IMAGE);
		brickTexture = atlas.findRegion(Constants.ASSET_BRICK_IMAGE);
		font = assetManager.get(Constants.ASSET_FONT);
		meow = assetManager.get(Constants.ASSET_SOUND_MEOW);
		money = assetManager.get(Constants.ASSET_SOUND_MONEY);
		knock = assetManager.get(Constants.ASSET_SOUND_BRICK);
		
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
		
		buildBlocks();
		
		BodyDef catBodyDef = new BodyDef();
		catBodyDef.type = BodyType.DynamicBody;
		catBodyDef.position.set(Constants.SCENE_WIDTH*0.5f, Constants.SCENE_HEIGHT);
		
		PolygonShape catShape = new PolygonShape();
		catShape.setAsBox(Constants.CAT_WIDTH*0.5f, Constants.CAT_HEIGHT*0.5f);
		
		FixtureDef catFixtureDef = new FixtureDef();
		catFixtureDef.shape = catShape;
		catFixtureDef.friction = 0.5f;
		catFixtureDef.restitution = 0.1f;
		
		cat = world.createBody(catBodyDef);
		cat.createFixture(catFixtureDef);
		cat.setUserData(new UserData(BodyRole.SEYMOUR, new Vector2(Constants.CAT_WIDTH, Constants.CAT_HEIGHT)));
		catShape.dispose();
				
	}
	
	
	private void buildBlocks() {
		
		//walls
		BodyDef edgeBodyDef = new BodyDef();
		edgeBodyDef.type = BodyType.StaticBody;
		edgeBodyDef.position.set(Constants.EDGE_WIDTH*0.5f, Constants.SCENE_HEIGHT*0.5f);

		PolygonShape edgeShape = new PolygonShape();
		edgeShape.setAsBox(Constants.EDGE_WIDTH*0.5f, Constants.SCENE_HEIGHT*0.5f);
				
		FixtureDef edgeFixtureDef = new FixtureDef();
		edgeFixtureDef.shape = edgeShape;
		edgeFixtureDef.friction = 0.5f;
		edgeFixtureDef.restitution = 0f;
		
		Body leftEdgeBody = world.createBody(edgeBodyDef);
		leftEdgeBody.createFixture(edgeFixtureDef);
		leftEdgeBody.setUserData(new UserData(BodyRole.WALL, new Vector2(Constants.EDGE_WIDTH, Constants.SCENE_HEIGHT)));
		
		edgeBodyDef.position.set(Constants.SCENE_WIDTH-Constants.EDGE_WIDTH*0.5f, Constants.SCENE_HEIGHT*0.5f);
		Body rightEdgeBody = world.createBody(edgeBodyDef);
		rightEdgeBody.createFixture(edgeFixtureDef);
		rightEdgeBody.setUserData(new UserData(BodyRole.WALL, new Vector2(Constants.EDGE_WIDTH, Constants.SCENE_HEIGHT)));
		edgeShape.dispose();
		
		// left ledges
		float leftLedgeXpos = Constants.EDGE_WIDTH+Constants.LEDGE_WIDTH*0.5f;
		for(float i=2f; i<=16f; i=i+2f) {
			float ypos = Constants.SCENE_HEIGHT-Constants.BLOCK_HEIGHT*i+Constants.BLOCK_HEIGHT*0.5f;
			makeLedge(leftLedgeXpos, ypos);
		}
		
		// right ledges
		float rightLedgeXpos = Constants.SCENE_WIDTH-(Constants.EDGE_WIDTH+Constants.LEDGE_WIDTH*0.5f);
		for(float i=2f; i<=16f; i=i+2f) {
			float ypos = Constants.SCENE_HEIGHT-Constants.BLOCK_HEIGHT*i+Constants.BLOCK_HEIGHT*0.5f;
			makeLedge(rightLedgeXpos, ypos);
		}
		
		// floor
		BodyDef floorBodyDef = new BodyDef();
		floorBodyDef.type = BodyType.StaticBody;
		floorBodyDef.position.set(Constants.SCENE_WIDTH*0.5f, Constants.BLOCK_HEIGHT*0.5f);

		PolygonShape floorShape = new PolygonShape();
		floorShape.setAsBox((Constants.SCENE_WIDTH-Constants.EDGE_WIDTH*2f)*0.5f, (Constants.BLOCK_HEIGHT-Constants.BLOCK_Y_SPACER)*0.5f);
				
		FixtureDef floorFixtureDef = new FixtureDef();
		floorFixtureDef.shape = floorShape;
		floorFixtureDef.friction = 0.5f;
		floorFixtureDef.restitution = 0f;
		
		Body floorBody = world.createBody(floorBodyDef);
		floorBody.createFixture(floorFixtureDef);
		floorBody.setUserData(new UserData(BodyRole.FLOOR, new Vector2(Constants.SCENE_WIDTH, Constants.BLOCK_HEIGHT)));
		floorShape.dispose();
		
		// blocks
		boolean isLedge = false;
		for(float i=1f; i<=16f; i=i+1f) {
			float availableSpace = Constants.SCENE_WIDTH - Constants.EDGE_WIDTH*2f - (isLedge ? Constants.LEDGE_WIDTH*2f : 0f);
			float leftEdge = Constants.EDGE_WIDTH + (isLedge ? + Constants.LEDGE_WIDTH : 0f) + Constants.BLOCK_X_SPACER;
			float ypos = i*Constants.BLOCK_HEIGHT + Constants.BLOCK_HEIGHT*0.5f;
			buildBlockRow(availableSpace, leftEdge, ypos, false);
			isLedge = !isLedge;
		}
		
	}
	
	
	private boolean isCatAwake() {
		boolean result = false;
		if(cat.getPosition().y<Constants.SCENE_HEIGHT-Constants.BLOCK_HEIGHT*1.5f) {
			result = true;
			if(!hasMeowed) {
				meow.play();
				hasMeowed = true;
			}
		}
		return result;
	}
	
	
	private void gameOverSequence() {
		if(!gameOverSequenceBegun) {
			
			gameOverSequenceBegun = true;
			
			// Drop in the boss
			BodyDef bossBodyDef = new BodyDef();
			bossBodyDef.type = BodyType.DynamicBody;
			bossBodyDef.position.set(Constants.SCENE_WIDTH*0.3f, Constants.SCENE_HEIGHT+Constants.BOSS_HEIGHT);
			
			PolygonShape bossShape = new PolygonShape();
			bossShape.setAsBox(Constants.BOSS_WIDTH*0.5f, Constants.BOSS_HEIGHT*0.5f);
			
			FixtureDef bossFixtureDef = new FixtureDef();
			bossFixtureDef.shape = bossShape;
			bossFixtureDef.friction = 1f;
			bossFixtureDef.restitution = 0.1f;
			
			Body boss = world.createBody(bossBodyDef);
			boss.createFixture(bossFixtureDef);
			boss.setUserData(new UserData(BodyRole.BOSS, new Vector2(Constants.BOSS_WIDTH, Constants.BOSS_HEIGHT)));
			bossShape.dispose();
			
			// destroy the walls
			Array<Body> worldBodies = new Array<Body>();
			world.getBodies(worldBodies);
			for(Body body : worldBodies) {
				UserData ud = GeneralUtil.retrieveUserData(body);
				if(BodyRole.WALL.equals(ud.getBodyRole())) {
					world.destroyBody(body);
				}
			}
			
			// destroy up to 5 bricks immediately
			destroyBricks(5);
			
			// ten time, destroy bricks in groups of up to 5
			for(float i=0.25f; i<2.75f; i = i + 0.25f) {
				Timer.schedule(new Task(){
					@Override
					public void run() {
						destroyBricks(5);
					}
				}, i);
			}
			
			// Destroy the last of the bricks
			Timer.schedule(new Task(){
				@Override
				public void run() {
					destroyBricks(-1);
				}
			}, 2.75f);
			
		}
	}
	
	
	private void destroyBricks(int max) {
		Array<Body> worldBodies = new Array<Body>();
		world.getBodies(worldBodies);
		int destroyed = 0;
		for(Body body : worldBodies) {
			UserData ud = GeneralUtil.retrieveUserData(body);
			if(BodyRole.BRICK.equals(ud.getBodyRole())) {
				world.destroyBody(body);
				destroyed++;
				if(max>0 && destroyed>=max) {
					break;
				}
			}
		}
		if(destroyed>0) {
			long soundId = knock.play();
			knock.setVolume(soundId, 0.2f);
		}
	}
	
	
	private void buildBlockRow(float availableSpace, float leftEdge, float ypos, boolean staticBody) {
		
		while(availableSpace>=Constants.MIN_BLOCK_WIDTH) {
			
			float maxWidth = Math.min(availableSpace, Constants.MAX_BLOCK_WIDTH);
			float minWidth = Math.min(availableSpace, Constants.MIN_BLOCK_WIDTH);
			float width = MathUtils.random(minWidth, maxWidth);
			
			float nextAvailableSpace = availableSpace - width - Constants.BLOCK_X_SPACER;
			if(nextAvailableSpace < Constants.MIN_BLOCK_WIDTH) {
				float combined = nextAvailableSpace + width - Constants.BLOCK_X_SPACER;
				if(combined <= maxWidth) {
					width = combined;
				} else {
					width = minWidth;
				}
			}
			
			BodyDef bodyDef = new BodyDef();
			bodyDef.type = staticBody ? BodyType.StaticBody: BodyType.DynamicBody;
			float xpos = leftEdge + width*0.5f;
			bodyDef.position.set(xpos, ypos);

			PolygonShape shape = new PolygonShape();
			float height = Constants.BLOCK_HEIGHT - Constants.BLOCK_Y_SPACER;
			shape.setAsBox(width*0.5f, height*0.5f);
					
			FixtureDef fixtureDef = new FixtureDef();
			fixtureDef.shape = shape;
			fixtureDef.density = 10f;
			fixtureDef.friction = 0.2f;
			fixtureDef.restitution = 0.1f;
			
			Body blockBody = world.createBody(bodyDef);
			blockBody.createFixture(fixtureDef);
			blockBody.setUserData(new UserData(BodyRole.BRICK, new Vector2(width, height)));
			shape.dispose();
			
			availableSpace = availableSpace - width - Constants.BLOCK_X_SPACER;
			leftEdge = leftEdge + width + Constants.BLOCK_X_SPACER;
		}
	}
	
	
	private void makeLedge(float xpos, float ypos) {
		
		BodyDef ledgeBodyDef = new BodyDef();
		ledgeBodyDef.type = BodyType.StaticBody;
		ledgeBodyDef.position.set(xpos, ypos);
		
		PolygonShape ledgeShape = new PolygonShape();
		ledgeShape.setAsBox(Constants.LEDGE_WIDTH*0.5f, Constants.BLOCK_HEIGHT*0.5f);
		
		FixtureDef ledgeFixtureDef = new FixtureDef();
		ledgeFixtureDef.shape = ledgeShape;
		ledgeFixtureDef.friction = 0.5f;
		ledgeFixtureDef.restitution = 0f;
		
		Body ledgeBody = world.createBody(ledgeBodyDef);
		ledgeBody.createFixture(ledgeFixtureDef);
		ledgeBody.setUserData(new UserData(BodyRole.WALL, new Vector2(Constants.LEDGE_WIDTH, Constants.BLOCK_HEIGHT)));
		ledgeShape.dispose();
		
	}
	
	
	QueryCallback brickTouchCallback = new QueryCallback() {
		@Override
		public boolean reportFixture (Fixture fixture) {
			boolean result = true;
			if(fixture.getBody()!=null) {
				Body body = fixture.getBody();
				BodyRole bodyRole = GeneralUtil.retrieveUserData(body).getBodyRole();
				if(BodyRole.BRICK.equals(bodyRole) && fixture.testPoint(point.x, point.y)) {
					
					// destroy brick
					world.destroyBody(body);
					knock.play();
					Timer.schedule(new Task(){
						@Override
						public void run(){
							money.play();
							bricksRemoved++;
						}
					}, 0.5f);
					timeSinceLastBrick = 0f;
					result = false;
					
				}
			}
			return result;
		}
	};
	
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		
		if(start && !isGameOver) {
			viewport.unproject(point.set(screenX, screenY, 0)); // important to use viewport, not camera
			world.QueryAABB(brickTouchCallback, point.x - 0.0001f, point.y - 0.0001f, point.x + 0.0001f, point.y + 0.0001f);
		}
		return false;
		
	}
	
	
	@Override
	public void beginContact(Contact contact) {
		final Body bodyA = contact.getFixtureA().getBody();
		BodyRole roleA = GeneralUtil.retrieveUserData(bodyA).getBodyRole();
		final Body bodyB = contact.getFixtureB().getBody();
		BodyRole roleB = GeneralUtil.retrieveUserData(bodyB).getBodyRole();
		if(BodyRole.SEYMOUR.equals(roleA) && BodyRole.FLOOR.equals(roleB)) {
			animateSeymour = true;
			meow.play();
			Gdx.app.postRunnable(new Runnable() {
				@Override
                public void run () {
					bodyA.setTransform(bodyA.getPosition().x, bodyA.getPosition().y, new Float(Math.toRadians(0d)));
					bodyA.setLinearVelocity(5f, 0f);
                }
			});
		} else if(BodyRole.SEYMOUR.equals(roleB) && BodyRole.FLOOR.equals(roleA)) {
			animateSeymour = true;
			meow.play();
			Gdx.app.postRunnable(new Runnable() {
				@Override
                public void run () {
					bodyB.setTransform(bodyB.getPosition().x, bodyB.getPosition().y, new Float(Math.toRadians(0d)));
					bodyB.setLinearVelocity(5f, 0f);
				}
			});
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
	
	
	private void writeText(String text, float yPos) {
		GlyphLayout gl = new GlyphLayout();
		gl.setText(font, text);
		font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, yPos);
	}
	
	
	private void renderBody(Body body, TextureRegion textureRegion) {
		renderBody(body, textureRegion, 0.5f);
	}
	
	
	private void renderBody(Body body, TextureRegion textureRegion, float yPosMult) {
		if(body!=null) {
			UserData userData = GeneralUtil.retrieveUserData(body);
			float bodyWidth = userData.getDimensions().x;
			float bodyHeight = userData.getDimensions().y;
			float xPos = body.getPosition().x - bodyWidth*0.5f;
			float yPos = body.getPosition().y - bodyHeight*yPosMult;
			batch.draw(textureRegion, 
					xPos, yPos,  
					bodyWidth*0.5f, bodyHeight*0.5f, 
					bodyWidth, bodyHeight,  
					1f, 1f, (float) Math.toDegrees(body.getAngle()));
		}
	}
	

	@Override
	public void render(float delta) {
		
		Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		world.step(1 / 60f, 6, 2);
		
		if(isGameOver) { 
			timeSinceGameOver = timeSinceGameOver + delta;
			gameOverSequence();
		}
		
		if(!isGameOver && start) { // if game is running, calculate and apply brick count-down
			timeSinceLastBrick = timeSinceLastBrick + delta;
			if(timeSinceLastBrick>=timePerBrick) {
				isGameOver = true;
			}
			if(isCatAwake()) {
				isGameOver = true;
				wokeCat = true;
			}
		}
		
		float countDown = timePerBrick-timeSinceLastBrick;
		
		// background
		batch.setProjectionMatrix(fontViewport.getCamera().combined);
		batch.begin();
		batch.draw(backgroundTexture, 0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT);
		batch.end();
		
		if(start) {
			
			// walls, ledges, floor and bricks
			batch.setProjectionMatrix(viewport.getCamera().combined);
			batch.begin();
			
			Array<Body> worldBodies = new Array<Body>();
			world.getBodies(worldBodies);
			for(Body body : worldBodies) {
				UserData ud = GeneralUtil.retrieveUserData(body);
				if(BodyRole.WALL.equals(ud.getBodyRole())) {
					renderBody(body, wallTexture);
				} else if(BodyRole.FLOOR.equals(ud.getBodyRole())) {
					renderBody(body, wallTexture);
				} else if(BodyRole.BRICK.equals(ud.getBodyRole())) {
					renderBody(body, brickTexture);
				} else if(BodyRole.SEYMOUR.equals(ud.getBodyRole())) {
					if(isCatAwake()) {
						TextureRegion awakeCatTexture = atlas.findRegion(Constants.ASSET_AWAKE_CAT_IMAGE_1);
						if(animateSeymour) {
							awakeCatTexture = catAnimation.getKeyFrame(timeSinceGameOver);
							body.setLinearVelocity(5f, 0f);
						}
						renderBody(body, awakeCatTexture);
					} else {
						renderBody(body, sleepingCatTexture, 0.6f);
					}
				} else if(BodyRole.BOSS.equals(ud.getBodyRole())) {
					TextureRegion bossTexture = bossAnimation.getKeyFrame(timeSinceGameOver);
					renderBody(body, bossTexture);
				}
			}
			
			batch.end();
			
			// fonts
			batch.setProjectionMatrix(fontViewport.getCamera().combined);
			batch.begin();
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			String score = Integer.toString(Math.round(countDown * 10f)) + " / " + bricksRemoved;
			font.draw(batch, score, 60f, Constants.VIRTUAL_HEIGHT - 25f);
			if(isGameOver) {
				float gameOverTextY = Constants.VIRTUAL_HEIGHT * 0.65f;
				if(timeSinceGameOver>7f) {
					writeText("BUT YOU EARNED... " + bricksRemoved + " DOLLARS", gameOverTextY);
					if(moneyShot) {
						money.play();
						moneyShot = false;
					} 
				} else if(timeSinceGameOver>5f) {
					writeText("YOU'RE FIRED!", gameOverTextY);
				} else if(timeSinceGameOver>3f) {
					if(wokeCat) {
						writeText("YOU WOKE SEYMOUR!", gameOverTextY);
					} else {
						writeText("YOU ARE TOO SLOW!", gameOverTextY);
					}
				} else if(timeSinceGameOver>0f) {
					writeText("GAME OVER", gameOverTextY);
				}
			}
			batch.end();
			
			if(isGameOver) {
				if(timeSinceGameOver>10f) {
					game.showSplashScreen();
				}
			}
			
		} else {
			
			// show the get ready text and don't render the bricks
			batch.setProjectionMatrix(fontViewport.getCamera().combined);
			batch.begin();
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			writeText("GET READY TO START", Constants.VIRTUAL_HEIGHT * 0.5f);
			writeText(Integer.toString(Math.round(countDownToStart)), Constants.VIRTUAL_HEIGHT * 0.4f);
			batch.end();
			countDownToStart = countDownToStart - delta;
			if(countDownToStart<=0) {
				start = true;
			}
			
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
