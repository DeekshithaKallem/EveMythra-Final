package org.bvrit.evemythra.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.SpeakersListAdapter;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.data.Speaker;
import org.bvrit.evemythra.databinding.ActivitySessionsDetailBinding;
import org.bvrit.evemythra.databinding.ContentSessionDetailBinding;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.receivers.NotificationAlarmReceiver;
import org.bvrit.evemythra.utils.ConstantStrings;
import org.bvrit.evemythra.utils.ISO8601Date;
import org.bvrit.evemythra.utils.WidgetUpdater;

import java.util.Calendar;
import java.util.List;

import timber.log.Timber;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

/**
 * User: MananWason
 * Date: 08-07-2015
 */
public class SessionDetailActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener{
    private static final String TAG = "Session Detail";

    private SpeakersListAdapter adapter;

    private Session session;

    private String timings;
    private String FRAGMENT_TAG_REST = "fgtr";




    private String trackName, title;

    private Spanned result;

    private boolean isHideToolbarView = false;
    ActivitySessionsDetailBinding binding;
    ContentSessionDetailBinding contentSessionDetailBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_sessions_detail);
        contentSessionDetailBinding = DataBindingUtil.setContentView(this, R.layout.content_session_detail);
     //   sessionsDetailBinding = DataBindingUtil.setContentView(this, R.layout.content_session_detail);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        DbSingleton dbSingleton = DbSingleton.getInstance();
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int id;

        title = getIntent().getStringExtra(ConstantStrings.SESSION);
        trackName = getIntent().getStringExtra(ConstantStrings.TRACK);
        id = getIntent().getIntExtra(ConstantStrings.ID, 0);
        Timber.tag(TAG).d(title);

        binding.toolbarLayout.setTitle(" ");
        binding.appBarSessionDetail.addOnOffsetChangedListener(this);

        final List<Speaker> speakers = dbSingleton.getSpeakersbySessionName(title);
        try {
            session = dbSingleton.getSessionById(id);
            sharedPreferences.edit().putInt(ConstantStrings.SESSION_MAP_ID, id).apply();
        } catch (Exception e) {
            session = dbSingleton.getSessionbySessionname(title);
            sharedPreferences.edit().putInt(ConstantStrings.SESSION_MAP_ID, -1).apply();
        }

        String microlocationName = "Not decided yet";
        if (dbSingleton.getMicrolocationById(session.getMicrolocation().getId()) != null){
            // This function returns id=0 when microlocation is null in session JSON
            microlocationName = dbSingleton.getMicrolocationById(session.getMicrolocation().getId()).getName();
        }
        contentSessionDetailBinding.location.setText(microlocationName);

        binding.titleSession.setText(title);
        if (session.getSubtitle().equals("")) {
            contentSessionDetailBinding.subtitleSession.setVisibility(View.GONE);
        }
        contentSessionDetailBinding.subtitleSession.setText(session.getSubtitle());
        contentSessionDetailBinding.track.setText(trackName);

        updateFloatingIcon(binding.fabSessionBookmark);

        binding.fabSessionBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DbSingleton dbSingleton = DbSingleton.getInstance();
                if (dbSingleton.isBookmarked(session.getId())) {
                    Timber.tag(TAG).d("Bookmark Removed");
                    dbSingleton.deleteBookmarks(session.getId());
                  
                    binding.fabSessionBookmark.setImageResource(R.drawable.ic_bookmark_outline_white_24dp);
                    Snackbar.make(v, R.string.removed_bookmark, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dbSingleton.addBookmarks(session.getId());
                                    binding.fabSessionBookmark.setImageResource(R.drawable.ic_bookmark_white_24dp);
                                    WidgetUpdater.updateWidget(getApplicationContext());
                                }
                            });
                } else {
                    Timber.tag(TAG).d("Bookmarked");
                    dbSingleton.addBookmarks(session.getId());
                    binding.fabSessionBookmark.setImageResource(R.drawable.ic_bookmark_white_24dp);
                    createNotification();
                    Toast.makeText(SessionDetailActivity.this, R.string.added_bookmark, Toast.LENGTH_SHORT).show();
                }
                WidgetUpdater.updateWidget(getApplicationContext());
            }
        });

        String date = ISO8601Date.getTimeZoneDateString(
                ISO8601Date.getDateObject(session.getStartTime())).split(",")[0] + ","
                + ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getStartTime())).split(",")[1];
        String startTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getStartTime())).split(",")[2] + ","
                + ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getStartTime())).split(",")[3];
        String endTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getEndTime())).split(",")[2] + ","
                + ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getEndTime())).split(",")[3];

        if (TextUtils.isEmpty(startTime) && TextUtils.isEmpty(endTime)) {
            contentSessionDetailBinding.startTimeSession.setText(R.string.time_not_specified);
            contentSessionDetailBinding.endTimeSession.setVisibility(View.GONE);

        } else {
            contentSessionDetailBinding.startTimeSession.setText(startTime.trim());
            contentSessionDetailBinding.endTimeSession.setText(endTime.trim());
            contentSessionDetailBinding.dateSession.setText(date.trim());
            Timber.d(date+"\n"+endTime+"\n"+startTime);

        }

        contentSessionDetailBinding.tvAbstractText.setMovementMethod(LinkMovementMethod.getInstance());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(session.getDescription(), FROM_HTML_MODE_LEGACY);
            contentSessionDetailBinding.tvAbstractText.setText(Html.fromHtml(session.getSummary(), FROM_HTML_MODE_LEGACY));
        } else {
            result = Html.fromHtml(session.getDescription());
            contentSessionDetailBinding.tvAbstractText.setText(Html.fromHtml(session.getSummary()));

        }
        contentSessionDetailBinding.tvDescription.setText(result);

        adapter = new SpeakersListAdapter(speakers, this);

        contentSessionDetailBinding.listSpeakerss.setLayoutManager(new LinearLayoutManager(this));
        contentSessionDetailBinding.listSpeakerss.setAdapter(adapter);
        contentSessionDetailBinding.listSpeakerss.setItemAnimator(new DefaultItemAnimator());
    }

    private void updateFloatingIcon(FloatingActionButton fabSessionBookmark) {
        DbSingleton dbSingleton = DbSingleton.getInstance();
        if (dbSingleton.isBookmarked(session.getId())) {
            Timber.tag(TAG).d("Bookmarked");
            fabSessionBookmark.setImageResource(R.drawable.ic_bookmark_white_24dp);
        } else {
            Timber.tag(TAG).d("Bookmark Removed");
            fabSessionBookmark.setImageResource(R.drawable.ic_bookmark_outline_white_24dp);
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.fabSessionBookmark.getVisibility() == View.GONE) {
            /** hide fragment again on back pressed and show session views **/
            binding.contentFrameSession.setVisibility(View.GONE);
            binding.fabSessionBookmark.setVisibility(View.VISIBLE);
            if (contentSessionDetailBinding.nestedScrollviewSessionDetail.getVisibility() == View.GONE) {
                contentSessionDetailBinding.nestedScrollviewSessionDetail.setVisibility(View.VISIBLE);
            }
            if (binding.appBarSessionDetail.getVisibility() == View.GONE) {
                binding.appBarSessionDetail.setVisibility(View.VISIBLE);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map:
                /** Hide all the views except the frame layout **/
                contentSessionDetailBinding.nestedScrollviewSessionDetail.setVisibility(View.GONE);
                binding.appBarSessionDetail.setVisibility(View.GONE);
                binding.fabSessionBookmark.setVisibility(View.GONE);

                binding.contentFrameSession.setVisibility(View.VISIBLE);

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.content_frame_session,
                        ((OpenEventApp) getApplication())
                                .getMapModuleFactory()
                                .provideMapModule()
                                .provideMapFragment(), FRAGMENT_TAG_REST).commit();
                return true;

            case R.id.action_share:
                String startTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getStartTime()));
                String endTime = ISO8601Date.getTimeZoneDateString(ISO8601Date.getDateObject(session.getEndTime()));
                StringBuilder shareText = new StringBuilder();
                shareText.append(String.format("Session Track: %s \nTitle: %s \nStart Time: %s \nEnd Time: %s\n",
                        trackName, title, startTime, endTime));
                if (!result.toString().isEmpty()) {
                    shareText.append("\nDescription: ").append(result.toString());
                } else {
                    shareText.append(getString(R.string.descriptionEmpty));
                }
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_links)));
                return true;

            case R.id.action_add_to_calendar:
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra(CalendarContract.Events.TITLE, title);
                intent.putExtra(CalendarContract.Events.DESCRIPTION, session.getDescription());
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ISO8601Date.getDateObject(session.getStartTime()).getTime());
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        ISO8601Date.getDateObject(session.getEndTime()).getTime());
                startActivity(intent);

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_session_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }


    public void createNotification() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ISO8601Date.getTimeZoneDate(ISO8601Date.getDateObject(session.getStartTime())));

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Integer pref_result = Integer.parseInt(sharedPrefs.getString("notification", "10 mins").substring(0, 2).trim());
        if (pref_result.equals(1)) {
            calendar.add(Calendar.HOUR, -1);
        } else if (pref_result.equals(12)) {
            calendar.add(Calendar.HOUR, -12);
        } else {
            calendar.add(Calendar.MINUTE, -10);
        }
        Intent myIntent = new Intent(this, NotificationAlarmReceiver.class);
        myIntent.putExtra(ConstantStrings.SESSION, session.getId());
        myIntent.putExtra(ConstantStrings.SESSION_TIMING, timings);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(verticalOffset) / (float) maxScroll;

        if (percentage == 1f && isHideToolbarView) {
            // Collapsed

            binding.headerTitleSession.setVisibility(View.GONE);
            binding.toolbarLayout.setTitle(title);
            isHideToolbarView = !isHideToolbarView;
        } else if (percentage < 1f && !isHideToolbarView) {
            // Not Collapsed

            binding.toolbarLayout.setTitle(" ");
            binding.titleSession.setMaxLines(2);
            binding.headerTitleSession.setVisibility(View.VISIBLE);
            isHideToolbarView = !isHideToolbarView;
        }
    }
}