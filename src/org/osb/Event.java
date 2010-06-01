package org.osb;
import java.util.Date;

import org.osb.R;

import com.google.gson.annotations.SerializedName;

public class Event {
	private static final int TRACK_BUSINESS = 7;
	private static final int TRACK_CHEMISTRY = 8;
	private static final int TRACK_COOKING = 9;
	private static final int TRACK_CULTURE = 10;
	private static final int TRACK_HACKS = 11;
	
	@SerializedName("start_time")
	public Date start;
	@SerializedName("end_time")
	public Date end;
	public String description;
	public String title;
	public String url;
	public String location;
	public String brief;
	public String id;
	public int track = -1;
	public String speakers;
	public String[] user_titles;
	public Integer[] speaker_ids;
	
	public Event(){
		start = null;
		end = null;
		description = null;
		title = null;
		url = null;
		location = null;
		track = -1;
		speakers = null;
		speaker_ids = null;
		user_titles = new String[0];
	}
	
	public String getTrackName() {
		switch(track) {
		case TRACK_BUSINESS:
			return "Business";
		case TRACK_CHEMISTRY:
			return "Chemistry";
		case TRACK_COOKING:
			return "Cooking";
		case TRACK_CULTURE:
			return "Culture";
		case TRACK_HACKS:
			return "Hacks";
		default:
			return "";
		}
	}
	
	/**
	 * @return the resource id for the track color
	 */
	public int getTrackColor() {
		switch(track) {
		case TRACK_BUSINESS:
			return R.color.track_business;
		case TRACK_CHEMISTRY:
			return R.color.track_chemistry;
		case TRACK_COOKING:
			return R.color.track_cooking;
		case TRACK_CULTURE:
			return R.color.track_culture;
		case TRACK_HACKS:
			return R.color.track_hacks;
		default:
			return R.color.track_other;
		}
	}
	
	/**
    * @return the resource id for the track color darker shade
    */
    public int getTrackColorDark() {
	   switch(track) {
	   case TRACK_BUSINESS:
	           return R.color.track_business_dark;
	   case TRACK_CHEMISTRY:
	           return R.color.track_chemistry_dark;
	   case TRACK_COOKING:
	           return R.color.track_cooking_dark;
	   case TRACK_CULTURE:
	           return R.color.track_culture_dark;
	   case TRACK_HACKS:
	           return R.color.track_hacks_dark;
	   default:
	           return R.color.track_other_dark;
	   }
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
