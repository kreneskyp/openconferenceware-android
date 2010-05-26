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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class OpenSourceBridgeSchedule extends Activity {
	
	EventAdapter mAdapter;
	ViewFlipper mFlipper;
	Button mBack;
	Animation mInLeft;
    Animation mInRight;
    Animation mOutLeft;
    Animation mOutRight;
	
    TextView mTitle;
    TextView mTime;
    TextView mLocation;
    TextView mDescription;
    
    private static final String SCHEDULE_URI = "http://opensourcebridge.org/events/2010/schedule.ics";
    
    
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mBack = (Button) findViewById(R.id.back);
        mFlipper = (ViewFlipper) findViewById(R.id.flipper);
        Context context = getApplicationContext();
        mInLeft = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
        mInRight = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        mOutLeft = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);
        mOutRight = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
        
        // grab views for details
        View detail = findViewById(R.id.detail); 
        mTitle = (TextView) detail.findViewById(R.id.title);
        mTime = (TextView) detail.findViewById(R.id.time);
        mLocation = (TextView) detail.findViewById(R.id.location);
        mDescription = (TextView) detail.findViewById(R.id.description);
        
        loadSchedule();
        
        mBack.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mFlipper.setInAnimation(mInLeft);
                mFlipper.setOutAnimation(mOutRight);
                mFlipper.showPrevious();
			}
		});
    }

	/**
	 * Loads the osbridge schedule from a combination of ICal and json data
	 */
	private void loadSchedule() {
		InputStream is = null;
        URLConnection conn = null;
		try {
			URL url = new URL(SCHEDULE_URI);
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
					Event event = (Event) adapterview.getAdapter().getItem(position);
					mTitle.setText(event.title);
					mLocation.setText(event.location);
					DateFormat startFormat = new SimpleDateFormat("E, H:mm");
					DateFormat endFormat = new SimpleDateFormat("H:mm a");
					String timeString = startFormat.format(event.start) + " - " + endFormat.format(event.end);
					mTime.setText(timeString);
					mDescription.setText(event.description);
					mFlipper.setInAnimation(mInRight);
                    mFlipper.setOutAnimation(mOutLeft);
                    mFlipper.showNext();
				}
			});
            
			// parse the proposals json to get additional fields
			parseProposals(calendar);
			
						
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
	 * Update calendar with data from proposals.json.  This is done because proposals
	 * contains talks that weren't accepted, and doesn't include the schedule.
	 * the ical doesn't include the long description and other fields.
	 * @param calendar
	 * @return
	 */
	private void parseProposals(ICal calendar){
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
					event.description = json_event.getString("description")
											.replace("\r","");
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
	
	/**
	 * EventAdapter used for displaying a list of events
	 *
	 */
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
				TextView time = (TextView) v.findViewById(R.id.time);
				if (title != null) {
					title.setText(e.title);
				}
				if (location != null) {
					location.setText(e.location);
				}
				if (time != null) {
					DateFormat formatter = new SimpleDateFormat("HH:mm");
					time.setText(formatter.format(e.start) + "-" + formatter.format(e.end));
				}
			}
			return v;
		}
	}
}