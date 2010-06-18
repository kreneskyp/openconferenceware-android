package org.osb;

import java.io.Serializable;
import java.util.*;

import com.google.gson.annotations.SerializedName;

public class Schedule implements Serializable
{
	private static final long serialVersionUID = -3970790030379213457L;
	@SerializedName("items")
	public List<Event> events = new ArrayList<Event>();
}
