package org.bvrit.evemythra.activities;

import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.SessionsListAdapter;
import org.bvrit.evemythra.databinding.ActivityTracksBinding;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.utils.ConstantStrings;

/**
 * User: MananWason
 * Date: 14-06-2015
 */
public class TrackSessionsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    final private String SEARCH = "org.fossasia.openevent.searchText";

    private SessionsListAdapter sessionsListAdapter;

    private GridLayoutManager gridLayoutManager;

    private String track;

    private String searchText = "";

    private SearchView searchView;

    private static final int trackWiseSessionList = 4;

    ActivityTracksBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_tracks);

        getWindow().setBackgroundDrawable(null);

        setSupportActionBar(binding.toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        DbSingleton dbSingleton = DbSingleton.getInstance();
        track = getIntent().getStringExtra(ConstantStrings.TRACK);
        if (!TextUtils.isEmpty(track))
            binding.toolbar.setTitle(track);

        //setting the grid layout to cut-off white space in tablet view
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width/250.00);

        binding.recyclerView.setHasFixedSize(true);
        gridLayoutManager = new GridLayoutManager(this, spanCount);
        binding.recyclerView.setLayoutManager(gridLayoutManager);
        sessionsListAdapter = new SessionsListAdapter(this, dbSingleton.getSessionbyTracksname(track),trackWiseSessionList);
        binding.recyclerView.setAdapter(sessionsListAdapter);
        binding.recyclerView.scrollToPosition(SessionsListAdapter.listPosition);
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());

        if (sessionsListAdapter.getItemCount() != 0) {
            binding.txtNoSessions.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoSessions.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search_sessions:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search_tracks).getActionView();
        searchView.setOnQueryTextListener(this);
        if (searchText != null) {
            searchView.setQuery(searchText, false);
        }
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
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (!TextUtils.isEmpty(query)) {
            sessionsListAdapter.setTrackName(track);
            sessionsListAdapter.getFilter().filter(query);
        } else {
            sessionsListAdapter.setTrackName(track);
            sessionsListAdapter.refresh();
        }
        searchText = query;
        return true;
    }
}