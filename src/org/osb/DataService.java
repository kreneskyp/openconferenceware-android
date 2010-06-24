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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.content.res.Resources;

import com.google.gson.Gson;

public class DataService
{
    private static final String CONFERENCE_URI = "http://hendrix:8000/conference/";
    private static final String SCHEDULE_URI = "http://hendrix:8000/sessions_day/";
    private static final String SPEAKER_URI_BASE = "http://hendrix:8000/speaker/";
    private static final String EVENT_URI_BASE = "http://hendrix:8000/session/";
    
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";
    
    // Cache timeouts in milliseconds
    private static final long SCHEDULE_CACHE_TIMEOUT = 14400000;     // 4 hours
    private static final long EVENT_CACHE_TIMEOUT = 14400000;        // 4 hours
    private static final long SPEAKER_CACHE_TIMEOUT = 432000000;     // 5 days
    private static final long CONFERENCE_CACHE_TIMEOUT = 432000000;  // 5 days
    
    private File dataDirectory;
    private Resources resources;
    private String packageName;
    
   
    final DefaultHttpClient client = new DefaultHttpClient(new BasicHttpParams());
    
    
    public DataService(File dataDir, Resources resources, String packageName)
    {
        this.dataDirectory = dataDir;
        this.resources = resources;
        this.packageName = packageName;
    }
    
    /**
     * get the conference object containing general info about the con
     * @param force - force refresh
     * @return
     */
    public Conference getConference(boolean force) {
        return getObject(Conference.class, CONFERENCE_URI, "conference.json", force, CONFERENCE_CACHE_TIMEOUT);
    }
    
    /**
     * loads full details for the event
     * @param event
     * @param force
     * @return
     */
    public Event getEvent(String event_id, boolean force){
        Event event = getObject(Event.class, EVENT_URI_BASE+event_id, "event_"+event_id+".json", force, EVENT_CACHE_TIMEOUT);
        if (event != null){
            event.details = true;
        }
        return event;
    }
    
    
    public Speaker getSpeaker(Integer speakerId, boolean force)
    {
        return getObject(Speaker.class, SPEAKER_URI_BASE + speakerId, "speaker_"+speakerId+".json", force, SPEAKER_CACHE_TIMEOUT);
    }
    
    /**
     * Gets the schedule
     * @param date - date of schedule to fetch
     * @param force - force refresh from server
     * @return
     */
    public Schedule getSchedule(Date date, boolean force)
    {
        DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
        Schedule s = getObject(Schedule.class, SCHEDULE_URI+date.getTime(), "schedule_"+formatter.format(date)+".json", force, SCHEDULE_CACHE_TIMEOUT);    
        if (s != null) {
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
    private <T> T getObject(Class<T> clazz, String uri, String local_file, boolean force, long timeout)
    {
        T obj = null;
        // get file path for cached file
        String dir = this.dataDirectory.getAbsolutePath();
        File file = new File(dir+"/"+local_file);
        
        if (file.exists() && file.lastModified()+timeout > System.currentTimeMillis() && !force){
            // file was cached and cache hasn't expire, load local file
            try {
                obj = getLocalObject(clazz, file);
            } catch (Exception e) {
                // error loading local file.
                // fall back to loading from the source uri
                obj = getRemoteObject(clazz, uri, file);
            }
        } else {
            obj = getRemoteObject(clazz, uri, file);
            if (obj == null && file.exists()) {
                // data couldn't be fetched remotely. fall back to local file 
                // if it exists regardless of age. any data is better than no data
                obj = getLocalObject(clazz, file);
            }
        }
        
        // last ditch effort, load pre-packaged data
        if (obj == null) {
            obj = getPreloadedObject(clazz, file, local_file);
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
     * Loads data from a preloaded files.  preloaded files are shipped with the
     * app as a last resort to load data when there is no network available.
     * @param <T> - class that is being loaded
     * @param clazz - class that is being loaded
     * @param uri - uri to retrieve object from
     * @param file - file to cache object to.
     * @return object, or null if object can't be loaded
     */
    private <T> T getPreloadedObject(Class<T> clazz, File file, String filename) {
        T obj = null;
        Gson gson = GsonFactory.createGson();
        String json = null;
        InputStream is = null;
        String line;
        StringBuilder sb = new StringBuilder();
        try{
            // read entire file
            is = resources.openRawResource(resources.getIdentifier(filename.substring(0,filename.length()-5), "raw", packageName));
            BufferedReader br = new BufferedReader(new InputStreamReader(is), 8192);
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            json = sb.toString();
            if (json != null)
                obj = gson.fromJson(json, clazz);
                // cache serialized object to file in thread so it does not
                // impact speed of rendering the UI.  use the last modified
                // date of the preloaded file.  This ensures that the app
                // will detect it as out of date sooner.
                new WriteThread(file, obj, file.lastModified()).start();
            
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
            // create and execute GET request
            final HttpGet request = new HttpGet(uri);
            request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
            final HttpResponse response = client.execute(request);
            final HttpEntity entity = response.getEntity();
            
            // check headers for gzip compression
            final Header encoding = entity.getContentEncoding();
            if (encoding != null) {
                for (HeaderElement element : encoding.getElements()) {
                    if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
                        is = new GZIPInputStream(entity.getContent());
                    }
                }
            }
            
            // wasn't compressed, open vanilla InputStream
            if (is == null) {
                is = entity.getContent();
            }
            
            // read entire file
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8192);
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            
            json = sb.toString();
        } catch (Exception e) {
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
        long lastmodified;
        
        /**
         * 
         * @param file
         * @param obj - 
         * @param lastmodified - date to set lastmodified
         */
        public WriteThread(File file, Object obj, long lastmodified){
            this.file = file;
            this.obj = obj;
            this.lastmodified = lastmodified;
        }
        
        public WriteThread(File file, Object obj){
            this.file = file;
            this.obj = obj;
            lastmodified = -1;
        }
        
        public void run(){
            ObjectOutputStream out = null;
            try{
                out = new ObjectOutputStream(new FileOutputStream(file));
                out.writeObject(obj);
                
                if (lastmodified != -1){
                    file.setLastModified(lastmodified);
                }
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
