package org.bvrit.evemythra.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.DayScheduleAdapter;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.databinding.ListScheduleBinding;
import org.bvrit.evemythra.dbutils.DataDownloadManager;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.RefreshUiEvent;
import org.bvrit.evemythra.events.SessionDownloadEvent;
import org.bvrit.evemythra.utils.ConstantStrings;
import org.bvrit.evemythra.utils.NetworkUtils;
import org.bvrit.evemythra.utils.ShowNotificationSnackBar;
import org.bvrit.evemythra.utils.SortOrder;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Created by Manan Wason on 17/06/16.
 */
public class DayScheduleFragment extends Fragment implements SearchView.OnQueryTextListener {

    final private String SEARCH = "searchText";

    private String searchText = "";

    private SearchView searchView;

    private SwipeRefreshLayout swipeRefreshLayout;


    private DayScheduleAdapter dayScheduleAdapter;

    private String date;

    private int sortType;

    private SharedPreferences sharedPreferences;

    private Snackbar snackbar;

    ListScheduleBinding binding;
    int eventId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        date = getArguments().getString(ConstantStrings.EVENT_DAY, "");
        eventId = getArguments().getInt("eventId");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.list_schedule, container, false);
        View view = binding.getRoot();
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sortType = sharedPreferences.getInt(ConstantStrings.PREF_SORT, 0);

        /**
         * Loading data in background to improve performance.
         * */
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Session> sortedSessions = DbSingleton.getInstance().getSessionbyDate(date, SortOrder.sortOrderSchedule(getActivity()));
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (binding.listSchedule != null && binding.txtNoSchedule != null) {
                            if (!sortedSessions.isEmpty()) {
                                binding.txtNoSchedule.setVisibility(View.GONE);
                            } else {
                                binding.txtNoSchedule.setVisibility(View.VISIBLE);
                            }
                            dayScheduleAdapter = new DayScheduleAdapter(sortedSessions, getContext());
                            binding.listSchedule.setAdapter(dayScheduleAdapter);
                            dayScheduleAdapter.setEventDate(date);
                        }
                    }
                });
            }
        }).start();
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.schedule_swipe_refresh);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        binding.listSchedule.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }
        //scrollup shows actionbar
        binding.listSchedule.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
    public void onSaveInstanceState(Bundle bundle) {
        if (isAdded() && searchView != null) {
                bundle.putString(SEARCH, searchText);
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_schedule:
                final AlertDialog.Builder dialogSort = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.dialog_sort_title)
                        .setSingleChoiceItems(R.array.session_sort, sortType, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sortType = which;
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt(ConstantStrings.PREF_SORT, which);
                                editor.apply();
                                dayScheduleAdapter.refresh();
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
        menu.clear();
        searchText = "";
        inflater.inflate(R.menu.menu_schedule, menu);
        MenuItem item = menu.findItem(R.id.action_search_schedule);
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
            dayScheduleAdapter.getFilter().filter(searchText);
        } else {
            if(dayScheduleAdapter!=null)
                dayScheduleAdapter.refresh();
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return false;
    }

    @Subscribe
    public void refreshData(RefreshUiEvent event) {
        /**
         * if adapter has not initialised, no point in refreshing it.
         * */
        if (dayScheduleAdapter != null)
            dayScheduleAdapter.refresh();

    }

    @Subscribe
    public void onScheduleDownloadDone(SessionDownloadEvent event) {

        swipeRefreshLayout.setRefreshing(false);
        if (event.isState()) {
            if (dayScheduleAdapter != null && searchView != null) {
                if (!searchView.getQuery().toString().isEmpty() && !searchView.isIconified()) {
                    dayScheduleAdapter.getFilter().filter(searchView.getQuery());
                } else {
                    dayScheduleAdapter.refresh();
                }
            }
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
        if (NetworkUtils.haveNetworkConnection(getContext())) {
            if (NetworkUtils.isActiveInternetPresent()) {
                //Internet is working
                DataDownloadManager.getInstance().downloadSession(eventId);
            } else {
                //Device is connected to WI-FI or Mobile Data but Internet is not working
                //set is refreshing false as let user to login
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                ShowNotificationSnackBar showNotificationSnackBar = new ShowNotificationSnackBar(getContext(),getView(),swipeRefreshLayout) {
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
            OpenEventApp.getEventBus().post(new SessionDownloadEvent(false));
        }
    }


}