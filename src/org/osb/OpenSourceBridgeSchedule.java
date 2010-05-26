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
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
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

	private static final Date JUN1 = new Date(110, 5, 1);
	private static final Date JUN2 = new Date(110, 5, 2);
	private static final Date JUN3 = new Date(110, 5, 3);
	private static final Date JUN4 = new Date(110, 5, 4);
	
	private static final int MENU_NOW = 0;
	private static final int MENU_JUN1 = 1;
	private static final int MENU_JUN2 = 2;
	private static final int MENU_JUN3 = 3;
	private static final int MENU_JUN4 = 4;
	private static final int MENU_NEXT = 5;
	private static final int MENU_PREV = 6;
	
	Date mCurrentDate;
	TextView mDate;
	boolean mDetail = false;
	
	EventAdapter mAdapter;
	ListView mEvents;
	
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
        
        mDate = (TextView) findViewById(R.id.date);
        mEvents = (ListView) findViewById(R.id.events);
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
				showList();
			}
		});
    }

	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
	   
		menu.add(0, MENU_PREV, 0, "Previous Day").setIcon(R.drawable.ic_menu_back);
	    menu.add(0, MENU_NEXT, 0, "Next Day").setIcon(R.drawable.ic_menu_forward);
	    menu.add(0, MENU_NOW, 0, "Now").setIcon(android.R.drawable.ic_menu_mylocation);
	    
	    SubMenu dayMenu = menu.addSubMenu("Day").setIcon(android.R.drawable.ic_menu_today);   
	    dayMenu.add(0, MENU_JUN1, 0, "Jun 1");
	    dayMenu.add(0, MENU_JUN2, 0, "Jun 2");
	    dayMenu.add(0, MENU_JUN3, 0, "Jun 3");
	    dayMenu.add(0, MENU_JUN4, 0, "Jun 4");
	    
	    menu.add(0, MENU_NOW, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
	    return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_NOW:
	        now();
	        return true;
	    case MENU_JUN1:
	    	setDay(JUN1);
	        return true;
    	case MENU_JUN2:
    		setDay(JUN2);
	        return true;
		case MENU_JUN3:
			setDay(JUN3);
    		return true;
		case MENU_JUN4:
			setDay(JUN4);
			return true;
		case MENU_PREV:
			previous();
			return true;
		case MENU_NEXT:
			next();
			return true;
	    }
	    return false;
	}
	
	/* sets the current day, filtering the list if need be */
	public void setDay(Date date) {
		if (isSameDay(mCurrentDate, date)) {
			// same day, just jump to current time
			mAdapter.now(date);
		} else {
			// different day, update the list
			mCurrentDate = date;
			mAdapter.filterDay(date);
			DateFormat formatter = new SimpleDateFormat("E, MMMM d");
			mDate.setText(formatter.format(mCurrentDate));
		} 
		
		// take user back to the listings if not already there 
		showList();
	}
	
	/**
	 * Jumps the user to right now in the event list:
	 * 
	 *    - if its before or after the conference, it shows the beginning
	 *      of day 1
	 *    - if its during the conference it will show the first event 
	 *      currently underway
	 */
	public void now(){
		Date now = new Date();
		if (now.before(JUN1) || now.after(JUN4)) {
			setDay(JUN1);
		} else {
			// use now, since it will have the time of day for 
			// jumping to the right time
			setDay(now);
		}
	}
	
	/**
	 * Jumps to the next day, if not already at the end
	 */
	public void next() {
		if (isSameDay(mCurrentDate, JUN1)) {
			setDay(JUN2);
		} else if (isSameDay(mCurrentDate, JUN2)) {
			setDay(JUN3);
		} else if (isSameDay(mCurrentDate, JUN3)) {
			setDay(JUN4);
		}
	}
	
	/**
	 * Jumps to the previous day if now already at the beginning
	 */
	public void previous() {
		if (isSameDay(mCurrentDate, JUN4)) {
			setDay(JUN3);
		} else if (isSameDay(mCurrentDate, JUN3)) {
			setDay(JUN2);
		} else if (isSameDay(mCurrentDate, JUN2)) {
			setDay(JUN1);
		}
	}
	
	/**
	 * Shows the event listing
	 */
	public void showList() {
		if (mDetail) {
			mFlipper.setInAnimation(mInLeft);
            mFlipper.setOutAnimation(mOutRight);
            mFlipper.showPrevious();
            mDetail=false;
		}
	}
	
	/**
	 * Loads the osbridge schedule from a combination of ICal and json data
	 */
	private void loadSchedule() {
		//XXX set date to a day that is definitely, not now.  This will cause it to update the list immediately.
		mCurrentDate = new Date(1900, 0, 0);
		InputStream is = null;
        URLConnection conn = null;
		try {
			URL url = new URL(SCHEDULE_URI);
		    conn = url.openConnection(); 
		    conn.setDoInput(true); 
		    conn.setUseCaches(false);
			is = conn.getInputStream();

			ICal calendar = new ICal(is);
			
			
			mAdapter = new EventAdapter(this, R.layout.listevent, calendar.getEvents());
	        mEvents.setAdapter(mAdapter);
			mEvents.setOnItemClickListener(new ListView.OnItemClickListener() {
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
                    mDetail = true;
				}
			});
            
			// parse the proposals json to get additional fields
			parseProposals(calendar);
			
			// always set the initial state to "now"
			now();
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
			int size = events.size();
			for(int i=0; i<size; i++){
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
		private ArrayList<Event> mItems;
		private ArrayList<Event> mFiltered;
		
		public EventAdapter(Context context, int textViewResourceId,
				ArrayList<Event> items) {
			super(context, textViewResourceId, items);
			this.mItems = items;
			filterDay(JUN1);
		}

		/**
		 * Filters the list to contain only events for the given date.
		 * @param date - date to filter by
		 */
		public void filterDay(Date date){
			ArrayList<Event> items = mItems;
			ArrayList<Event> filtered = new ArrayList<Event>();
			int size = mItems.size();
			for (int i=0; i<size; i++){
				Event event = items.get(i);
				if(isSameDay(date, event.start)){
					filtered.add(event);
				}
			}
			mFiltered = filtered; 
			notifyDataSetChanged();
			now(date);
		}
		
		/**
		 * sets the position to the current time
		 * @param date
		 */
		public void now(Date date) {
			ArrayList<Event> filtered = mFiltered;
			int size = filtered.size();
			for (int i=0; i<size; i++){
				Event event = filtered.get(i);
				if (event.end.after(date)) {
					mEvents.setSelection(i);
					return;
				}
			}
		}
		
		public int getCount(){
			return mFiltered.size();
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.listevent, null);
			}
			
			Event e = mFiltered.get(position);
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
					DateFormat formatter = new SimpleDateFormat("H:mm");
					time.setText(formatter.format(e.start) + "-" + formatter.format(e.end));
				}
			}
			return v;
		}
	}
	
	/**
	 * Checks if two dates are the same day
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }
 
	/**
	 * Checks if two calendars are the same day
	 * @param cal1
	 * @param cal2
	 * @return
	 */
	public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
	
}