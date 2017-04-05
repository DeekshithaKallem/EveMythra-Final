package org.bvrit.evemythra.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;


import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.TracksListAdapter;
import org.bvrit.evemythra.api.APIClient;
import org.bvrit.evemythra.data.Track;
import org.bvrit.evemythra.databinding.ListTracksBinding;
import org.bvrit.evemythra.dbutils.DataDownloadManager;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.RefreshUiEvent;
import org.bvrit.evemythra.events.TracksDownloadEvent;
import org.bvrit.evemythra.utils.NetworkUtils;
import org.bvrit.evemythra.utils.ShowNotificationSnackBar;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * User: MananWason
 * Date: 05-06-2015
 */
public class TracksFragment extends Fragment implements SearchView.OnQueryTextListener {

    final private String SEARCH = "searchText";

    private TracksListAdapter tracksListAdapter;


    private String searchText = "";

    private SearchView searchView;

    private DbSingleton dbSingleton;

    private Snackbar snackbar;
    List<Track> mTracks;

    private Toolbar toolbar;
    private AppBarLayout.LayoutParams layoutParams;
    private int SCROLL_OFF = 0;
    int eventId;

    ListTracksBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        binding = DataBindingUtil.inflate(inflater, R.layout.list_tracks, container, false);
        View view = binding.getRoot();
        Bundle b = getArguments();
        eventId = b.getInt("eventId");

        dbSingleton = DbSingleton.getInstance();
        setVisibility();
        binding.tracksSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });


        //setting the grid layout to cut-off white space in tablet view
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width/200.00);

        binding.listTracks.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        binding.listTracks.setLayoutManager(linearLayoutManager);
        mTracks = new ArrayList<>();
        tracksListAdapter = new TracksListAdapter(getContext(), mTracks);
        binding.listTracks.setAdapter(tracksListAdapter);
        binding.listTracks.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                layoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == linearLayoutManager.getChildCount() - 1) {
                    layoutParams.setScrollFlags(SCROLL_OFF);
                    toolbar.setLayoutParams(layoutParams);
                }
                binding.listTracks.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }
        //scrollup shows actionbar
        binding.listTracks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy < 0){
                    AppBarLayout appBarLayout;
                    appBarLayout = (AppBarLayout) getActivity().findViewById(R.id.appbar);
                    appBarLayout.setExpanded(true);
                }
            }
        });

        getTracks();

        return view;
    }

    public void setVisibility() {
        if (!dbSingleton.getTrackList().isEmpty()) {
            binding.txtNoTracks.setVisibility(View.GONE);
            binding.listTracks.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoTracks.setVisibility(View.VISIBLE);
            binding.listTracks.setVisibility(View.GONE);
        }
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
    public void onDestroyView() {
        super.onDestroyView();
        layoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        toolbar.setLayoutParams(layoutParams);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (isAdded()) {
            if (searchView != null) {
                bundle.putString(SEARCH, searchText);
            }
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_tracks, menu);
        MenuItem item = menu.findItem(R.id.action_search_tracks);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);
        if (searchText != null) {
            searchView.setQuery(searchText, false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (!TextUtils.isEmpty(query)) {
            searchText = query;
            tracksListAdapter.getFilter().filter(searchText);
        } else {
            tracksListAdapter.refresh();
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return false;
    }

    @Subscribe
    public void RefreshData(RefreshUiEvent event) {
        setVisibility();
        if (searchText.length() == 0) {
            tracksListAdapter.refresh();
        }
    }

    @Subscribe
    public void onTrackDownloadDone(TracksDownloadEvent event) {
        if(binding.tracksSwipeRefresh!=null)
            binding.tracksSwipeRefresh.setRefreshing(false);
        if (event.isState()) {
            if (!searchView.getQuery().toString().isEmpty() && !searchView.isIconified()) {
                tracksListAdapter.getFilter().filter(searchView.getQuery());
            } else {
                tracksListAdapter.refresh();
            }
        } else {
            if (getActivity() != null) {
                Snackbar.make(binding.tracksFrame, getActivity().getString(R.string.refresh_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        refresh();
                    }
                }).show();
            }
        }
    }

    private void refresh() {
        if (NetworkUtils.haveNetworkConnection(getActivity())) {
            if (NetworkUtils.isActiveInternetPresent()) {
                //Internet is working
                DataDownloadManager.getInstance().downloadTracks(eventId);
            } else {
                //set is refreshing false as let user to login
                if (binding.tracksSwipeRefresh.isRefreshing()) {
                    binding.tracksSwipeRefresh.setRefreshing(false);
                }
                //Device is connected to WI-FI or Mobile Data but Internet is not working
                ShowNotificationSnackBar showNotificationSnackBar = new ShowNotificationSnackBar(getContext(),getView(),binding.tracksSwipeRefresh) {
                    @Override
                    public void refreshClicked() {
                        refresh();
                    }
                };
                //show snackbar will be useful if user have blocked notification for this app
                snackbar = showNotificationSnackBar.showSnackBar();
                //show notification (Only when connected to WiFi)
                showNotificationSnackBar.buildNotification();
            }
        } else {
            if (snackbar!=null && snackbar.isShown()) {
                snackbar.dismiss();
            }
            OpenEventApp.getEventBus().post(new TracksDownloadEvent(false));
        }
        setVisibility();
    }


    private void getTracks(){
        APIClient APIClient = new APIClient();

        Call<List<Track>> call= APIClient.getOpenEventAPI().getTracks(eventId);
        call.enqueue(new Callback<List<Track>>() {

            @Override
            public void onResponse(Call<List<Track>> call, final Response<List<Track>> response) {
                if (response.isSuccessful()) {
                    mTracks = response.body();
                    tracksListAdapter.notifyDataSetChanged();
                } else {
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
            }
        });
    }
}
