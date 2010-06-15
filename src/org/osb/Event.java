package org.osb;
import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class Event {
	
	@SerializedName("start_time")
	public Date start;
	@SerializedName("end_time")
	public Date end;
	public String description;
	public String title;
	public String url;
	@SerializedName("room_id")
	public int location = -1;
	public String brief;
	public String id;
	@SerializedName("track_id")
	public int track = -1;
	public String trackTitle;
	public String speakers;
	public String[] user_titles;
	@SerializedName("user_ids")
	public Integer[] speaker_ids;
	
	public Event(){
		start = null;
		end = null;
		description = null;
		title = null;
		url = null;
		location = -1;
		track = -1;
		speakers = null;
		speaker_ids = new Integer[0];
		user_titles = new String[0];
	}
    
    @Override
    public String toString()
    {
    		return this.title + "|" + this.id + "|" + this.speakers 
    				+ "| start="
    				+ start
    				+ "| end="
    				+ end;
    }
}
