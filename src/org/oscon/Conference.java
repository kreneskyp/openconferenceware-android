package org.oscon;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Date;

public class Conference  implements Serializable{
	private static final long serialVersionUID = 8942450052954646740L;
	public Date start;
	public Date end;
	public HashMap<Integer, Track> tracks = new HashMap<Integer, Track>();
	public HashMap<Integer, Location> locations = new HashMap<Integer, Location>();
	
	/**
	 * creates array of dates from start and end date of conference
	 * @return
	 */
	public Date[] getDates() {
		int length = 0;
		int day = start.getDate();
		int month = start.getMonth();
		int year = start.getYear();
		
		if (month == end.getMonth()) {
			length = end.getDate() - day+1;
		}
		
		Date[] dates = new Date[length];
		
		for(int i=0; i<length; i++) {
			dates[i] = new Date(year, month, day+i);
		}
		
		return dates;
	}
}
