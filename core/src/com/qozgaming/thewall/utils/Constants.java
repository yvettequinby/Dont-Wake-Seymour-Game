package com.qozgaming.thewall.utils;

public class Constants {
	
	public static final float SCENE_WIDTH = 12.8f; 
	public static final float SCENE_HEIGHT = 7.2f; 
	public static final float WORLD_TO_SCREEN = 0.01f;
	public static final float SCREEN_TO_WORLD = 100.0f;
	public static final float VIRTUAL_WIDTH = SCENE_WIDTH * SCREEN_TO_WORLD; 
	public static final float VIRTUAL_HEIGHT = SCENE_HEIGHT * SCREEN_TO_WORLD; 
	
	public static final float BLOCK_HEIGHT = 0.4f;
	public static final float EDGE_WIDTH = 0.2f;
	public static final float LEDGE_WIDTH = 0.3f;
	
	public static final float BLOCK_Y_SPACER = 0.015f;
	public static final float BLOCK_X_SPACER = 0.02f;
	public static final float MIN_BLOCK_WIDTH = 1f;
	public static final float MAX_BLOCK_WIDTH = 3f;
	
	public static final String ASSET_BACKGROUND_IMAGE = "background.png";
	public static final String ASSET_BALL_IMAGE = "ball.png";
	public static final String ASSET_WALL_IMAGE = "wall.png";
	public static final String ASSET_BRICK_IMAGE = "brick.png";
	public static final String ASSET_FONT_NARKISIM = "narkisim.fnt"; 
}
