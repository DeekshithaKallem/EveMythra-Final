package org.bvrit.evemythra.adapters;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import org.bvrit.evemythra.R;
import org.bvrit.evemythra.activities.TrackSessionsActivity;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.data.Track;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.utils.ConstantStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * User: MananWason
 * Date: 07-06-2015
 */
public class TracksListAdapter extends BaseRVAdapter<Track, TracksListAdapter.RecyclerViewHolder> {

    private Context context;
    private ColorGenerator colorGenerator = ColorGenerator.MATERIAL;
    private TextDrawable.IBuilder drawableBuilder = TextDrawable.builder().round();
    @SuppressWarnings("all")
    Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            DbSingleton instance = DbSingleton.getInstance();
            List<Track> trackList = instance.getTrackList();
            final ArrayList<Track> filteredTracksList = new ArrayList<>();
            String query = constraint.toString().toLowerCase(Locale.getDefault());
            for (Track track : trackList) {
                final String text = track.getName().toLowerCase(Locale.getDefault());
                if (text.contains(query)) {
                    filteredTracksList.add(track);
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredTracksList;
            filterResults.count = filteredTracksList.size();
            Timber.d("Filtering done total results %d", filterResults.count);
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            animateTo((List<Track>) results.values);
        }
    };

    public TracksListAdapter(Context context, List<Track> tracks) {
        super(tracks);
        this.context = context;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_track, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        final Track currentTrack = getItem(position);
       /* ((BindingHolder) holder).binding.setVariable(BR.bindingData, mValues.get(position));
        ((BindingHolder) holder).binding.executePendingBindings();
*/
        DbSingleton dbSingleton = DbSingleton.getInstance();
        List<Session> sessions = dbSingleton.getSessionbyTracksname(currentTrack.getName());

        if (sessions.size() != 0) {
            holder.trackTitle.setText(currentTrack.getName());
            holder.trackDescription.setText(currentTrack.getDescription());

            TextDrawable drawable = drawableBuilder.build(String.valueOf(currentTrack.getName().charAt(0)), colorGenerator.getColor(currentTrack.getName()));
            holder.trackImageIcon.setImageDrawable(drawable);
            holder.trackImageIcon.setBackgroundColor(Color.TRANSPARENT);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String trackTitle = currentTrack.getName();
                    Intent intent = new Intent(context, TrackSessionsActivity.class);
                    intent.putExtra(ConstantStrings.TRACK, trackTitle);
                    context.startActivity(intent);
                }
            });
        }
    }

    public void refresh() {
        Timber.d("Refreshing tracks from db");
        clear();
        animateTo(DbSingleton.getInstance().getTrackList());
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    protected class RecyclerViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.imageView)
        ImageView trackImageIcon;

        @BindView(R.id.track_title)
        TextView trackTitle;

        @BindView(R.id.track_description)
        TextView trackDescription;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }
///*
//    private static class BindingHolder extends RecyclerView.ViewHolder {
//        ViewDataBinding binding;
//
//        public BindingHolder(View itemView) {
//            super(itemView);
//            binding = DataBindingUtil.bind(itemView);
//
//        }
//
//    }*/
}
