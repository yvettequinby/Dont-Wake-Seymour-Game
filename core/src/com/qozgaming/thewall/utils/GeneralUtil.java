package com.qozgaming.thewall.utils;

import com.badlogic.gdx.physics.box2d.Body;

public class GeneralUtil {
	
	public static UserData retrieveUserData(Body b) {
		UserData result = null;
		if(b!=null) {
			result = (UserData)b.getUserData();
		}
		return result;
	}
	
}
