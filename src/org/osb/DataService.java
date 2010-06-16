package org.osb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import com.google.gson.Gson;

public class DataService
{
    private static final String CONFERENCE_URI = "http://doors.osuosl.org:8000/conference/";
	private static final String SCHEDULE_URI = "http://doors.osuosl.org:8000/sessions_day/";
    private static final String SPEAKER_URI_BASE = "http://doors.osuosl.org:8000/speaker/";
    private static final String EVENT_URI_BASE = "http://doors.osuosl.org:8000/session/";
	// Cache files for 2 hours (in milliseconds)
	private static final long CACHE_TIMEOUT = 7200000;
    
    private File dataDirectory;
    
	public DataService(File dataDir)
	{
		this.dataDirectory = dataDir;
	}
	
	/**
	 * get the conference object containing general info about the con
	 * @param force - force refresh
	 * @return
	 */
	public Conference getConference(boolean force) {
		Conference conference = null;
		try{
		    conference = getObject(Conference.class, CONFERENCE_URI, "conference.json", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conference;
	}
	
	/**
	 * loads full details for the event
	 * @param event
	 * @param force
	 * @return
	 */
	public Event getEvent(String event_id, boolean force){
		Event event = getObject(Event.class, EVENT_URI_BASE+event_id, "event_"+event_id+".json", true);
		event.details = true;
		return event;
	}
	
	
	public Speaker getSpeaker(Integer speakerId, boolean force)
	{
		Speaker s = getObject(Speaker.class, SPEAKER_URI_BASE + speakerId, "speaker_"+speakerId+".json", force);
		
		if (s.biography != null)
		{
			s.biography = s.biography.replace("\r", "");
		}
		
		return s;
	}
	
	/**
	 * Gets the schedule
	 * @param date - date of schedule to fetch
	 * @param force - force refresh from server
	 * @return
	 */
	public Schedule getSchedule(Date date, boolean force)
	{
		Schedule s = getObject(Schedule.class, SCHEDULE_URI+date.getTime(), "schedule_"+date.getTime()+".json", true);
		for (Event event : s.events)
		{
				if (event.description == null)
				{
					event.description = "";
				}
				event.description = event.description
							.replace("\r","")
							.replace("<br>","\n")
							.replace("<blockquote>","")
							.replace("</blockquote>","")
							.replace("<b>","")
							.replace("</b>","");
				if (event.description.equals("")){
					//XXX fill description with spaces, fixes a bug where android will
					//    center the logo on the detail page without content in description
					event.description = "                                                                                  ";
				}

				if (event.user_titles != null ) {
					StringBuilder speakers = new StringBuilder();
					for (int z = 0; z < event.user_titles.length; z++) {
						String speaker = event.user_titles[z];
						
						speakers.append(speaker);
						
						if (z < (event.user_titles.length - 1))
						{
							speakers.append(", ");
						}
						
					}
					event.speakers = speakers.toString();
				} 
			}
		return s;
	}
	
	/**
	 * 
	 * @param clazz - class to fetch
	 * @param uri - uri to fetch it from
	 * @param local_file - local file to cache it in 
	 * @param force - ignore cache, force remote retrieval
	 * @return fetched object
	 */
	private <T> T getObject(Class<T> clazz, String uri, String local_file, boolean force)
	{
		Gson gson = GsonFactory.createGson();
		String json = getURL(uri, local_file, force);
		//System.out.println(json);
		return gson.fromJson(json, clazz);
	}
	
	/**
	 * fetches a url and returns it as a string.  This method will cache the
	 * result locally and use the cache on repeat loads
	 * @param uri - a uri beginning with http://
	 * @param local_file - local file name data is cached in
	 * @param force - force refresh of data
	 * @return
	 */
	private String getURL(String uri, String local_file, boolean force) {
		InputStream is = null;
		OutputStream os = null;
		
		// get file path for cached file
		String dir = this.dataDirectory.getAbsolutePath();
		File file = new File(dir+"/"+local_file);
		String line;
		StringBuilder sb = new StringBuilder();
		try {
			// determine whether to open local file or remote file
			if (file.exists() && file.lastModified()+CACHE_TIMEOUT > System.currentTimeMillis() && !force){
				is = new FileInputStream(file);
			} else {
				URL url = new URL(uri);
				URLConnection conn = null;
				try {
					conn = url.openConnection(); 
					conn.setDoInput(true); 
					conn.setUseCaches(false);
					is = conn.getInputStream();
					os = new FileOutputStream(file);
				} catch (IOException e) {
					// fall back to local file if exists, regardless of age
					if (file.exists()) {
						is = new FileInputStream(file);
					} else {
						e.printStackTrace();
						throw e;
					}
				}
			}
		
			// read entire file, write cache at same time if we are fetching from the remote uri
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8192);
			OutputStreamWriter bw = null;
			if (os != null) {
				bw = new OutputStreamWriter(os);
			}
			while ((line = br.readLine()) != null) {
				sb.append(line);
				if (bw != null) {
					bw.append(line);
				}
			}
			if (bw != null) {
				bw.flush();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
			
		} finally {
			if (is!=null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (os!=null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return sb.toString();
	}

}
