package org.osb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;


public class ScheduleActivity extends AbstractActivity {

	
	private static final Date JUN1 = new Date(110, 5, 1);
	private static final Date JUN2 = new Date(110, 5, 2);
	private static final Date JUN3 = new Date(110, 5, 3);
	private static final Date JUN4 = new Date(110, 5, 4);

	private static final int MENU_JUN1 = 1;
	private static final int MENU_JUN2 = 2;
	private static final int MENU_JUN3 = 3;
	private static final int MENU_JUN4 = 4;
	private static final int MENU_NEXT = 5;
	private static final int MENU_PREV = 6;
	private static final int MENU_ABOUT = 7;
	private static final int MENU_NOW = 8;
	private static final int MENU_REFRESH = 9;
	
	// state
	Date mCurrentDate;
	TextView mDate;
	boolean mDetail = false;
	Handler mHandler; 
	
	// general conference data
	Conference mConference;
	Date[] mDates;
	
	// session list
	EventAdapter mAdapter;
	ListView mEvents;
	
	// screen animation
	ViewFlipper mFlipper;
	Animation mInLeft;
    Animation mInRight;
    Animation mOutLeft;
    Animation mOutRight;
	
    // session details
    Event mEvent = null;
    HashMap<Integer, Speaker> mSpeakers;
    View mHeader;
    TextView mTitle;
    TextView mTime;
    TextView mLocation;
    View mTimeLocation;
    TextView mSpeaker;
    ScrollView mDescriptionScroller;
    TextView mDescription;
    ImageView mMapImage;
    LinearLayout mBio;
    
    // session detail actions
    Button mFoursquare;
    Button mShare;
    Button mMap;
    Button mShowDescription;
    Button mShowBio;
    
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mHandler = new Handler();
        
        mSpeakers = new HashMap<Integer, Speaker>();
        
        mDate = (TextView) findViewById(R.id.date);
        mEvents = (ListView) findViewById(R.id.events);
        mFlipper = (ViewFlipper) findViewById(R.id.flipper);
        Context context = getApplicationContext();
        mInLeft = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
        mInRight = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        mOutLeft = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);
        mOutRight = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
        
        // grab views for details
        View detail = findViewById(R.id.detail);
        mHeader = findViewById(R.id.detail_header);
        mSpeaker = (TextView) findViewById(R.id.speaker);
        mTitle = (TextView) detail.findViewById(R.id.title);
        mTimeLocation = detail.findViewById(R.id.time_location);
        mTime = (TextView) detail.findViewById(R.id.time);
        mLocation = (TextView) detail.findViewById(R.id.location);
        mDescription = (TextView) detail.findViewById(R.id.description);
        mDescriptionScroller = (ScrollView) detail.findViewById(R.id.description_scroller);
        mMapImage = (ImageView) detail.findViewById(R.id.map_image);
        mBio = (LinearLayout) detail.findViewById(R.id.bio);
        
        // detail action buttons 
        mFoursquare = (Button) findViewById(R.id.foursquare);
        mShare = (Button) findViewById(R.id.share);
        mMap = (Button) findViewById(R.id.map);
        mShowDescription = (Button) findViewById(R.id.show_description);
        mShowBio = (Button) findViewById(R.id.show_bio);
        
        mEvents.setOnItemClickListener(new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
				Object item = mAdapter.mFiltered.get(position);
				if (item instanceof Date) {
					return;// ignore clicks on the dates
				}
				Event event = (Event) item;
				Track track = mConference.tracks.get(event.track);
				Location location = mConference.locations.get(event.location);
				mHeader.setBackgroundColor(track.color);
				mTitle.setText(event.title);
				mLocation.setText(location.name);
				DateFormat startFormat = new SimpleDateFormat("E, h:mm");
				DateFormat endFormat = new SimpleDateFormat("h:mm a");
				String timeString = startFormat.format(event.start) + " - " + endFormat.format(event.end);
				mTime.setText(timeString);
				mSpeaker.setText(event.speakers);
				mTimeLocation.setBackgroundColor(track.color);
				mDescription.setText(event.description);
				show_description();
				mDescriptionScroller.scrollTo(0, 0);
				mFlipper.setInAnimation(mInRight);
                mFlipper.setOutAnimation(mOutLeft);
                mFlipper.showNext();
                mEvent = event;
                mDetail = true;
			}
		});
        
        mShowDescription.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				show_description();
			}
        });
        
        mMap.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				mMapImage.setImageResource(getMapResource(mLocation.getText()));
				mDescription.setVisibility(View.GONE);
				mBio.setVisibility(View.GONE);
				mMapImage.setVisibility(View.VISIBLE);
			}
			
			private int getMapResource(CharSequence roomName) {
				if (roomName.equals("Hawthorne")) {
					return R.drawable.hawthorne;
				} else if (roomName.equals("Burnside")) {
					return R.drawable.burnside;
				} else if (roomName.equals("St. Johns")) {
					return R.drawable.st_johns;
				} else if (roomName.equals("Broadway")) {
					return R.drawable.broadway;
				} else if (roomName.equals("Morrison")) {
					return R.drawable.morrison;
				} else if (roomName.equals("Fremont")) {
					return R.drawable.fremont;
				} else if (roomName.equals("Steel")) {
					return R.drawable.steel;
				}
				return R.drawable.icon_footer;
			}
        });
        
        mShowBio.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				
				mBio.removeAllViews();
				Integer[] speaker_ids = mEvent.speaker_ids;
				if (speaker_ids != null) {
					for (int i=0; i<speaker_ids.length; i++) {
							View view = loadBioView(speaker_ids[i]);
							if (view != null) {
								if (i>0){
									view.setPadding(0, 30, 0, 0);
								}
								mBio.addView(view);
							}
					}
				
					mDescription.setVisibility(View.GONE);
					mMapImage.setVisibility(View.GONE);
					mBio.setVisibility(View.VISIBLE);
				}
			}
			
			/**
			 * loads a view populated with the speakers info
			 * @param id
			 * @return
			 */
			private View loadBioView(int sid) {
				Integer id = new Integer(sid);
				Speaker speaker = null;
				View view = null;
				// check memory to see if speaker had already been loaded
				// else load the speaker from persistent storage
				if (mSpeakers.containsKey(id)){
					speaker = mSpeakers.get(id);
				} else {
					speaker = getDataService().getSpeaker(id, false);
					mSpeakers.put(id, speaker);
				}
				
				// create view
				if (speaker != null) {
					LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					view = vi.inflate(R.layout.bio, null);
					TextView name = (TextView) view.findViewById(R.id.name);
					name.setText(speaker.name);
					TextView biography = (TextView) view.findViewById(R.id.biography);
					biography.setText(speaker.biography);
					
					String twitter = speaker.twitter;
					if (twitter != null && twitter != ""  && twitter != "null"){
						TextView text = (TextView) view.findViewById(R.id.twitter);
						text.setText(twitter);
						View parent = (View) text.getParent();
						parent.setVisibility(View.VISIBLE);
					}
					
					String website = speaker.website;
					if (website != null && website != "" && website != "null"){
						TextView text = (TextView) view.findViewById(R.id.website);
						text.setText(speaker.website);
						View parent = (View) text.getParent();
						parent.setVisibility(View.VISIBLE);
					}
					
					String blog = speaker.blog;
					if (blog != null && blog != "" && blog != "null"){
						TextView text = (TextView) view.findViewById(R.id.blog);
						text.setText(speaker.blog);
						View parent = (View) text.getParent();
						parent.setVisibility(View.VISIBLE);
					}
					
					if (speaker.affiliation != null){
						TextView text = (TextView) view.findViewById(R.id.affiliation);
						text.setText(speaker.affiliation);
					}
					
					String identica = speaker.identica;
					if (identica != null && identica != "" && identica != "null"){
						TextView text = (TextView) view.findViewById(R.id.identica);
						text.setText(speaker.identica);
						View parent = (View) text.getParent();
						parent.setVisibility(View.VISIBLE);
					}
				}
				
				return view;
			}

        });
        
        mFoursquare.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				String url = mapRoomNameToFqUrl((mLocation).getText().toString());
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
			}

			private String mapRoomNameToFqUrl(String roomName) {
				String vid = "";
				if (roomName.equals("Hawthorne")) {
					vid = "4281683";
				} else if (roomName.equals("Burnside")) {
					vid = "4281826";
				} else if (roomName.equals("St. Johns")) {
					vid = "4281970";
				} else if (roomName.equals("Broadway")) {
					vid = "4281777";
				} else if (roomName.equals("Morrison")) {
					vid = "4281923";
				} else if (roomName.equals("Fremont")) {
					vid = "4281874";
				} else if (roomName.equals("Steel")) {
					vid = "4282004";
				}
				return "http://m.foursquare.com/checkin?vid="+vid;
			}
		});
        
        mShare.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				Intent intent = new Intent(android.content.Intent.ACTION_SEND);
				intent.setType("text/plain");
				Resources r = getApplicationContext().getResources();
				intent.putExtra(Intent.EXTRA_SUBJECT, r.getString(R.string.share_subject));
				intent.putExtra(Intent.EXTRA_TEXT, r.getString(R.string.share_text) + mTitle.getText() + r.getString(R.string.share_text2));
				startActivity(Intent.createChooser(intent, "Share"));
			}
        });
        
        // spawn loading into separate thread
        mHandler.post(new Runnable() {
		    public void run() {
		    	loadSchedule(false);
		    	now();
		    	}
		});
    }
	
	/**
	 * Shows the session description, hides all other subviews
	 */
	private void show_description(){
		mMapImage.setVisibility(View.GONE);
		mBio.setVisibility(View.GONE);
		mDescription.setVisibility(View.VISIBLE);
	}
	
	/**
	 * overridden to hook back button when on the detail page
	 */
	public boolean onKeyDown(int keyCode, KeyEvent  event){
		if (mDetail && keyCode == KeyEvent.KEYCODE_BACK){
			showList();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_PREV, 0, "Previous Day").setIcon(R.drawable.ic_menu_back);
		SubMenu dayMenu = menu.addSubMenu("Day").setIcon(android.R.drawable.ic_menu_today);
	    dayMenu.add(0, MENU_JUN1, 0, "Tuesday, June 1");
	    dayMenu.add(0, MENU_JUN2, 0, "Wednesday, June 2");
	    dayMenu.add(0, MENU_JUN3, 0, "Thursday, June 3");
	    dayMenu.add(0, MENU_JUN4, 0, "Friday, June 4");
		menu.add(0, MENU_NEXT, 0, "Next Day").setIcon(R.drawable.ic_menu_forward);
	    menu.add(0, MENU_NOW, 0, "Now").setIcon(android.R.drawable.ic_menu_mylocation);
	    menu.add(0, MENU_REFRESH, 0, "Refresh").setIcon(R.drawable.ic_menu_refresh);
	    menu.add(0, MENU_ABOUT, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
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
		case MENU_ABOUT:
			showDialog(0);
			return true;
		case MENU_REFRESH:
			mHandler.post(new Runnable() {
			    public void run() { 
			    	loadSchedule(true);
			    	now();
			    	}
			}); 
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
		if (now.before(mDates[0]) || now.after(mConference.end)) {
			setDay(mDates[0]);
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
		try {
			if (!isSameDay(mCurrentDate, mConference.end)) {
				mCurrentDate.setDate(mCurrentDate.getDate()+1);
				mCurrentDate.setHours(0);
				mCurrentDate.setMinutes(0);
				setDay(mCurrentDate);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Jumps to the previous day if now already at the beginning
	 */
	public void previous() {
		if (!isSameDay(mCurrentDate, mConference.start)) {
			mCurrentDate.setDate(mCurrentDate.getDate()-1);
			mCurrentDate.setHours(0);
			mCurrentDate.setMinutes(0);
			setDay(mCurrentDate);
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
	 * @param force - force reload
	 */
	private void loadSchedule(boolean force) {
		System.out.println("================Schedule================");
		//XXX set date to a day that is definitely, not now.  
		//    This will cause it to update the list immediately.
		mCurrentDate = new Date(1900, 0, 0);
		DataService service = getDataService();
		mConference  = service.getConference(force);
		try {
		mDates = mConference.getDates();
		ICal calendar = new ICal();
		
			Schedule schedule = service.getSchedule(force);
		
		calendar.setEvents(schedule.events);
		mAdapter = new EventAdapter(this, R.layout.listevent, calendar.getEvents());
        mEvents.setAdapter(mAdapter);
		} catch (Exception e) {
			e.printStackTrace();
		}
        System.out.println("----------------Schedule----------------");
	}

	
	protected Dialog onCreateDialog(int id){
        Context context = getApplicationContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.about, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About");
        builder.setCancelable(true);
        builder.setView(v);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        final AlertDialog alert = builder.create();
        return alert;
    }	
	
	/**
	 * EventAdapter used for displaying a list of events
	 *
	 */
	private class EventAdapter extends ArrayAdapter<Event> {
		private List<Event> mItems;
		private List<Object> mFiltered;
		
		public EventAdapter(Context context, int textViewResourceId,
				List<Event> items) {
			super(context, textViewResourceId, items);
			this.mItems = items;
			mFiltered = new ArrayList<Object>();
		}

		/**
		 * Filters the list to contain only events for the given date.
		 * @param date - date to filter by
		 */
		public void filterDay(Date date){
			List<Event> items = mItems;
			List<Object> filtered = new ArrayList<Object>();
			int size = mItems.size();
			Date currentStart = null;
			for (int i=0; i<size; i++){
				Event event = items.get(i);
				if(isSameDay(date, event.start)){
					if(currentStart == null || event.start.after(currentStart)) {
						currentStart = event.start;
						filtered.add(currentStart);
					}
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
			List<Object> filtered = mFiltered;
			int size = filtered.size();
			for (int i=0; i<size; i++){
				Object item = filtered.get(i);
				
				// find either the first session that hasn't ended yet
				// or the first time marker that hasn't occured yet.
				if (item instanceof Date ){
					Date slot = (Date) item;
					if (date.before(slot)) {
						mEvents.setSelection(i);
						return;
					}
				} else {
					Event event = (Event) item;
					if (event.end.after(date)) {
						// should display the time marker instead of the
						// session
						mEvents.setSelection(i-1);
						return;
					}
				}
			}
			
			// no current event was found, jump to the next day
			next();
		}
		
		public int getCount(){
			return mFiltered.size();
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			Object item = mFiltered.get(position);
			if (item instanceof Date) {
				Date date = (Date)item;
				v = vi.inflate(R.layout.list_slot, null);
				TextView time = (TextView) v.findViewById(R.id.time);
				DateFormat formatter = new SimpleDateFormat("h:mm a");
				time.setText(formatter.format(date));
			} else {
				Event e = (Event) item;
				v = vi.inflate(R.layout.listevent, null);
				if (e != null) {
					TextView title = (TextView) v.findViewById(R.id.title);
					TextView locationView = (TextView) v.findViewById(R.id.location);
					TextView time = (TextView) v.findViewById(R.id.time);
					if (title != null) {
						title.setText(e.title);
					}
					if (e.location != -1) {
						Location location = mConference.locations.get(e.location);
						locationView.setText(location.name);
					}
					if (time != null) {
						DateFormat formatter = new SimpleDateFormat("h:mm");
						time.setText(formatter.format(e.start) + "-" + formatter.format(e.end));
					}
					if (e.track != -1) {
						TextView track_view = (TextView) v.findViewById(R.id.track);
						Track track = mConference.tracks.get(e.track);
						track_view.setTextColor(track.color);
						track_view.setText(track.name);
					}
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