package com.qozgaming.thewall.utils;

import com.badlogic.gdx.math.Vector2;

public class UserData {
	
	private BodyRole bodyRole;
	private Vector2 dimensions;
	
	public UserData(BodyRole bodyRole, Vector2 dimensions) {
		this.bodyRole = bodyRole;
		this.dimensions = dimensions;
	}

	public BodyRole getBodyRole() {
		return bodyRole;
	}

	public Vector2 getDimensions() {
		return dimensions;
	}
	
}
