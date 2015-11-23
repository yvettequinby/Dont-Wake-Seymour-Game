package com.qozgaming.thewall.screens;

import java.util.ArrayList;
import java.util.List;

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
import com.qozgaming.thewall.utils.Constants;

public class GameScreen extends InputAdapter implements Screen, ContactListener {
	
	private final TheWallGame game;
	
	private SpriteBatch batch;
	private Texture backgroundTexture;
	private BitmapFont font;
	
	private TextureAtlas atlas;
	private TextureRegion sleepingCatTexture;
	private TextureRegion wallTexture;
	private TextureRegion brickTexture;
	private Animation bossAnimation;
	private Animation catAnimation;
	private boolean animateSeymour;
	
	private boolean moneyShot = true;
	private Sound meow;
	private Sound money;
	private Sound knock;
		
	private Body cat;
	private Body floorBody;
	private Body boss;
	private List<Body> wallBodies = new ArrayList<Body>();
	private List<Body> brickBodies = new ArrayList<Body>();
	
	private boolean gameOverSequenceBegun = false;
	private boolean hasMeowed = false;
	private boolean wokeCat = false;
	private boolean start = false;
	private float countDownToStart = 3f;
	private float timePerBrick = 3f;
	private int bricksRemoved = 0;
	private float timeSinceLastBrick = 0f;
	private boolean isGameOver = false;
	private float timeSinceBossKilledBricks = 0f;
	
	private Viewport viewport;
	private OrthographicCamera fontCamera;
	private Viewport fontViewport;
	private Vector3 point = new Vector3();
	private World world;
	private Body hitBody = null;
	
	
	public GameScreen(final TheWallGame game) {
		
		this.game = game;
		
		viewport = new FitViewport(Constants.SCENE_WIDTH, Constants.SCENE_HEIGHT);
		viewport.getCamera().position.set(viewport.getCamera().position.x + Constants.SCENE_WIDTH * 0.5f, viewport.getCamera().position.y + Constants.SCENE_HEIGHT * 0.5f, 0);
		viewport.getCamera().update();
		
		fontCamera = new OrthographicCamera();
		fontCamera.position.set(Constants.VIRTUAL_WIDTH * 0.5f, Constants.VIRTUAL_HEIGHT * 0.5f, 0.0f);
		fontViewport = new FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT, fontCamera);
		
		backgroundTexture = new Texture(Gdx.files.internal(Constants.ASSET_BACKGROUND_IMAGE));
		font = new BitmapFont(Gdx.files.internal(Constants.ASSET_FONT));
		
		atlas = new TextureAtlas(Gdx.files.internal(Constants.ASSET_PACK));
		sleepingCatTexture = atlas.findRegion(Constants.ASSET_SLEEPING_CAT_IMAGE);
		wallTexture = atlas.findRegion(Constants.ASSET_WALL_IMAGE);
		brickTexture = atlas.findRegion(Constants.ASSET_BRICK_IMAGE);
		
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
		
		meow = Gdx.audio.newSound(Gdx.files.internal(Constants.ASSET_SOUND_MEOW));
		money = Gdx.audio.newSound(Gdx.files.internal(Constants.ASSET_SOUND_MONEY));
		knock = Gdx.audio.newSound(Gdx.files.internal(Constants.ASSET_SOUND_BRICK));
		
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
		//catFixtureDef.density = 1f;
		catFixtureDef.friction = 0.5f;
		catFixtureDef.restitution = 0.1f;
		
		cat = world.createBody(catBodyDef);
		cat.createFixture(catFixtureDef);
		cat.setUserData(new Vector2(Constants.CAT_WIDTH, Constants.CAT_HEIGHT));
				
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
		leftEdgeBody.setUserData(new Vector2(Constants.EDGE_WIDTH, Constants.SCENE_HEIGHT));
		wallBodies.add(leftEdgeBody);
		
		edgeBodyDef.position.set(Constants.SCENE_WIDTH-Constants.EDGE_WIDTH*0.5f, Constants.SCENE_HEIGHT*0.5f);
		Body rightEdgeBody = world.createBody(edgeBodyDef);
		rightEdgeBody.createFixture(edgeFixtureDef);
		rightEdgeBody.setUserData(new Vector2(Constants.EDGE_WIDTH, Constants.SCENE_HEIGHT));
		wallBodies.add(rightEdgeBody);		
		
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
		
		floorBody = world.createBody(floorBodyDef);
		floorBody.createFixture(floorFixtureDef);
		floorBody.setUserData(new Vector2(Constants.SCENE_WIDTH, Constants.BLOCK_HEIGHT));
		//staticBodies.add(floorBody);
		
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
			
			BodyDef bossBodyDef = new BodyDef();
			bossBodyDef.type = BodyType.DynamicBody;
			bossBodyDef.position.set(Constants.SCENE_WIDTH*0.3f, Constants.SCENE_HEIGHT+Constants.BOSS_HEIGHT);
			
			PolygonShape bossShape = new PolygonShape();
			bossShape.setAsBox(Constants.BOSS_WIDTH*0.5f, Constants.BOSS_HEIGHT*0.5f);
			
			FixtureDef bossFixtureDef = new FixtureDef();
			bossFixtureDef.shape = bossShape;
			bossFixtureDef.friction = 1f;
			bossFixtureDef.restitution = 0.1f;
			
			boss = world.createBody(bossBodyDef);
			boss.createFixture(bossFixtureDef);
			boss.setUserData(new Vector2(Constants.BOSS_WIDTH, Constants.BOSS_HEIGHT));
			
			// make a copy of the brick list
			List<Body> bricks = new ArrayList<Body>();
			for(Body body : brickBodies) {
				bricks.add(body);
			}
			
			Float b = 0f;
			for(final Body brick : bricks) {
				Timer.schedule(new Task(){
					@Override
					public void run(){
						world.destroyBody(brick);
						long soundId = knock.play();
						knock.setVolume(soundId, 0.2f);
						brickBodies.remove(brick);
					}
				}, 0.2f*b.intValue());
				b = b + 0.25f; // destroy four bricks at a time
			}
			
			// destroy all the walls in one go
			final List<Body> walls = new ArrayList<Body>();
			for(Body body : wallBodies) {
				walls.add(body);
			}
			Timer.schedule(new Task(){
				@Override
				public void run(){
					for(final Body wall : walls) {
						world.destroyBody(wall);
						wallBodies.remove(wall);
					}
				}
			}, 0.2f*b.intValue());
			
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
			blockBody.setUserData(new Vector2(width, height));
			brickBodies.add(blockBody);
			
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
		ledgeBody.setUserData(new Vector2(Constants.LEDGE_WIDTH, Constants.BLOCK_HEIGHT));
		wallBodies.add(ledgeBody);
		
		ledgeShape.dispose();
		
	}
	
	
	QueryCallback callback = new QueryCallback() {
		@Override
		public boolean reportFixture (Fixture fixture) {
			if (!cat.equals(fixture.getBody()) && fixture.getBody().getType().equals(BodyType.DynamicBody) && fixture.testPoint(point.x, point.y)) {
				hitBody = fixture.getBody();
				return false;
			} else
				return true;
		}
	};
	
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		
		if(start && !isGameOver) {
			
			viewport.unproject(point.set(screenX, screenY, 0)); // important to use viewport, not camera
			
			hitBody = null;
			world.QueryAABB(callback, point.x - 0.0001f, point.y - 0.0001f, point.x + 0.0001f, point.y + 0.0001f);
			if (hitBody != null) {
				world.destroyBody(hitBody);
				brickBodies.remove(hitBody);
				knock.play();
				Timer.schedule(new Task(){
					@Override
					public void run(){
						money.play();
						bricksRemoved++;
					}
				}, 0.5f);
				timeSinceLastBrick = 0f;
			}
			
		}
		
		return false;
		
	}
	
	@Override
	public void beginContact(Contact contact) {
		final Body bodyA = contact.getFixtureA().getBody();
		final Body bodyB = contact.getFixtureB().getBody();
		if(bodyA.equals(cat) && bodyB.equals(floorBody)) {
			animateSeymour = true;
			meow.play();
			Gdx.app.postRunnable(new Runnable() {
				@Override
                public void run () {
					bodyA.setTransform(bodyA.getPosition().x, bodyA.getPosition().y, new Float(Math.toRadians(0d)));
					bodyA.setLinearVelocity(5f, 0f);
                }
			});
		} else if(bodyB.equals(cat) && bodyA.equals(floorBody)) {
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

	@Override
	public void render(float delta) {
		
		Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		world.step(1 / 60f, 6, 2);
		
		if(isGameOver) { // pull out floor when game is over
			if(brickBodies.isEmpty())  {
				timeSinceBossKilledBricks = timeSinceBossKilledBricks + delta;
			}
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
			for (Body body : wallBodies) {
				float bodyWidth = ((Vector2)body.getUserData()).x;
				float bodyHeight = ((Vector2)body.getUserData()).y;
				float xPos = body.getPosition().x - bodyWidth*0.5f;
				float yPos = body.getPosition().y - bodyHeight*0.5f;
				batch.draw(wallTexture, 
						xPos, yPos,  
						bodyWidth*0.5f, bodyHeight*0.5f, 
						bodyWidth, bodyHeight,  
						1f, 1f, (float) Math.toDegrees(body.getAngle()));
			}
			float bodyWidth = ((Vector2)floorBody.getUserData()).x;
			float bodyHeight = ((Vector2)floorBody.getUserData()).y;
			float xPos = floorBody.getPosition().x - bodyWidth*0.5f;
			float yPos = floorBody.getPosition().y - bodyHeight*0.5f;
			batch.draw(wallTexture, 
					xPos, yPos,  
					bodyWidth*0.5f, bodyHeight*0.5f, 
					bodyWidth, bodyHeight,  
					1f, 1f, (float) Math.toDegrees(floorBody.getAngle()));
			for (Body body : brickBodies) {
				bodyWidth = ((Vector2)body.getUserData()).x;
				bodyHeight = ((Vector2)body.getUserData()).y;
				xPos = body.getPosition().x - bodyWidth*0.5f;
				yPos = body.getPosition().y - bodyHeight*0.5f;
				batch.draw(brickTexture, 
						xPos, yPos,  
						bodyWidth*0.5f, bodyHeight*0.5f, 
						bodyWidth, bodyHeight,  
						1f, 1f, (float) Math.toDegrees(body.getAngle()));
			}
			
			// render seymour
			bodyWidth = ((Vector2)cat.getUserData()).x;
			bodyHeight = ((Vector2)cat.getUserData()).y;
			xPos = cat.getPosition().x - bodyWidth*0.5f;
			if(isCatAwake()) {
				yPos = cat.getPosition().y - bodyHeight*0.5f;
				TextureRegion awakeCatTexture = atlas.findRegion(Constants.ASSET_AWAKE_CAT_IMAGE_1);
				if(animateSeymour) {
					awakeCatTexture = catAnimation.getKeyFrame(timeSinceBossKilledBricks);
					cat.setLinearVelocity(5f, 0f);
				}
				batch.draw(awakeCatTexture, 
						xPos, yPos,  
						bodyWidth*0.5f, bodyHeight*0.5f, 
						bodyWidth, bodyHeight,  
						1f, 1f, (float) Math.toDegrees(cat.getAngle()));
			} else {
				yPos = cat.getPosition().y - bodyHeight*0.6f;
				batch.draw(sleepingCatTexture, 
						xPos, yPos,  
						bodyWidth*0.5f, bodyHeight*0.5f, 
						bodyWidth, bodyHeight,  
						1f, 1f, (float) Math.toDegrees(cat.getAngle()));
			}
			
			if(boss!=null) {
				// render boss
				TextureRegion bossTexture = bossAnimation.getKeyFrame(timeSinceBossKilledBricks);
				bodyWidth = ((Vector2)boss.getUserData()).x;
				bodyHeight = ((Vector2)boss.getUserData()).y;
				float bossXPos = boss.getPosition().x - bodyWidth*0.5f;
				float bossYPos = boss.getPosition().y - bodyHeight*0.5f;
				batch.draw(bossTexture, 
						bossXPos, bossYPos,  
						bodyWidth*0.5f, bodyHeight*0.5f, 
						bodyWidth, bodyHeight,  
						1f, 1f, (float) Math.toDegrees(boss.getAngle()));
			}
			batch.end();
			
			// fonts
			batch.setProjectionMatrix(fontViewport.getCamera().combined);
			batch.begin();
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 0.7f);
			String score = Integer.toString(Math.round(countDown * 10f)) + " / " + bricksRemoved;
			font.draw(batch, score, 60f, Constants.VIRTUAL_HEIGHT - 25f);
			if(isGameOver) {
				if(brickBodies.isEmpty()) {
					if(timeSinceBossKilledBricks>6f) {
						font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
						GlyphLayout gl = new GlyphLayout();
						gl.setText(font, "BUT YOU EARNED... " + bricksRemoved + " DOLLARS");
						font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.7f);
						if(moneyShot) {
							money.play();
							moneyShot = false;
						} 
					} else if(timeSinceBossKilledBricks>4f) {
						font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
						GlyphLayout gl = new GlyphLayout();
						gl.setText(font, "YOU'RE FIRED!");
						font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.7f);
					} else if(timeSinceBossKilledBricks>2f) {
						font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
						GlyphLayout gl = new GlyphLayout();
						if(wokeCat) {
							gl.setText(font, "YOU WOKE SEYMOUR!");
						} else {
							gl.setText(font, "YOU ARE TOO SLOW!");
						}
						font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.7f);
					} else if(timeSinceBossKilledBricks>0f) {
						font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
						GlyphLayout gl = new GlyphLayout();
						gl.setText(font, "GAME OVER");
						font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.7f);
					}
				}
			}
			batch.end();
			
			if(isGameOver) {
				if(timeSinceBossKilledBricks>10f) {
					game.showSplashScreen();
				}
			}
			
		} else {
			
			// show the get ready text and don't render the bricks
			batch.setProjectionMatrix(fontViewport.getCamera().combined);
			batch.begin();
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
			GlyphLayout gl1 = new GlyphLayout();
			gl1.setText(font, "GET READY TO START");
			font.draw(batch, gl1, (Constants.VIRTUAL_WIDTH-gl1.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.5f);
			GlyphLayout gl2 = new GlyphLayout();
			gl2.setText(font, Integer.toString(Math.round(countDownToStart)));
			font.draw(batch, gl2, (Constants.VIRTUAL_WIDTH-gl2.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.5f-gl1.height*2f);
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
		meow.dispose();
		money.dispose();
		knock.dispose();
		font.dispose();
		atlas.dispose();
		backgroundTexture.dispose();
		world.dispose();
	}

}
