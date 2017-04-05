package org.bvrit.evemythra.api.processor;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.data.Track;
import org.bvrit.evemythra.dbutils.DbContract;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.TracksDownloadEvent;
import org.bvrit.evemythra.utils.CommonTaskLoop;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;


/**
 * Created by MananWason on 27-05-2015.
 */
public class TrackListResponseProcessor implements Callback<List<Track>> {
    //private static final String TAG = "TRACKS";

    @Override
    public void onResponse(Call<List<Track>> call, final Response<List<Track>> response) {
        if (response.isSuccessful()) {
            CommonTaskLoop.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> queries = new ArrayList<>();

                    for (Track track : response.body()) {
                        String query = track.generateSql();
                        queries.add(query);
                        Timber.d(query);
                    }

                    DbSingleton dbSingleton = DbSingleton.getInstance();
                    dbSingleton.clearTable(DbContract.Tracks.TABLE_NAME);
                    dbSingleton.insertQueries(queries);

                    OpenEventApp.postEventOnUIThread(new TracksDownloadEvent(true));
                }
            });
        } else {
            OpenEventApp.getEventBus().post(new TracksDownloadEvent(false));
        }
    }

    @Override
    public void onFailure(Call<List<Track>> call, Throwable t) {
        OpenEventApp.getEventBus().post(new TracksDownloadEvent(false));
    }
}