package org.bvrit.evemythra.fragments;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
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


import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.SpeakersListAdapter;
import org.bvrit.evemythra.api.Urls;
import org.bvrit.evemythra.data.Speaker;
import org.bvrit.evemythra.databinding.ListSpeakersBinding;
import org.bvrit.evemythra.dbutils.DataDownloadManager;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.SpeakerDownloadEvent;
import org.bvrit.evemythra.utils.NetworkUtils;
import org.bvrit.evemythra.utils.ShowNotificationSnackBar;
import org.bvrit.evemythra.views.MarginDecoration;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import timber.log.Timber;

import static org.bvrit.evemythra.utils.SortOrder.sortOrderSpeaker;

public class SpeakersListFragment extends Fragment implements SearchView.OnQueryTextListener {

    private static final String PREF_SORT = "sortType";

    final private String SEARCH = "searchText";

    private SharedPreferences prefsSort;


    private SpeakersListAdapter speakersListAdapter;

    private GridLayoutManager gridLayoutManager;

    private String searchText = "";

    private SearchView searchView;

    private int sortType;

    private Snackbar snackbar;
    private Toolbar toolbar;
    private AppBarLayout.LayoutParams layoutParams;
    private int SCROLL_OFF = 0;
    private int spanCount = 2;
    ListSpeakersBinding binding;
    int eventId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.list_speakers, container, false);
        View view = binding.getRoot();
        setHasOptionsMenu(true);

        eventId = getArguments().getInt("eventId");

        final DbSingleton dbSingleton = DbSingleton.getInstance();
        final List<Speaker> mSpeakers = dbSingleton.getSpeakerList(sortOrderSpeaker(getActivity()));
        prefsSort = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sortType = prefsSort.getInt(PREF_SORT, 0);

        //setting the grid layout to cut-off white space in tablet view
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width/150.00);

        binding.rvSpeakers.addItemDecoration(new MarginDecoration(getContext()));
        binding.rvSpeakers.setHasFixedSize(true);
        speakersListAdapter = new SpeakersListAdapter(mSpeakers, getActivity());
        binding.rvSpeakers.setAdapter(speakersListAdapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), spanCount);
        binding.rvSpeakers.setLayoutManager(gridLayoutManager);
        binding.rvSpeakers.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                layoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                if (gridLayoutManager.findLastVisibleItemPosition() == mSpeakers.size()-1) {
                    layoutParams.setScrollFlags(SCROLL_OFF);
                    toolbar.setLayoutParams(layoutParams);
                }
                binding.rvSpeakers.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
        binding.speakerSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }
        if (!mSpeakers.isEmpty()) {
            binding.txtNoSpeakers.setVisibility(View.GONE);
            binding.rvSpeakers.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoSpeakers.setVisibility(View.VISIBLE);
            binding.rvSpeakers.setVisibility(View.GONE);
        }
        //scrollup shows actionbar
        binding.rvSpeakers.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        switch (item.getItemId()) {
            case R.id.share_speakers_url:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, Urls.WEB_APP_URL_BASIC + Urls.SPEAKERS);
                intent.putExtra(Intent.EXTRA_SUBJECT, R.string.share_links);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_links)));
                break;
            case R.id.action_sort:

                final AlertDialog.Builder dialogSort = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.dialog_sort_title)
                        .setSingleChoiceItems(R.array.speaker_sort, sortType, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sortType = which;
                                SharedPreferences.Editor editor = prefsSort.edit();
                                editor.putInt(PREF_SORT, which);
                                editor.apply();
                                speakersListAdapter.refresh();
                                dialog.dismiss();
                            }
                        });

                dialogSort.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_speakers, menu);
        MenuItem item = menu.findItem(R.id.action_search_speakers);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);
        searchView.setQuery(searchText, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 150.00);
        gridLayoutManager.setSpanCount(spanCount);
    }

    @Subscribe
    public void speakerDownloadDone(SpeakerDownloadEvent event) {
        binding.speakerSwipeRefresh.setRefreshing(false);
        if (event.isState()) {
            if (!searchView.getQuery().toString().isEmpty() && !searchView.isIconified()) {
                speakersListAdapter.getFilter().filter(searchView.getQuery());
            } else {
                speakersListAdapter.refresh();
            }
            Timber.i("Speaker download completed");
        } else {
            if (getActivity() != null) {
                Snackbar.make(getView(), getActivity().getString(R.string.refresh_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        refresh();
                    }
                }).show();
            }
            Timber.i("Speaker download failed.");
        }
    }

    private void refresh() {
        if (NetworkUtils.haveNetworkConnection(getActivity())) {
            if (NetworkUtils.isActiveInternetPresent()) {
                //Internet is working
                DataDownloadManager.getInstance().downloadSpeakers(eventId);
            } else {
                //set is refreshing false as let user to login
                if (binding.speakerSwipeRefresh.isRefreshing()) {
                    binding.speakerSwipeRefresh.setRefreshing(false);
                }
                //Device is connected to WI-FI or Mobile Data but Internet is not working
                ShowNotificationSnackBar showNotificationSnackBar = new ShowNotificationSnackBar(getContext(),getView(),binding.speakerSwipeRefresh) {
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
            OpenEventApp.getEventBus().post(new SpeakerDownloadEvent(false));
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        //hide keyboard on search click
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        searchText = query;
        if (!TextUtils.isEmpty(query)) {
            speakersListAdapter.getFilter().filter(query);
        } else {
            speakersListAdapter.refresh();
        }
        return true;
    }

}
