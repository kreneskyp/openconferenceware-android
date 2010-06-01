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

import com.google.gson.Gson;

public class DataService
{
    private static final String SCHEDULE_URI = "http://opensourcebridge.org/events/2010/schedule.json";
    private static final String SPEAKER_URI_BASE = "http://opensourcebridge.org/users/";
	// Cache files for 2 hours (in milliseconds)
	private static final long CACHE_TIMEOUT = 7200000;
    
    private File dataDirectory;
    
	public DataService(File dataDir)
	{
		this.dataDirectory = dataDir;
	}
	
	public Speaker getSpeaker(Integer speakerId, boolean force)
	{
		Speaker s = getObject(Speaker.class, SPEAKER_URI_BASE + speakerId + ".json", force);
		
		if (s.biography != null)
		{
			s.biography = s.biography.replace("\r", "");
		}
		
		return s;
	}
	
	public Schedule getSchedule()
	{
		Schedule s = getObject(Schedule.class, SCHEDULE_URI, false);

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
				if ( (event.location == "null") || (event.location == null) ) {
					event.location = "";
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
	
	private <T> T getObject(Class<T> clazz, String uri, boolean force)
	{
		Gson gson = GsonFactory.createGson();
		
		String json = getURL(uri, force);
		
		System.out.println(json);
		
		return gson.fromJson(json, clazz);
	}
	
	/**
	 * fetches a url and returns it as a string.  This method will cache the
	 * result locally and use the cache on repeat loads
	 * @param uri - a uri beginning with http://
	 * @param force - force refresh of data
	 * @return
	 */
	private String getURL(String uri, boolean force) {
		InputStream is = null;
		OutputStream os = null;
		
		// get file path for cached file
		String dir = this.dataDirectory.getAbsolutePath();
		String path = uri.substring(uri.lastIndexOf("/")+1);
		File file = new File(dir+"/"+path);
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
