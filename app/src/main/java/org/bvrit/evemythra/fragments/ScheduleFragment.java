package org.bvrit.evemythra.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.ScheduleViewPagerAdapter;
import org.bvrit.evemythra.databinding.FragmentScheduleBinding;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.utils.Days;

import java.util.List;

/**
 * Created by Manan Wason on 16/06/16.
 */
public class ScheduleFragment extends Fragment {


    FragmentScheduleBinding binding;
    int eventId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_schedule, container, false);
        View view = binding.getRoot();
        setHasOptionsMenu(true);

        eventId = getArguments().getInt("eventId");

        setupViewPager(binding.viewpager);
        binding.tabLayout.setupWithViewPager(binding.viewpager);
        return view;
    }


    private void setupViewPager(ViewPager viewPager) {
        ScheduleViewPagerAdapter adapter = new ScheduleViewPagerAdapter(getChildFragmentManager());
        DbSingleton dbSingleton = DbSingleton.getInstance();

        List<String> event_days = dbSingleton.getDateList();
        int daysofEvent = event_days.size();

        for (int i = 0; i < daysofEvent; i++) {
            adapter.addFragment(new DayScheduleFragment(), Days.values()[i].toString(), i, eventId);
        }
        viewPager.setAdapter(adapter);
    }

}

