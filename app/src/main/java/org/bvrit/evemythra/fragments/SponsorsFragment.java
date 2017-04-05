package org.bvrit.evemythra.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;


import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.SponsorsListAdapter;
import org.bvrit.evemythra.databinding.ListSponsorsBinding;
import org.bvrit.evemythra.dbutils.DataDownloadManager;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.SponsorDownloadEvent;
import org.bvrit.evemythra.utils.NetworkUtils;
import org.bvrit.evemythra.utils.ShowNotificationSnackBar;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import timber.log.Timber;

/**
 * Created by MananWason on 05-06-2015.
 */
public class SponsorsFragment extends Fragment {

    private SponsorsListAdapter sponsorsListAdapter;



    private Snackbar snackbar;

    private LinearLayoutManager linearLayoutManager;
    private Toolbar toolbar;
    private AppBarLayout.LayoutParams layoutParams;
    private int SCROLL_OFF = 0;
    ListSponsorsBinding binding;

    int eventId;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.list_sponsors, container, false);
        View view = binding.getRoot();
        setHasOptionsMenu(true);

        eventId = getArguments().getInt("eventId");

        final DbSingleton dbSingleton = DbSingleton.getInstance();

        binding.sponsorSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        sponsorsListAdapter = new SponsorsListAdapter(getContext(), dbSingleton.getSponsorList(),
                getActivity(), true);
        binding.listSponsors.setAdapter(sponsorsListAdapter);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        binding.listSponsors.setLayoutManager(linearLayoutManager);
        binding.listSponsors.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                layoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == linearLayoutManager.getChildCount() - 1) {
                    layoutParams.setScrollFlags(SCROLL_OFF);
                    toolbar.setLayoutParams(layoutParams);
                }
                binding.listSponsors.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });


        if (sponsorsListAdapter.getItemCount() != 0) {
            binding.txtNoSponsors.setVisibility(View.GONE);
            binding.listSponsors.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoSponsors.setVisibility(View.VISIBLE);
            binding.listSponsors.setVisibility(View.GONE);
        }
        //scrollup shows actionbar
        binding.listSponsors.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

    @Subscribe
    public void sponsorDownloadDone(SponsorDownloadEvent event) {

        binding.sponsorSwipeRefresh.setRefreshing(false);
        if (event.isState()) {
            sponsorsListAdapter.refresh();
            Timber.d("Refresh done");

        } else {
            if (getActivity() != null) {
                Snackbar.make(getView(), getActivity().getString(R.string.refresh_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        refresh();
                    }
                }).show();
            }
            Timber.d("Refresh not done");

        }
    }

    private void refresh() {
        if (NetworkUtils.haveNetworkConnection(getActivity())) {
            if (NetworkUtils.isActiveInternetPresent()) {
                //Internet is working
                DataDownloadManager.getInstance().downloadSponsors(eventId);
            } else {
                //Device is connected to WI-FI or Mobile Data but Internet is not working
                ShowNotificationSnackBar showNotificationSnackBar = new ShowNotificationSnackBar(getContext(),getView(),binding.sponsorSwipeRefresh) {
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
            Snackbar.make(getView(), getActivity().getString(R.string.refresh_failed), Snackbar.LENGTH_LONG).setAction(R.string.retry_download, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    refresh();
                }
            }).show();
            OpenEventApp.getEventBus().post(new SponsorDownloadEvent(true));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
}
