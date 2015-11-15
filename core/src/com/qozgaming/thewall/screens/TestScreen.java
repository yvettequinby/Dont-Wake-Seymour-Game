package com.qozgaming.thewall.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
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
import com.qozgaming.thewall.utils.Constants;

public class TestScreen extends InputAdapter implements Screen, ContactListener {
	
	
	private Viewport viewport;
	private Vector3 point = new Vector3();
	private Box2DDebugRenderer debugRenderer;
	private World world;
	private Body hitBody = null;
	
	
	public TestScreen() {
		
		viewport = new FitViewport(Constants.SCENE_WIDTH, Constants.SCENE_HEIGHT);
		viewport.getCamera().position.set(viewport.getCamera().position.x + Constants.SCENE_WIDTH * 0.5f, viewport.getCamera().position.y + Constants.SCENE_HEIGHT * 0.5f, 0);
		viewport.getCamera().update();
		
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
		
	}
	
	
	private void buildBlocks() {
		
		BodyDef edgeBodyDef = new BodyDef();
		edgeBodyDef.type = BodyType.StaticBody;
		edgeBodyDef.position.set(Constants.SCENE_WIDTH*0.5f, 0.5f);

		PolygonShape edgeShape = new PolygonShape();
		edgeShape.setAsBox(Constants.SCENE_WIDTH*0.5f, 0.5f);
				
		FixtureDef edgeFixtureDef = new FixtureDef();
		edgeFixtureDef.shape = edgeShape;
		edgeFixtureDef.friction = 0.5f;
		edgeFixtureDef.restitution = 0f;
		
		Body leftEdgeBody = world.createBody(edgeBodyDef);
		leftEdgeBody.createFixture(edgeFixtureDef);
		
		
		
	}
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		
		viewport.unproject(point.set(screenX, screenY, 0));
		
		hitBody = null;
		System.out.println("screen: " + screenX + " " + screenY);
		System.out.println("world: " + point.x + " " + point.y);
		world.QueryAABB(callback, point.x - 0.0001f, point.y - 0.0001f, point.x + 0.0001f, point.y + 0.0001f);
		return false;
	}
	
	QueryCallback callback = new QueryCallback() {
		@Override
		public boolean reportFixture (Fixture fixture) {
			// if the hit point is inside the fixture of the body
			// we report it
			if (fixture.testPoint(point.x, point.y)) {
				hitBody = fixture.getBody();
				System.out.println("HIT");
				return false;
			} else
				return true;
		}
	};
	

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
		Gdx.gl.glClearColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, Color.WHITE.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		world.step(1 / 60f, 6, 2);
		debugRenderer.render(world, viewport.getCamera().combined);
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
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
		debugRenderer.dispose();
		world.dispose();
	}

}
