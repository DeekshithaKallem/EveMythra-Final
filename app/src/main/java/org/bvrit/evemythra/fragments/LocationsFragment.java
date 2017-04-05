package org.bvrit.evemythra.fragments;

import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
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


import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.LocationsListAdapter;
import org.bvrit.evemythra.databinding.ListLocationsBinding;
import org.bvrit.evemythra.dbutils.DataDownloadManager;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.MicrolocationDownloadEvent;
import org.bvrit.evemythra.events.RefreshUiEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * User: MananWason
 * Date: 8/18/2015
 */
public class LocationsFragment extends Fragment implements SearchView.OnQueryTextListener {
    final private String SEARCH = "searchText";



    private LocationsListAdapter locationsListAdapter;

    private GridLayoutManager gridLayoutManager;

    private String searchText = "";

    private SearchView searchView;

    private Toolbar toolbar;
    private AppBarLayout.LayoutParams layoutParams;
    private int SCROLL_OFF = 0;
    ListLocationsBinding binding;
    int eventId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.list_locations, container, false);
        View view = binding.getRoot();
        setHasOptionsMenu(true);
        eventId = getArguments().getInt("eventId");

        final DbSingleton dbSingleton = DbSingleton.getInstance();
        binding.locationsSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });


        //setting the grid layout to cut-off white space in tablet view
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width/200.00);

        binding.listLocations.setHasFixedSize(true);
        gridLayoutManager = new GridLayoutManager(getActivity(), spanCount);
        binding.listLocations.setLayoutManager(gridLayoutManager);
        locationsListAdapter = new LocationsListAdapter(getContext(), dbSingleton.getMicrolocationsList());
        binding.listLocations.setAdapter(locationsListAdapter);
        binding.listLocations.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                layoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                if (gridLayoutManager.findLastCompletelyVisibleItemPosition() == gridLayoutManager.getChildCount() - 1) {
                    layoutParams.setScrollFlags(SCROLL_OFF);
                    toolbar.setLayoutParams(layoutParams);
                }
                binding.listLocations.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });

        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }
        if (locationsListAdapter.getItemCount() != 0) {
            binding.txtNoMicrolocations.setVisibility(View.GONE);
            binding.listLocations.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoMicrolocations.setVisibility(View.VISIBLE);
            binding.listLocations.setVisibility(View.GONE);
        }
        //scrollup shows actionbar
        binding.listLocations.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        return view;
    }


    public void setVisibility(Boolean isDownloadDone) {
        if (isDownloadDone) {
            binding.txtNoMicrolocations.setVisibility(View.GONE);
            binding.listLocations.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoMicrolocations.setVisibility(View.VISIBLE);
            binding.listLocations.setVisibility(View.GONE);
        }
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
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_locations_fragment, menu);
        MenuItem item = menu.findItem(R.id.action_search_locations);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);
        searchView.setQuery(searchText, false);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 200.00);
        gridLayoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (!TextUtils.isEmpty(query)) {
            locationsListAdapter.getFilter().filter(query);
        } else {
            locationsListAdapter.refresh();
        }
        searchText = query;
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return true;
    }

    @Subscribe
    public void onDataRefreshed(RefreshUiEvent event) {
        setVisibility(true);
        if (TextUtils.isEmpty(searchText)) {
            locationsListAdapter.refresh();
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

    @Subscribe
    public void LocationsDownloadDone(MicrolocationDownloadEvent event) {

        binding.locationsSwipeRefresh.setRefreshing(false);
        if (event.isState()) {
            locationsListAdapter.refresh();

        } else {
            if (getActivity() != null) {
                Snackbar.make(getView(), getActivity().getString(R.string.refresh_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        refresh();
                    }
                }).show();
            }
        }
    }

    private void refresh() {
        DataDownloadManager.getInstance().downloadMicrolocations(eventId);
    }
}
