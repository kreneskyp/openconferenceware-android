package org.osb;
import java.util.Date;


public class Event {
	public Date start, end;
	public String description;
	public String title;
	public String url;
	public String location;
	public String brief;
	public String id;
	public int track;
	
	public Event(){
		start = null;
		end = null;
		description = null;
		title = null;
		url = null;
		location = null;
		track = 0;
	}
	
	public String getTrackColor() {
		switch(track) {
		default:
			return "#FFF";
		}
	}
}
