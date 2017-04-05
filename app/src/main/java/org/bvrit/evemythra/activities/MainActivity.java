package org.bvrit.evemythra.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.R;
import org.bvrit.evemythra.api.Urls;
import org.bvrit.evemythra.data.Event;
import org.bvrit.evemythra.data.EventDates;
import org.bvrit.evemythra.data.Microlocation;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.data.SessionSpeakersMapping;
import org.bvrit.evemythra.data.Speaker;
import org.bvrit.evemythra.data.Sponsor;
import org.bvrit.evemythra.data.Track;
import org.bvrit.evemythra.databinding.ActivityMainBinding;
import org.bvrit.evemythra.dbutils.DataDownloadManager;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.CounterEvent;
import org.bvrit.evemythra.events.DataDownloadEvent;
import org.bvrit.evemythra.events.DownloadEvent;
import org.bvrit.evemythra.events.EventDownloadEvent;
import org.bvrit.evemythra.events.JsonReadEvent;
import org.bvrit.evemythra.events.MicrolocationDownloadEvent;
import org.bvrit.evemythra.events.NoInternetEvent;
import org.bvrit.evemythra.events.RefreshUiEvent;
import org.bvrit.evemythra.events.RetrofitError;
import org.bvrit.evemythra.events.RetrofitResponseEvent;
import org.bvrit.evemythra.events.SessionDownloadEvent;
import org.bvrit.evemythra.events.ShowNetworkDialogEvent;
import org.bvrit.evemythra.events.SpeakerDownloadEvent;
import org.bvrit.evemythra.events.SponsorDownloadEvent;
import org.bvrit.evemythra.events.TracksDownloadEvent;
import org.bvrit.evemythra.fragments.BookmarksFragment;
import org.bvrit.evemythra.fragments.LocationsFragment;
import org.bvrit.evemythra.fragments.ScheduleFragment;
import org.bvrit.evemythra.fragments.SpeakersListFragment;
import org.bvrit.evemythra.fragments.SponsorsFragment;
import org.bvrit.evemythra.fragments.TracksFragment;
import org.bvrit.evemythra.utils.CommonTaskLoop;
import org.bvrit.evemythra.utils.ConstantStrings;
import org.bvrit.evemythra.utils.ISO8601Date;
import org.bvrit.evemythra.utils.NetworkUtils;
import org.bvrit.evemythra.utils.SmoothActionBarDrawerToggle;
import org.bvrit.evemythra.views.CustomTabsSpan;
import org.bvrit.evemythra.widget.DialogFactory;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {


    private static final String COUNTER_TAG = "Donecounter";

    private final static String STATE_FRAGMENT = "stateFragment";

    private static final String NAV_ITEM = "navItem";

    private static final String BOOKMARK = "bookmarks";

    private final String FRAGMENT_TAG_TRACKS = "FTAGT";

    private final String FRAGMENT_TAG_REST = "FTAGR";


    ActivityMainBinding binding;

    private SharedPreferences sharedPreferences;
    private int counter;
    private boolean atHome = true;
    private boolean backPressedOnce = false;
    private int eventsDone;
    private int currentMenuItemId;
    private SmoothActionBarDrawerToggle smoothActionBarToggle;
    private boolean customTabsSupported;
    private CustomTabsServiceConnection customTabsServiceConnection;
    private CustomTabsClient customTabsClient;
    private Runnable runnable;
    private Handler handler;
    private long timer = 2000;
    private int eventId;
    private String navImageUrl;

    public static Intent createLaunchFragmentIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .putExtra(NAV_ITEM, BOOKMARK);
    }

    public static void getDaysBetweenDates(Date startdate, Date enddate) {
        ArrayList<String> dates = new ArrayList<>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startdate);

        while (calendar.getTime().before(enddate)) {
            Date result = calendar.getTime();
            Calendar calendar1 = Calendar.getInstance();
            calendar1.setTime(result);
            dates.add(new EventDates(ISO8601Date.dateFromCalendar(calendar1)).generateSql());
            calendar.add(Calendar.DATE, 1);
        }

        DbSingleton.getInstance().insertQueries(dates);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setTheme(R.style.AppTheme_NoActionBar_MainTheme);

        counter = 0;
        eventsDone = 0;
        ButterKnife.setDebug(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putInt(ConstantStrings.SESSION_MAP_ID, -1).apply();

        Bundle b = getIntent().getExtras();
        eventId = b.getInt("eventId");
        navImageUrl = b.getString("navImageUrl");
        setUpToolbar();
        setUpNavDrawer();
        setUpProgressBar();
        setUpCustomTab();
        if (NetworkUtils.haveNetworkConnection(this) && NetworkUtils.isActiveInternetPresent()) {
            DbSingleton.getInstance().clearDatabase();
            EventBus.getDefault().post(new DataDownloadEvent(eventId));
        } else {
            downloadFromAssets();
        }
        if (savedInstanceState == null) {
            currentMenuItemId = R.id.nav_tracks;
        } else {
            currentMenuItemId = savedInstanceState.getInt(STATE_FRAGMENT);
        }

        if (getIntent().hasExtra(NAV_ITEM) && getIntent().getStringExtra(NAV_ITEM).equalsIgnoreCase(BOOKMARK)) {
            currentMenuItemId = R.id.nav_bookmarks;
        }

        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_TRACKS) == null && getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_REST) == null) {
            doMenuAction(currentMenuItemId);
        }
    }


    private void setUpCustomTab() {
        Intent customTabIntent = new Intent("android.support.customtabs.action.CustomTabsService");
        customTabIntent.setPackage("com.android.chrome");
        customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                customTabsClient = client;
                customTabsClient.warmup(0L);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                //initially left empty
            }
        };
        customTabsSupported = bindService(customTabIntent, customTabsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_FRAGMENT, currentMenuItemId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentMenuItemId = savedInstanceState.getInt(STATE_FRAGMENT);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        setupDrawerContent(binding.navView);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
        return true;
    }

    private void setUpToolbar() {
        if (binding.toolbar != null) {
            setSupportActionBar(binding.toolbar);
        }
    }

    private void setUpProgressBar() {
        ProgressBar downloadProgress = (ProgressBar) findViewById(R.id.progress);
        downloadProgress.setVisibility(View.VISIBLE);
        downloadProgress.setIndeterminate(true);
    }

    private void setUpNavDrawer() {
        if (binding.toolbar != null) {
            final ActionBar ab = getSupportActionBar();
            if (ab != null) {
                smoothActionBarToggle = new SmoothActionBarDrawerToggle(this,
                        binding.drawer, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                ab.setDisplayHomeAsUpEnabled(true);
            }

            binding.drawer.addDrawerListener(smoothActionBarToggle);
            smoothActionBarToggle.syncState();
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            View headerView = navigationView.getHeaderView(0);
            ImageView imageView = (ImageView) headerView.findViewById(R.id.headerDrawer);
            Picasso.with(this).load(navImageUrl).placeholder(R.mipmap.ic_launcher).into(imageView);

        }
    }

    private void syncComplete() {
        binding.progress.setVisibility(View.GONE);
        Event currentEvent = DbSingleton.getInstance().getEventDetails();
        if (currentEvent != null)
            getDaysBetweenDates(ISO8601Date.getDateObject(currentEvent.getStart()), ISO8601Date.getDateObject(currentEvent.getEnd()));

        EventBus bus = OpenEventApp.getEventBus();
        bus.post(new RefreshUiEvent());
        DbSingleton dbSingleton = DbSingleton.getInstance();
        try {
            if (!(dbSingleton.getEventDetails().getLogo().isEmpty())) {
                ImageView headerDrawer = (ImageView) findViewById(R.id.headerDrawer);
                Picasso.with(getApplicationContext()).load(dbSingleton.getEventDetails().getLogo()).into(headerDrawer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Snackbar.make(binding.layoutMain, getString(R.string.download_complete), Snackbar.LENGTH_SHORT).show();
        Timber.d("Download done");
    }

    private void downloadFailed(final DownloadEvent event) {
        binding.progress.setVisibility(View.GONE);
        Snackbar.make(binding.layoutMain, getString(R.string.download_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (event == null) {
                    Timber.d("no internet.");
                    OpenEventApp.postEventOnUIThread(new DataDownloadEvent(eventId));
                } else {
                    Timber.tag(COUNTER_TAG).d(event.getClass().getSimpleName());
                    OpenEventApp.postEventOnUIThread(event);
                }
            }
        }).show();

    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        int id = menuItem.getItemId();
                        doMenuAction(id);
                        DbSingleton dbSingleton = DbSingleton.getInstance();
                        if (id == R.id.nav_bookmarks && dbSingleton.isBookmarksTableEmpty()) {
                            return false;
                        }
                        // to ensure bookmarks is not shown clicked if at all there are no bookmarks
                        return true;
                    }
                });
    }

    private void doMenuAction(int menuItemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment;
        Bundle b = new Bundle();
        b.putInt("eventId", eventId);
        addShadowToAppBar(true);
        switch (menuItemId) {
            case R.id.nav_home:
                finish();
            case R.id.nav_tracks:
                atHome = true;
                fragment = new TracksFragment();
                fragment.setArguments(b);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment, FRAGMENT_TAG_TRACKS).commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_tracks);
                }
                binding.appbar.setExpanded(true, true);
                break;
            case R.id.nav_schedule:
                atHome = false;
                fragment = new ScheduleFragment();
                fragment.setArguments(b);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment, FRAGMENT_TAG_REST).commit();
                addShadowToAppBar(false);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_schedule);
                }
                binding.appbar.setExpanded(true, true);
                break;
            case R.id.nav_bookmarks:
                fragment = new BookmarksFragment();
                fragment.setArguments(b);
                DbSingleton dbSingleton = DbSingleton.getInstance();
                if (!dbSingleton.isBookmarksTableEmpty()) {
                    atHome = false;
                    fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, fragment, FRAGMENT_TAG_REST).commit();
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(R.string.menu_bookmarks);
                    }
                    binding.appbar.setExpanded(true, true);
                } else {
                    DialogFactory.createSimpleActionDialog(this, R.string.bookmarks, R.string.empty_list, null).show();
                    if (currentMenuItemId == R.id.nav_schedule) addShadowToAppBar(false);
                }
                break;
            case R.id.nav_speakers:
                atHome = false;
                fragment = new SpeakersListFragment();
                fragment.setArguments(b);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment, FRAGMENT_TAG_REST).commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_speakers);
                }
                binding.appbar.setExpanded(true, true);
                break;
            case R.id.nav_sponsors:
                atHome = false;
                fragment = new SponsorsFragment();
                fragment.setArguments(b);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment, FRAGMENT_TAG_REST).commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_sponsor);
                }
                binding.appbar.setExpanded(true, true);
                break;
            case R.id.nav_locations:
                atHome = false;
                fragment = new LocationsFragment();
                fragment.setArguments(b);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment, FRAGMENT_TAG_REST).commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_locations);
                }
                binding.appbar.setExpanded(true, true);
                break;
            case R.id.nav_map:
                atHome = false;

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                fragmentTransaction.replace(R.id.content_frame,
                        ((OpenEventApp) getApplication())
                                .getMapModuleFactory()
                                .provideMapModule()
                                .provideMapFragment(), FRAGMENT_TAG_REST).commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_map);
                }
                binding.appbar.setExpanded(true, true);
                break;
            case R.id.nav_settings:
                final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                smoothActionBarToggle.runWhenIdle(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }
                });
                break;
            case R.id.nav_share:
                try {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            String.format(getString(R.string.whatsapp_promo_msg_template),
                                    String.format(getString(R.string.app_share_url), getPackageName())));
                    startActivity(shareIntent);
                } catch (Exception e) {
                    Snackbar.make(binding.layoutMain, getString(R.string.error_msg_retry), Snackbar.LENGTH_SHORT).show();
                }
                break;
            case R.id.nav_about:
                final AlertDialog aboutUs = new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.about_text)
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                aboutUs.show();
                ((TextView) aboutUs.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                final TextView aboutUsTV = (TextView) aboutUs.findViewById(android.R.id.message);
                aboutUsTV.setMovementMethod(LinkMovementMethod.getInstance());
                if (customTabsSupported) {
                    SpannableString welcomeAlertSpannable = new SpannableString(aboutUsTV.getText());
                    URLSpan[] spans = welcomeAlertSpannable.getSpans(0, welcomeAlertSpannable.length(), URLSpan.class);
                    for (URLSpan span : spans) {
                        CustomTabsSpan newSpan = new CustomTabsSpan(span.getURL(), getApplicationContext(), this,
                                customTabsClient.newSession(new CustomTabsCallback()));
                        welcomeAlertSpannable.setSpan(newSpan, welcomeAlertSpannable.getSpanStart(span),
                                welcomeAlertSpannable.getSpanEnd(span),
                                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                        welcomeAlertSpannable.removeSpan(span);
                    }
                    aboutUsTV.setText(welcomeAlertSpannable);
                }
                break;
        }
        currentMenuItemId = menuItemId;
        binding.drawer.closeDrawers();
    }

/*
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawer(GravityCompat.START);
        } else if (atHome) {
            if (backPressedOnce) {
                super.onBackPressed();
            } else {
                backPressedOnce = true;
                Snackbar snackbar = Snackbar.make(binding.layoutMain, R.string.press_back_again, 2000);
                snackbar.show();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        backPressedOnce = false;
                    }
                };
                handler = new Handler();
                handler.postDelayed(runnable, timer);
            }
        } else {
            atHome = true;
            fragmentManager.beginTransaction().replace(R.id.content_frame, new TracksFragment(), FRAGMENT_TAG_TRACKS).commit();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.menu_tracks);
            }
            binding.navView.setCheckedItem(R.id.nav_tracks);
        }
    }
*/

    public void addShadowToAppBar(boolean addShadow) {
        if (addShadow) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.appbar.setElevation(12);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.appbar.setElevation(0);
            }
        }
    }

    public void showErrorDialog(String errorType, String errorDesc) {
        binding.progress.setVisibility(View.GONE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error)
                .setMessage(errorType + ": " + errorDesc)
                .setNeutralButton(android.R.string.ok, null)
                .create();
        builder.show();
    }

    //Subscribe EVENT
    @Subscribe
    public void onCounterReceiver(CounterEvent event) {
        counter = event.getRequestsCount();
        Timber.tag(COUNTER_TAG).d(counter + " counter");
        if (counter == 0) {
            syncComplete();
        }
    }

    @Subscribe
    public void onTracksDownloadDone(TracksDownloadEvent event) {
        if (event.isState()) {
            eventsDone++;
            Timber.tag(COUNTER_TAG).d(eventsDone + " tracks " + counter);
            if (counter == eventsDone) {
                syncComplete();
            }
        } else {
            downloadFailed(event);
        }
    }

    @Subscribe
    public void onSponsorsDownloadDone(SponsorDownloadEvent event) {
        if (event.isState()) {
            eventsDone++;
            Timber.tag(COUNTER_TAG).d(eventsDone + " sponsors " + counter);
            if (counter == eventsDone) {
                syncComplete();
            }
        } else {

            downloadFailed(event);
        }
    }

    @Subscribe
    public void onSpeakersDownloadDone(SpeakerDownloadEvent event) {
        if (event.isState()) {
            eventsDone++;
            Timber.tag(COUNTER_TAG).d(eventsDone + " speakers " + counter);
            if (counter == eventsDone) {
                syncComplete();
            }
        } else {

            downloadFailed(event);
        }
    }

    @Subscribe
    public void onSessionDownloadDone(SessionDownloadEvent event) {
        if (event.isState()) {
            eventsDone++;
            Timber.tag(COUNTER_TAG).d(eventsDone + " session " + counter);
            if (counter == eventsDone) {
                syncComplete();
            }
        } else {

            downloadFailed(event);
        }
    }

    @Subscribe
    public void noInternet(NoInternetEvent event) {
        downloadFailed(null);
    }

    @Subscribe
    public void onEventsDownloadDone(EventDownloadEvent event) {
        if (event.isState()) {
            eventsDone++;
            Timber.tag(COUNTER_TAG).d(eventsDone + " events " + counter);
            if (counter == eventsDone) {
                syncComplete();
            }
        } else {

            downloadFailed(event);
        }
    }

    @Subscribe
    public void onMicrolocationsDownloadDone(MicrolocationDownloadEvent event) {
        if (event.isState()) {
            eventsDone++;
            Timber.tag(COUNTER_TAG).d(eventsDone + " microlocation " + counter);
            if (counter == eventsDone) {
                syncComplete();
            }
        } else {

            downloadFailed(event);
        }

    }

    @Subscribe
    public void showNetworkDialog(ShowNetworkDialogEvent event) {
        binding.progress.setVisibility(View.GONE);
        DialogFactory.createSimpleActionDialog(this,
                R.string.net_unavailable,
                R.string.turn_on,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent setNetworkIntent = new Intent(Settings.ACTION_SETTINGS);
                        startActivity(setNetworkIntent);
                    }
                }).show();
    }

    @Subscribe
    public void downloadData(DataDownloadEvent event) {
        if (Urls.getBaseUrl().equals(Urls.INVALID_LINK)) {
            showErrorDialog("Invalid Api", "Api link doesn't seem to be valid");
        } else {
            DataDownloadManager.getInstance().downloadEvents(event.getEventId());
        }
        binding.progress.setVisibility(View.VISIBLE);
        Timber.d("Download has started");
    }

    @Subscribe
    public void handleResponseEvent(RetrofitResponseEvent responseEvent) {
        Integer statusCode = responseEvent.getStatusCode();
        if (statusCode.equals(404)) {
            showErrorDialog("HTTP Error", statusCode + "Api Not Found");
        }
    }

    @Subscribe
    public void handleJsonEvent(final JsonReadEvent jsonReadEvent) {
        final String name = jsonReadEvent.getName();
        final String json = jsonReadEvent.getJson();
        CommonTaskLoop.getInstance().post(new Runnable() {
            @Override
            public void run() {
                final Gson gson = new Gson();
                switch (name) {
                    case ConstantStrings.EVENT:
                        CommonTaskLoop.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                Event event = gson.fromJson(json, Event.class);
                                DbSingleton.getInstance().insertQuery(event.generateSql());
                                OpenEventApp.postEventOnUIThread(new EventDownloadEvent(true));
                            }
                        });
                        break;
                    case ConstantStrings.TRACKS:
                        CommonTaskLoop.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                Type listType = new TypeToken<List<Track>>() {
                                }.getType();
                                List<Track> tracks = gson.fromJson(json, listType);
                                ArrayList<String> queries = new ArrayList<String>();
                                for (Track current : tracks) {
                                    queries.add(current.generateSql());
                                }
                                DbSingleton.getInstance().insertQueries(queries);
                                EventBus.getDefault().post(new TracksDownloadEvent(true));
                            }
                        });
                        break;
                    case ConstantStrings.SESSIONS: {
                        Type listType = new TypeToken<List<Session>>() {
                        }.getType();
                        List<Session> sessions = gson.fromJson(json, listType);
                        ArrayList<String> queries = new ArrayList<>();
                        for (Session current : sessions) {
                            current.setStartDate(current.getStartTime().split("T")[0]);
                            queries.add(current.generateSql());
                        }
                        DbSingleton.getInstance().insertQueries(queries);
                        OpenEventApp.postEventOnUIThread(new SessionDownloadEvent(true));
                        break;
                    }
                    case ConstantStrings.SPEAKERS: {
                        Type listType = new TypeToken<List<Speaker>>() {
                        }.getType();
                        List<Speaker> speakers = gson.fromJson(json, listType);

                        ArrayList<String> queries = new ArrayList<String>();
                        for (Speaker current : speakers) {
                            for (int i = 0; i < current.getSession().size(); i++) {
                                SessionSpeakersMapping sessionSpeakersMapping = new SessionSpeakersMapping(current.getSession().get(i).getId(), current.getId());
                                String query_ss = sessionSpeakersMapping.generateSql();
                                queries.add(query_ss);
                            }

                            queries.add(current.generateSql());
                        }
                        DbSingleton.getInstance().insertQueries(queries);
                        OpenEventApp.postEventOnUIThread(new SpeakerDownloadEvent(true));

                        break;
                    }
                    case ConstantStrings.SPONSORS:
                        CommonTaskLoop.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                Type listType = new TypeToken<List<Sponsor>>() {
                                }.getType();
                                List<Sponsor> sponsors = gson.fromJson(json, listType);
                                ArrayList<String> queries = new ArrayList<String>();
                                for (Sponsor current : sponsors) {
                                    queries.add(current.generateSql());
                                }
                                DbSingleton.getInstance().insertQueries(queries);
                                OpenEventApp.postEventOnUIThread(new SponsorDownloadEvent(true));
                            }
                        });
                        break;
                    case ConstantStrings.MICROLOCATIONS:
                        CommonTaskLoop.getInstance().post(new Runnable() {
                            @Override
                            public void run() {

                                Type listType = new TypeToken<List<Microlocation>>() {
                                }.getType();
                                List<Microlocation> microlocations = gson.fromJson(json, listType);
                                ArrayList<String> queries = new ArrayList<String>();
                                for (Microlocation current : microlocations) {
                                    queries.add(current.generateSql());
                                }
                                DbSingleton.getInstance().insertQueries(queries);
                                OpenEventApp.postEventOnUIThread(new MicrolocationDownloadEvent(true));
                            }
                        });
                        break;
                    default:
                        //do nothing
                }
            }
        });

    }

    @Subscribe
    public void errorHandlerEvent(RetrofitError error) {
        String errorType;
        String errorDesc;
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = connMgr.getActiveNetworkInfo();
        if (!(netinfo != null && netinfo.isConnected())) {
            OpenEventApp.postEventOnUIThread(new ShowNetworkDialogEvent());
        } else {
            if (error.getThrowable() instanceof IOException) {
                errorType = "Timeout";
                errorDesc = String.valueOf(error.getThrowable().getCause());
            } else if (error.getThrowable() instanceof IllegalStateException) {
                errorType = "ConversionError";
                errorDesc = String.valueOf(error.getThrowable().getCause());
            } else {
                errorType = "Other Error";
                errorDesc = String.valueOf(error.getThrowable().getLocalizedMessage());
            }
            Timber.tag(errorType).e(errorDesc);
            showErrorDialog(errorType, errorDesc);
        }
    }

    public void downloadFromAssets() {

        if (!sharedPreferences.getBoolean(ConstantStrings.DATABASE_RECORDS_EXIST, false)) {
            //TODO: Add and Take counter value from to config.json
            sharedPreferences.edit().putBoolean(ConstantStrings.DATABASE_RECORDS_EXIST, true).apply();
            counter = 6;
            readJsonAsset(Urls.EVENT);
            readJsonAsset(Urls.TRACKS);
            readJsonAsset(Urls.SPEAKERS);
            readJsonAsset(Urls.SESSIONS);
            readJsonAsset(Urls.SPONSORS);
            readJsonAsset(Urls.MICROLOCATIONS);
        } else {
            binding.progress.setVisibility(View.GONE);
        }
    }

    public void readJsonAsset(final String name) {
        CommonTaskLoop.getInstance().post(new Runnable() {
            String json = null;

            @Override
            public void run() {
                try {
                    InputStream inputStream = getAssets().open(name);
                    int size = inputStream.available();
                    byte[] buffer = new byte[size];
                    inputStream.read(buffer);
                    inputStream.close();
                    json = new String(buffer, "UTF-8");


                } catch (IOException e) {
                    e.printStackTrace();

                }
                EventBus.getDefault().post(new JsonReadEvent(name, json));

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(customTabsServiceConnection);
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }

}