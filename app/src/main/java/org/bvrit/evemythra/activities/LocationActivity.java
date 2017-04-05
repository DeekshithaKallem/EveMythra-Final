package org.bvrit.evemythra.activities;

import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;

import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.SessionsListAdapter;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.databinding.ActivityLocationsBinding;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.utils.ConstantStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * User: MananWason
 * Date: 8/18/2015
 */
public class LocationActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    final private String SEARCH = "searchText";

    private SessionsListAdapter sessionsListAdapter;

    private GridLayoutManager gridLayoutManager;

    private List<Session> mSessions;
    private static final int locationWiseSessionList = 1;


    ActivityLocationsBinding binding;
    private String location;

    private String searchText = "";

    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_locations);
        setSupportActionBar(binding.toolbarLocations);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final DbSingleton dbSingleton = DbSingleton.getInstance();
        location = getIntent().getStringExtra(ConstantStrings.MICROLOCATIONS);
        binding.toolbarLocations.setTitle(location);
        mSessions = dbSingleton.getSessionbyLocationName(location);

        //setting the grid layout to cut-off white space in tablet view
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width/250.00);

        binding.recyclerViewLocations.setHasFixedSize(true);
        gridLayoutManager = new GridLayoutManager(this, spanCount);
        binding.recyclerViewLocations.setLayoutManager(gridLayoutManager);
        sessionsListAdapter = new SessionsListAdapter(this, mSessions,locationWiseSessionList);
        binding.recyclerViewLocations.setAdapter(sessionsListAdapter);
        binding.recyclerViewLocations.scrollToPosition(SessionsListAdapter.listPosition);
        binding.recyclerViewLocations.setItemAnimator(new DefaultItemAnimator());

        if (!mSessions.isEmpty()) {
            binding.txtNoSessions.setVisibility(View.GONE);
            binding.recyclerViewLocations.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoSessions.setVisibility(View.VISIBLE);
            binding.recyclerViewLocations.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (searchView != null) {
            bundle.putString(SEARCH, searchText);
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search_tracks).getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setQuery(searchText, false);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 250.00);
        gridLayoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        DbSingleton dbSingleton = DbSingleton.getInstance();

        mSessions = dbSingleton.getSessionbyLocationName(location);
        final List<Session> filteredModelList = filter(mSessions, query.toLowerCase(Locale.getDefault()));
        Timber.tag("xyz").d(mSessions.size() + " " + filteredModelList.size());

        sessionsListAdapter.animateTo(filteredModelList);
        binding.recyclerViewLocations.scrollToPosition(0);

        searchText = query;
        return false;
    }

    private List<Session> filter(List<Session> sessions, String query) {
        query = query.toLowerCase(Locale.getDefault());

        final List<Session> filteredTracksList = new ArrayList<>();
        for (Session session : sessions) {
            final String text = session.getTitle().toLowerCase(Locale.getDefault());
            if (text.contains(query)) {
                filteredTracksList.add(session);
            }
        }
        return filteredTracksList;
    }
}