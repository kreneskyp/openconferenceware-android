package org.osb;

import java.util.HashMap;
import java.util.Date;

public class Conference {
	public Date start;
	public Date end;
	public HashMap<Integer, Track> tracks = new HashMap<Integer, Track>();
}
