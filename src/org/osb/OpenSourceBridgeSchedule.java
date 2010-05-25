package org.osb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class OpenSourceBridgeSchedule extends Activity {
	/** Called when the activity is first created. */
	EventAdapter mAdapter;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        InputStream is = null;
        URLConnection conn = null;
		try {
			URL url = new URL("http://opensourcebridge.org/events/2010/schedule.ics");
		    conn = url.openConnection(); 
		    conn.setDoInput(true); 
		    conn.setUseCaches(false);
			is = conn.getInputStream();

			ICal calendar = new ICal(is);
			
			ListView events = (ListView) findViewById(R.id.events);
			mAdapter = new EventAdapter(this, R.layout.listevent, calendar.getEvents());
	        events.setAdapter(mAdapter);
			events.setOnItemClickListener(new ListView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
					Event event = (Event) adapterview.getSelectedItem();
					
					LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = vi.inflate(R.layout.eventdetail, null);
					
                    //WebView webview = (WebView) v.findViewById(R.id.content);
                    //webview.loadData("testing <a href='http://google.com'> html</a>!", "text/html", "UTF-8");
                    
					setContentView(v);
				}
			});
            
			ParseProposals(calendar);
			
						
        } catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is!=null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    }

	/**
	 * Update calendar with data from proposals.json
	 * @param calendar
	 * @return
	 */
	private void ParseProposals(ICal calendar){
		
		InputStream is;
		
		is = getResources().openRawResource(R.raw.proposals_dict);
		String line;
		StringBuilder sb = new StringBuilder();
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}	
			JSONObject json = new JSONObject(sb.toString());
			ArrayList<Event> events = calendar.getEvents();
			for(int i=0; i<events.size(); i++){
				Event event = events.get(i);
				if (json.has(event.id)){
					JSONObject json_event = json.getJSONObject(event.id);
					event.description = json_event.getString("description");
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is!=null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private class EventAdapter extends ArrayAdapter<Event> {

		private ArrayList<Event> items;

		public EventAdapter(Context context, int textViewResourceId,
				ArrayList<Event> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.listevent, null);
			}
			Event e = items.get(position);
			if (e != null) {
				TextView title = (TextView) v.findViewById(R.id.title);
				TextView location = (TextView) v.findViewById(R.id.location);
				if (title != null) {
					title.setText(e.title);
				}
				if (location != null) {
					location.setText(e.location);
				} else {
					v.findViewById(R.id.separator).setVisibility(0);
				}
			}
			return v;
		}
	}
}