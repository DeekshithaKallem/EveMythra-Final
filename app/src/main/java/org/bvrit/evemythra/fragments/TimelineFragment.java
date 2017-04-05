package org.bvrit.evemythra.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import org.bvrit.evemythra.R;
import org.bvrit.evemythra.activities.MainActivity;
import org.bvrit.evemythra.adapters.GenericRcyclerViewAdapter;
import org.bvrit.evemythra.api.APIClient;
import org.bvrit.evemythra.data.Event;
import org.bvrit.evemythra.databinding.TimelineFragmentBinding;
import org.bvrit.evemythra.utils.RecyclerItemClickListener;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Deekshitha on 09-03-2017.
 */

public class TimelineFragment extends Fragment {

    TimelineFragmentBinding binding;
    GenericRcyclerViewAdapter adapter;
    List<Event> eventList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.timeline_fragment, container, false);
        View v = binding.getRoot();
        setHasOptionsMenu(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), (view, position) -> {
            if (position != -1) {
                int eventId = eventList.get(position).getId();
                Intent i = new Intent(getActivity(), MainActivity.class);
                Bundle b = new Bundle();
                b.putInt("eventId",eventId);
                b.putString("navImageUrl", eventList.get(position).getBackground_image());
                i.putExtras(b);
                startActivity(i);
              /*  Fragment f1 = new TimelineDetailFragment();
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, f1); // f1_container is your FrameLayout container
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.addToBackStack(null);
                ft.commit();*/
            }
        }));
          getTimelineData();
        return v;
    }

    private void getTimelineData() {
        APIClient client = new APIClient();
        Call<List<Event>> call = client.getOpenEventAPI().getEvents();
        call.enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                eventList = response.body();
                adapter = new GenericRcyclerViewAdapter(getActivity(),eventList,0);
                binding.recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable throwable) {

                Toast.makeText(getActivity(), R.string.network_error_alert, Toast.LENGTH_SHORT).show();


            }
        });


    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        menuInflater.inflate(R.menu.timeline_menu, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
         /*   Fragment f1 = new TimelineDetailFragment();
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, f1); // f1_container is your FrameLayout container
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commit();*/
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
