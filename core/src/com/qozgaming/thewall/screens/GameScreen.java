package com.qozgaming.thewall.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.qozgaming.thewall.TheWallGame;
import com.qozgaming.thewall.utils.Constants;

public class GameScreen extends InputAdapter implements Screen, ContactListener {
	
	private final TheWallGame game;
	
	private SpriteBatch batch;
	private Texture backgroundTexture;
	private Texture ballTexture;
	private Texture wallTexture;
	private Texture brickTexture;
	private BitmapFont font;
	
	private Body cat;
	private Body floorBody;
	private List<Body> staticBodies = new ArrayList<Body>();
	private List<Body> dynamicBodies = new ArrayList<Body>();
	
	private boolean wokeCat = false;
	private boolean start = false;
	private float countDownToStart = 5f;
	private float timePerBrick = 5f;
	private int bricksRemoved = 0;
	private float timeSinceLastBrick = 0f;
	private boolean isGameOver = false;
	private float timeSinceGameOver = 0f;
	
	private Viewport viewport;
	private OrthographicCamera fontCamera;
	private Viewport fontViewport;
	private Vector3 point = new Vector3();
	private Box2DDebugRenderer debugRenderer;
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
		ballTexture = new Texture(Gdx.files.internal(Constants.ASSET_BALL_IMAGE));
		wallTexture = new Texture(Gdx.files.internal(Constants.ASSET_WALL_IMAGE));
		brickTexture = new Texture(Gdx.files.internal(Constants.ASSET_BRICK_IMAGE));
		font = new BitmapFont(Gdx.files.internal(Constants.ASSET_FONT_NARKISIM));
		
		batch = new SpriteBatch();
		
		Gdx.input.setInputProcessor(this);

		// Create Physics World
		world = new World(new Vector2(0, -9.8f), true);

		// Tweak debug information
		debugRenderer = new Box2DDebugRenderer(true, /* draw bodies */
				false, /* don't draw joints */
				true, /* draw aabbs */
				true, /* draw inactive bodies */
				false, /* don't draw velocities */
				true /* draw contacts */);

		world.setContactListener(this);
		
		buildBlocks();
		
		BodyDef catBodyDef = new BodyDef();
		catBodyDef.type = BodyType.DynamicBody;
		catBodyDef.position.set(Constants.SCENE_WIDTH*0.5f, Constants.SCENE_HEIGHT);
		CircleShape catShape = new CircleShape();
		catShape.setRadius(Constants.BLOCK_HEIGHT*0.5f);
				
		FixtureDef catFixtureDef = new FixtureDef();
		catFixtureDef.shape = catShape;
		catFixtureDef.friction = 0.9f;
		catFixtureDef.restitution = 0.1f;
		
		cat = world.createBody(catBodyDef);
		cat.createFixture(catFixtureDef);
		cat.setUserData(new Vector2(Constants.BLOCK_HEIGHT, Constants.BLOCK_HEIGHT));
				
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
		staticBodies.add(leftEdgeBody);
		
		edgeBodyDef.position.set(Constants.SCENE_WIDTH-Constants.EDGE_WIDTH*0.5f, Constants.SCENE_HEIGHT*0.5f);
		Body rightEdgeBody = world.createBody(edgeBodyDef);
		rightEdgeBody.createFixture(edgeFixtureDef);
		rightEdgeBody.setUserData(new Vector2(Constants.EDGE_WIDTH, Constants.SCENE_HEIGHT));
		staticBodies.add(rightEdgeBody);		
		
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
		floorFixtureDef.friction = 0.0f;
		floorFixtureDef.restitution = 0f;
		
		floorBody = world.createBody(floorBodyDef);
		floorBody.createFixture(floorFixtureDef);
		floorBody.setUserData(new Vector2(Constants.SCENE_WIDTH, Constants.BLOCK_HEIGHT));
		staticBodies.add(floorBody);
		
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
		}
		return result;
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
			fixtureDef.friction = 0.1f;
			fixtureDef.restitution = 0.1f;
			
			Body blockBody = world.createBody(bodyDef);
			blockBody.createFixture(fixtureDef);
			blockBody.setUserData(new Vector2(width, height));
			dynamicBodies.add(blockBody);
			
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
		staticBodies.add(ledgeBody);
		
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
		
		if(!isGameOver) {
			
			viewport.unproject(point.set(screenX, screenY, 0)); // important to use viewport, not camera
			
			hitBody = null;
			world.QueryAABB(callback, point.x - 0.0001f, point.y - 0.0001f, point.x + 0.0001f, point.y + 0.0001f);
			if (hitBody != null) {
				world.destroyBody(hitBody);
				dynamicBodies.remove(hitBody);
				bricksRemoved++;
				timeSinceLastBrick = 0f;
			}
			
		}
		
		return false;
		
	}
	
	@Override
	public void beginContact(Contact contact) {
		// TODO Auto-generated method stub
		
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
			timeSinceGameOver = timeSinceGameOver + delta;
			floorBody.setType(BodyType.DynamicBody);
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
			for (Body body : staticBodies) {
				float bodyWidth = ((Vector2)body.getUserData()).x;
				float bodyHeight = ((Vector2)body.getUserData()).y;
				float xPos = body.getPosition().x - bodyWidth*0.5f;
				float yPos = body.getPosition().y - bodyHeight*0.5f;
				batch.draw(wallTexture, 
						xPos, yPos,  
						bodyWidth*0.5f, bodyHeight*0.5f, 
						bodyWidth, bodyHeight,  
						1f, 1f, 
						(float) Math.toDegrees(body.getAngle()), 
						0, 0, 
						wallTexture.getWidth(), wallTexture.getHeight(),
						false,false);
			}
			for (Body body : dynamicBodies) {
				float bodyWidth = ((Vector2)body.getUserData()).x;
				float bodyHeight = ((Vector2)body.getUserData()).y;
				float xPos = body.getPosition().x - bodyWidth*0.5f;
				float yPos = body.getPosition().y - bodyHeight*0.5f;
				batch.draw(brickTexture, 
						xPos, yPos,  
						bodyWidth*0.5f, bodyHeight*0.5f, 
						bodyWidth, bodyHeight,  
						1f, 1f, 
						(float) Math.toDegrees(body.getAngle()), 
						0, 0, 
						brickTexture.getWidth(), brickTexture.getHeight(),
						false,false);
			}
			float bodyWidth = ((Vector2)cat.getUserData()).x;
			float bodyHeight = ((Vector2)cat.getUserData()).y;
			float xPos = cat.getPosition().x - bodyWidth*0.5f;
			float yPos = cat.getPosition().y - bodyHeight*0.5f;
			batch.draw(ballTexture, 
					xPos, yPos,  
					bodyWidth*0.5f, bodyHeight*0.5f, 
					bodyWidth, bodyHeight,  
					1f, 1f, 
					(float) Math.toDegrees(cat.getAngle()), 
					0, 0, 
					ballTexture.getWidth(), ballTexture.getHeight(),
					false,false);
			batch.end();
			
			// fonts
			batch.setProjectionMatrix(fontViewport.getCamera().combined);
			batch.begin();
			font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 0.7f);
			String score = Integer.toString(Math.round(countDown * 10f)) + " / " + bricksRemoved;
			font.draw(batch, score, 60f, Constants.VIRTUAL_HEIGHT - 25f);
			if(isGameOver) {
				if(timeSinceGameOver>6f) {
					font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
					GlyphLayout gl = new GlyphLayout();
					gl.setText(font, "YOU SCORED: " + bricksRemoved);
					font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.5f);
				} else if(timeSinceGameOver>4f) {
					font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
					GlyphLayout gl = new GlyphLayout();
					if(wokeCat) {
						gl.setText(font, "YOU WOKE THE CAT");
					} else {
						gl.setText(font, "YOU ARE TOO SLOW");
					}
					font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.5f);
				} else if(timeSinceGameOver>2f) {
					font.setColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, 1f);
					GlyphLayout gl = new GlyphLayout();
					gl.setText(font, "GAME OVER");
					font.draw(batch, gl, (Constants.VIRTUAL_WIDTH-gl.width)*0.5f, Constants.VIRTUAL_HEIGHT * 0.5f);
				}
			}
			batch.end();
			
			if(isGameOver) {
				if(timeSinceGameOver>8f) {
					game.restart();
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
		font.dispose();
		ballTexture.dispose();
		wallTexture.dispose();
		brickTexture.dispose();
		backgroundTexture.dispose();
		debugRenderer.dispose();
		world.dispose();
	}

}
