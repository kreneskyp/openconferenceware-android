package org.osb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.*;
import java.util.Date;



/**
 * Class for parsing ical files
 * @author Peter Krenesky - peter@osuosl.org
 *
 */
public class ICal {
	
	private String mName, mVersion, mProdid, mCalscale;
	private List<Event> mEvents; 
	
	public ICal() {}
	
	public ICal(InputStream is){
		mEvents = new ArrayList<Event>();
		Event event = null;
		String line;
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			DateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss"); 
		
        
			while ((line = br.readLine()) != null) {
				if(line.equals("BEGIN:VEVENT"))  {
					// start of event
					event = new Event();
      
				} else if(line.equals("END:VEVENT"))  {
					if (event != null){
						// clean up description
						event.brief = event.brief
												.replace("\\,", ",")
												.replace("\\;", ";");
						
			    		mEvents.add(event);
			    		event = null;
					}
				} else if(line.startsWith("DTEND:"))  {
					String str = line.substring(6);
					event.end = (Date)formatter.parse(str);
				
				} else if(line.startsWith("DTSTART:"))  {
					String str = line.substring(8);
					event.start = (Date)formatter.parse(str);
				
				} else if(line.startsWith("SUMMARY:"))  {
					event.title = line.substring(8)
										.replace("\\,", ",")
										.replace("\\;", ";");
				
				} else if(line.startsWith("DESCRIPTION:"))  {
					event.brief = line.substring(12);
				
				} else if(line.startsWith(" "))  {
					// continuation of description
					event.brief += line.substring(1);
				
				} else if(line.startsWith("URL:"))  {
					event.url = line.substring(4);
					event.id = event.url.substring(37);
					
				} else if(line.startsWith("LOCATION:"))  {
					event.location = line.substring(9);
				
				} else if(line.startsWith("X-WR-CALNAME:"))  {
					mName = line.substring(13); 
				} else if(line.startsWith("X-WR-TIMEZONE:"))  {
					// TODO - parse time zone
				} else if(line.startsWith("VERSION:"))  {
					mVersion = line.substring(8);
				} else if(line.startsWith("PRODID:"))  {
					mProdid = line.substring(7);
				} else if(line.startsWith("CALSCALE:"))  {
					mCalscale = line.substring(9);
				}
      }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public List<Event> getEvents(){
		return mEvents;
	}
	
	public void setEvents(List<Event> events){
		mEvents = events;
	}
}
