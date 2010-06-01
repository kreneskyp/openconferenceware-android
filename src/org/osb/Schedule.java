package org.osb;

import java.util.*;

import com.google.gson.annotations.SerializedName;

public class Schedule
{
	@SerializedName("items")
	public List<Event> events = new ArrayList<Event>();
}
