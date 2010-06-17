package org.osb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
		    conference = getObject(Conference.class, CONFERENCE_URI, "conference.json", force);
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
		Event event = getObject(Event.class, EVENT_URI_BASE+event_id, "event_"+event_id+".json", force);
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
		Schedule s = getObject(Schedule.class, SCHEDULE_URI+date.getTime(), "schedule_"+date.getTime()+".json", force);	
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
	 * Get an object.  This method will use the filedate+force to determine whether
	 * to load the object from the cached file or from the remote uri.  If the local
	 * file fails to load, the remote source will be tried.  If the remote source fails
	 * the local file will be tried if it exists.
	 * 
	 * @param clazz - class to fetch
	 * @param uri - uri to fetch it from
	 * @param local_file - local file to cache it in 
	 * @param force - ignore cache, force remote retrieval
	 * @return fetched object
	 */
	private <T> T getObject(Class<T> clazz, String uri, String local_file, boolean force)
	{
		T obj;
		// get file path for cached file
		String dir = this.dataDirectory.getAbsolutePath();
		File file = new File(dir+"/"+local_file);
		if (file.exists() && file.lastModified()+CACHE_TIMEOUT > System.currentTimeMillis() && !force){
			// file was cached and cache hasn't expire, load local file
			try {
				obj = getLocalObject(clazz, file);
			} catch (Exception e) {
				// error loading local file.
				// fall back to loading from the source uri
				obj = getRemoteObject(clazz, uri, file);
				e.printStackTrace();
			}
		} else {
			obj = getRemoteObject(clazz, uri, file);
			if (obj == null && file.exists()) {
				// data couldn't be fetched remotely. fall back to local file 
				// if it exists regardless of age. any data is better than no data
				obj = getLocalObject(clazz, file);
			}
		}
		return obj;
	}
	
	/**
	 * Load an object from a local file
	 * @param <T> - class that is being loaded
	 * @param clazz - class that is being loaded
	 * @param file - local file
	 * @return object, or null if object can't be loaded from the file
	 */
	@SuppressWarnings("unchecked")
	private <T> T getLocalObject(Class<T> clazz, File file) {
		ObjectInputStream is = null;
		T obj = null;
		try {
			is = new ObjectInputStream(new FileInputStream(file));
			obj  = (T) is.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return obj;
	}
	
	/**
	 * Remote an object from a remote uri.  The remote object will be json
	 * that needs to be decoded.  This method will cache the object
	 * in a local file.
	 * @param <T> - class that is being loaded
	 * @param clazz - class that is being loaded
	 * @param uri - uri to retrieve object from
	 * @param file - file to cache object to.
	 * @return object, or null if object can't be loaded
	 */
	private <T> T getRemoteObject(Class<T> clazz, String uri, File file) {
		Gson gson = GsonFactory.createGson();
		String json = getURL(uri);
		if (json == null)
			return null;
		T obj = gson.fromJson(json, clazz);
		
		// cache serialized object to file in thread so it does not
		// impact speed of rendering the UI
		new WriteThread(file, obj).start();
        
		return obj;
	}
	
	/**
	 * fetches a url and returns the response as a string. 
	 * @param uri - a uri beginning with http://
	 * @param local_file - local file name data is cached in
	 * @param force - force refresh of data
	 * @return string, null if there is an exception
	 */
	private String getURL(String uri) {
		InputStream is = null;
		
		// get file path for cached file
		String line;
		StringBuilder sb = new StringBuilder();
		String json = null;
		try {
			// determine whether to open local file or remote file
			URL url = new URL(uri);
			URLConnection conn = null;

			conn = url.openConnection(); 
			conn.setDoInput(true); 
			conn.setUseCaches(false);
			is = conn.getInputStream();

			// read entire file, write cache at same time if we are fetching from the remote uri
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8192);
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			
			json = sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			json=null;
		} finally {
			if (is!=null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return json;
	}

	/**
	 * Class for separating file writing from reading so that
	 * the user interface can return quicker 
	 * @author peter
	 */
	class WriteThread extends Thread {
		File file;
		Object obj;
		
		public WriteThread(File file, Object obj){
			this.file = file;
			this.obj = obj;
		}
		
		public void run(){
			ObjectOutputStream out = null;
			try{
				out = new ObjectOutputStream(new FileOutputStream(file));
				out.writeObject(obj);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
}
