package com.chteuchteu.lesjoiesdeletudiantinfo.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.chteuchteu.lesjoiesdeletudiantinfo.obj.Gif;
import com.chteuchteu.lesjoiesdeletudiantinfo.serv.NotificationService;
import com.chteuchteu.lesjoiesdeletudiantinfo.R;
import com.chteuchteu.lesjoiesdeletudiantinfo.hlpr.RSSReader;
import com.chteuchteu.lesjoiesdeletudiantinfo.hlpr.Util;
import com.google.analytics.tracking.android.EasyTracker;
import com.tjeannin.apprate.AppRate;

public class Activity_Main extends Activity {
	public String		rssUrl = "http://lesjoiesdeletudiantinfo.com/feed/";
	public String		countDownWS = "http://lesjoiesdeletudiantinfo.com/wp-admin/admin-ajax.php?action=getDeltaNext";
	private ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
	private static Activity 	a;
	public static List<Gif> 	gifs;
	public boolean		loaded;
	private MenuItem	notifs;
	private int			actionBarColor = Color.argb(200, 6, 124, 64);
	private boolean	notifsEnabled;
	public static int 	scrollY;
	private ListView 	lv_gifs;
	private CountDownTimer cdt;
	private LinearLayout countdown_container;
	private static int countdown_container_height = 120;
	
	public static boolean debug = true;
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.activity_main);
		
		lv_gifs = (ListView) findViewById(R.id.list);
		
		int contentPaddingTop = 0;
		int contentPaddingBottom = 0;
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setTitle(" Les Joies de l'Etudiant Info");
		actionBar.setBackgroundDrawable(new ColorDrawable(actionBarColor));
		final TypedArray styledAttributes = getApplicationContext().getTheme().obtainStyledAttributes(
				new int[] { android.R.attr.actionBarSize });
		contentPaddingTop += (int) styledAttributes.getDimension(0, 0);
		styledAttributes.recycle();
		
		countdown_container = (LinearLayout) findViewById(R.id.countdown_container);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int id = getResources().getIdentifier("config_enableTranslucentDecor", "bool", "android");
			if (id != 0 && getResources().getBoolean(id)) { // Translucent available
				Window w = getWindow();
				w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
				w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				LinearLayout notifBarBG = (LinearLayout) findViewById(R.id.kitkat_actionbar_notifs);
				notifBarBG.setBackgroundColor(actionBarColor);
				notifBarBG.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, getStatusBarHeight()));
				notifBarBG.setVisibility(View.VISIBLE);
				contentPaddingTop += getStatusBarHeight();
				contentPaddingBottom = 150;
			}
		}
		else
			findViewById(R.id.kitkat_actionbar_notifs).setVisibility(View.GONE);
		if (contentPaddingTop != 0) {
			((RelativeLayout.LayoutParams) countdown_container.getLayoutParams()).setMargins(0, contentPaddingTop, 0, 0);
			lv_gifs.setClipToPadding(false);
			lv_gifs.setPadding(0, contentPaddingTop, 0, contentPaddingBottom);
		}
		
		a = this;
		if (gifs == null) {
			gifs = new ArrayList<Gif>();
			loaded = false;
			
			Util.createLJDSYDirectory();
			getGifs();
			if (Util.removeUncompleteGifs(a, gifs))
				getGifs();
			Util.removeOldGifs(gifs);
		}
		
		if (Util.getPref(this, "first_disclaimer").equals("")) {
			Util.setPref(this, "first_disclaimer", "true");
			final LinearLayout l = (LinearLayout) findViewById(R.id.first_disclaimer);
			l.setVisibility(View.VISIBLE);
			findViewById(R.id.disclaimer_valider).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (notifsEnabled)
						enableNotifs();
					else
						disableNotifs();
					AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
					a.setDuration(300);
					a.setAnimationListener(new AnimationListener() {
						@Override public void onAnimationStart(Animation animation) { }
						@Override public void onAnimationRepeat(Animation animation) { }
						@Override public void onAnimationEnd(Animation animation) {
							l.setVisibility(View.GONE);
							l.setOnClickListener(null);
						}
					});
					l.startAnimation(a);
				}
			});
			final TextView tv1 = (TextView) findViewById(R.id.disclaimer_notifs);
			
			notifsEnabled = true;
			tv1.setText("[X] " + getText(R.string.first_disclaimer_notifs));
			
			tv1.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (notifsEnabled)
						tv1.setText("[   ] " + getText(R.string.first_disclaimer_notifs));
					else
						tv1.setText("[X] " + getText(R.string.first_disclaimer_notifs));
					notifsEnabled = !notifsEnabled;
				}
			});
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
		.setTitle(getString(R.string.vote_title))
		.setIcon(R.drawable.ic_launcher)
		.setMessage(getString(R.string.vote))
		.setPositiveButton(getString(R.string.vote_yes), null)
		.setNegativeButton(getString(R.string.vote_no), null)
		.setNeutralButton(getString(R.string.vote_notnow), null);
		new AppRate(this)
		.setCustomDialog(builder)
		.setMinDaysUntilPrompt(5)
		.setMinLaunchesUntilPrompt(5)
		.init();
		
		lv_gifs.post(new Runnable() {
			@Override
			public void run() {
				if (scrollY != 0)
					lv_gifs.setSelectionFromTop(scrollY, 0);
			}
		});
		
		launchUpdateIfNeeded();
		
		// Countdown
		setFont((TextView) findViewById(R.id.countdown_txt), "RobotoCondensed-Regular.ttf");
		setFont((TextView) findViewById(R.id.countdown_hh), "RobotoCondensed-Regular.ttf");
		setFont((TextView) findViewById(R.id.countdown_mm), "RobotoCondensed-Regular.ttf");
		setFont((TextView) findViewById(R.id.countdown_ss), "RobotoCondensed-Regular.ttf");
		
		if (cdt == null)
			new CountdownLauncher().execute();
	}
	
	private void enableNotifs() {
		Util.setPref(a, "notifs", "true");
		
		int minutes = 180;
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent i = new Intent(Activity_Main.this, NotificationService.class);
		PendingIntent pi = PendingIntent.getService(Activity_Main.this, 0, i, 0);
		am.cancel(pi);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + minutes*60*1000, minutes*60*1000, pi);
		if (notifs != null)
			notifs.setChecked(true);
		try {
			pi.send();
		} catch (CanceledException e) {	e.printStackTrace(); }
	}
	
	private void disableNotifs() {
		Util.setPref(a, "notifs", "false");
		if (notifs != null)
			notifs.setChecked(false);
	}
	
	private void saveLastViewed() {
		if (gifs != null && gifs.size() > 0)
			Util.setPref(a, "lastViewed", gifs.get(0).urlArticle);
	}
	
	@Override
	public void onBackPressed() {
		final LinearLayout l = (LinearLayout) findViewById(R.id.about);
		if (l.getVisibility() == View.VISIBLE) {
			AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
			a.setDuration(300);
			a.setAnimationListener(new AnimationListener() {
				@Override public void onAnimationStart(Animation animation) { }
				@Override public void onAnimationRepeat(Animation animation) { }
				@Override public void onAnimationEnd(Animation animation) {
					l.setVisibility(View.GONE);
					l.setOnClickListener(null);
				}
			});
			l.startAnimation(a);
		}
		else if (findViewById(R.id.first_disclaimer).getVisibility() == View.VISIBLE) { }
		else
			super.onBackPressed();
	}
	
	private void launchUpdateIfNeeded() {
		boolean letsFetch = false;
		if (!loaded) {
			String lastUpdate = Util.getPref(a, "lastGifsListUpdate");
			if (lastUpdate.equals("doitnow") || lastUpdate.equals(""))
				letsFetch = true;
			else {
				Date last = Util.stringToDate(Util.getPref(a, "lastGifsListUpdate"));
				long nbSecs = Util.getSecsDiff(last, new Date());
				long nbHours = nbSecs / 3600;
				if (nbHours > 12)
					letsFetch = true;
				else
					letsFetch = false;
			}
		}
		if (!loaded)
			getGifs();
		if (letsFetch)
			new parseFeed().execute();
	}
	
	public class parseFeed extends AsyncTask<String, Integer, Void> {
		boolean needsUpdate = false;
		ProgressBar pb;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pb = (ProgressBar) a.findViewById(R.id.pb);
			pb.setIndeterminate(true);
			pb.setProgress(0);
			pb.setMax(100);
			pb.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			
			if (findViewById(R.id.first_disclaimer).getVisibility() == View.VISIBLE)
				((TextView) findViewById(R.id.ascii_loading)).setText(progress[0] + "%");
			else {
				if (pb.getVisibility() == View.GONE)
					pb.setVisibility(View.VISIBLE);
				pb.setProgress(progress[0]);
				pb.setIndeterminate(false);
			}
		}
		
		public void manualPublishProgress(int n) {
			publishProgress(n);
		}
		
		@Override
		protected Void doInBackground(String... url) {
			List<Gif> l = RSSReader.parse(rssUrl, this);
			
			if (l.size() == 0)
				return null;
			if (gifs == null || (gifs != null && gifs.size() == 0) || (gifs != null && l.size() != gifs.size()) || (gifs != null && gifs.size() > 0 && !l.get(0).equals(gifs.get(0)))) {
				needsUpdate = true;
				gifs = new ArrayList<Gif>();
				for (int i=l.size()-1; i>=0; i--) {
					gifs.add(0, l.get(i));
				}
				Util.saveGifs(Activity_Main.this, gifs);
			}
			
			return null;
		}
		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(Void result) {
			pb.setVisibility(View.GONE);
			if (needsUpdate) {
				// Populate listview
				ListView l = (ListView) findViewById(R.id.list);
				list.clear();
				HashMap<String,String> item;
				for (Gif g : gifs) {
					if (g.isValide()) {
						item = new HashMap<String,String>();
						item.put("line1", g.nom);
						item.put("line2", g.date);
						list.add(item);
					}
				}
				
				int scrollY = l.getFirstVisiblePosition();
				View v = l.getChildAt(0);
				int top = (v == null) ? 0 : v.getTop();
				
				SimpleAdapter sa = new SimpleAdapter(Activity_Main.this, list, R.layout.list_item, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
				l.setAdapter(sa);
				
				l.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
						itemClick(view);
					}
				});
				
				l.setSelectionFromTop(scrollY, top);
				
				saveLastViewed();
			}
			Util.setPref(a, "lastGifsListUpdate", Util.dateToString(new Date()));
			loaded = true;
		}
	}
	
	public class CountdownLauncher extends AsyncTask<Void, Integer, Void> {
		private int nbSeconds;
		
		public CountdownLauncher() { }
		
		@Override
		protected void onPreExecute() { }
		
		@Override
		protected Void doInBackground(Void... arg0) {
			String source = "";
			try {
				URL adresse = new URL(countDownWS);
				BufferedReader in = new BufferedReader(new InputStreamReader(adresse.openStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					source += inputLine;
				}
				in.close();
				
				if (!source.equals(""))
					nbSeconds = Integer.parseInt(source);
			} catch (Exception e) { Log.e("", e.toString()); }
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (nbSeconds != 0) {
				countdown_container.setVisibility(View.INVISIBLE);
				final ViewTreeObserver observer = countdown_container.getViewTreeObserver();
				observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					@SuppressWarnings("deprecation")
					@Override
					public void onGlobalLayout() {
						countdown_container.setVisibility(View.VISIBLE);
						countdown_container_height = findViewById(R.id.countdown_container).getHeight();
						lv_gifs.setPadding(0, lv_gifs.getPaddingTop() + countdown_container_height, 0, lv_gifs.getPaddingBottom());
						observer.removeGlobalOnLayoutListener(this);
					}
				});
				
				if (lv_gifs.getFirstVisiblePosition() < 5) {
					lv_gifs.smoothScrollToPosition(0);
					lv_gifs.setSelectionFromTop(0, 0);
				}
				
				if (cdt != null)	cdt.cancel();
				cdt = new CountDownTimer(nbSeconds * 1000, 1000) {
					public void onTick(long millisUntilFinished) {
						int[] formatted = Util.getCountdownFromSeconds((int) millisUntilFinished / 1000);
						
						((TextView) findViewById(R.id.countdown_hh)).setText(formatted[0] + "");
						((TextView) findViewById(R.id.countdown_mm)).setText(Util.formatNumber(formatted[1]));
						((TextView) findViewById(R.id.countdown_ss)).setText(Util.formatNumber(formatted[2]));
					}
					
					public void onFinish() {
						Util.setPref(a, "lastGifsListUpdate", "doitnow");
						new parseFeed().execute("");
					}
				}
				.start();
			}
		}
	}
	
	private void itemClick(View view) {
		TextView label = (TextView) view.findViewById(R.id.line_a);
		Intent intent = new Intent(Activity_Main.this, Activity_Gif.class);
		intent.putExtra("name", label.getText().toString());
		scrollY = ((ListView) findViewById(R.id.list)).getFirstVisiblePosition();
		Gif g = Util.getGif(gifs, label.getText().toString());
		if (g != null) {
			intent.putExtra("url", g.urlGif);
			startActivity(intent);
			setTransition("rightToLeft");
		}
	}
	
	private void getGifs() {
		if (!Util.getPref(this, "gifs").equals("")) {
			List<Gif> li = Util.getGifs(this);
			
			if (li.size() > 0) {
				gifs = li;
				
				ListView l = (ListView) findViewById(R.id.list);
				list.clear();
				HashMap<String,String> item;
				for (Gif g : gifs) {
					if (g.isValide()) {
						item = new HashMap<String,String>();
						item.put("line1", g.nom);
						item.put("line2", g.date);
						list.add(item);
					}
				}
				SimpleAdapter sa = new SimpleAdapter(Activity_Main.this, list, R.layout.list_item, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
				l.setAdapter(sa);
				
				l.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
						itemClick(view);
					}
				});
				saveLastViewed();
				
				loaded = true;
			}
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				Util.setPref(this, "lastGifsListUpdate", "doitnow");
				new parseFeed().execute("");
				return true;
			case R.id.notifications:
				item.setChecked(!item.isChecked());
				if (item.isChecked()) enableNotifs();
				else disableNotifs();
				return true;
			case R.id.menu_about:
				final LinearLayout l = (LinearLayout) findViewById(R.id.about);
				if (l.getVisibility() == View.GONE) {
					setFont((ViewGroup) l, "Futura.ttf");
					l.setVisibility(View.VISIBLE);
					AlphaAnimation a = new AlphaAnimation(0.0f, 1.0f);
					a.setDuration(500);
					a.setFillAfter(true);
					a.setAnimationListener(new AnimationListener() {
						@Override public void onAnimationStart(Animation animation) { }
						@Override public void onAnimationRepeat(Animation animation) { }
						@Override public void onAnimationEnd(Animation animation) { }
					});
					l.startAnimation(a);
					l.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
							a.setDuration(300);
							a.setAnimationListener(new AnimationListener() {
								@Override public void onAnimationStart(Animation animation) { }
								@Override public void onAnimationRepeat(Animation animation) { }
								@Override public void onAnimationEnd(Animation animation) {
									l.setVisibility(View.GONE);
									l.setOnClickListener(null);
								}
							});
							l.startAnimation(a);
						}
					});
				} else {
					AlphaAnimation a = new AlphaAnimation(1.0f, 0.0f);
					a.setDuration(300);
					a.setAnimationListener(new AnimationListener() {
						@Override public void onAnimationStart(Animation animation) { }
						@Override public void onAnimationRepeat(Animation animation) { }
						@Override public void onAnimationEnd(Animation animation) {
							l.setVisibility(View.GONE);
							l.setOnClickListener(null);
						}
					});
					l.startAnimation(a);
				}
				return true;
			case R.id.menu_clear_cache:
				Util.clearCache(this);
				return true;
			case R.id.menu_contact:
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://lesjoiesdeletudiantinfo.com/contact/"));
				startActivity(browserIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	public void setFont(ViewGroup g, String font) {
		Typeface mFont = Typeface.createFromAsset(getAssets(), font);
		setFont(g, mFont);
	}
	public void setFont(TextView tv, String font) {
		Typeface mFont = Typeface.createFromAsset(getAssets(), font);
		tv.setTypeface(mFont);
	}
	public void setFont(ViewGroup group, Typeface font) {
		int count = group.getChildCount();
		View v;
		for (int i = 0; i < count; i++) {
			v = group.getChildAt(i);
			if (v instanceof TextView || v instanceof EditText || v instanceof Button) {
				((TextView) v).setTypeface(font);
			} else if (v instanceof ViewGroup)
				setFont((ViewGroup) v, font);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		notifs = menu.findItem(R.id.notifications);
		if (Util.getPref(this, "notifs").equals("true"))
			notifs.setChecked(true);
		return true;
	}
	
	public int getStatusBarHeight() {
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0)
			return getResources().getDimensionPixelSize(resourceId);
		return 0;
	}
	
	public void setTransition(String level) {
		if (level.equals("rightToLeft"))
			overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
		else if (level.equals("leftToRight"))
			overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!debug)
			EasyTracker.getInstance(this).activityStop(this);
	}
}