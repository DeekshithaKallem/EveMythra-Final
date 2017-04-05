package org.bvrit.evemythra.api.processor;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.data.Event;
import org.bvrit.evemythra.data.Version;
import org.bvrit.evemythra.dbutils.DataDownloadManager;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.CounterEvent;
import org.bvrit.evemythra.events.RetrofitError;
import org.bvrit.evemythra.events.RetrofitResponseEvent;
import org.bvrit.evemythra.utils.CommonTaskLoop;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * User: MananWason
 * Date: 27-05-2015
 */
public class EventListResponseProcessor implements Callback<Event> {
    private static final String TAG = "EVENT";
    private int counterRequests;

    @Override
    public void onResponse(Call<Event> call, final Response<Event> response) {
        if (response.isSuccessful()) {
            ArrayList<String> queries = new ArrayList<>();
            DbSingleton dbSingleton = DbSingleton.getInstance();
            Event event = response.body();
            String event_query = event.generateSql();
            Version version = response.body().getVersion();
            counterRequests = 0;

            if ((dbSingleton.getVersionIds() == null)) {
                queries.add(version.generateSql());
                queries.add(event_query);
                Timber.d(event_query);
                dbSingleton.insertQueries(queries);
                DataDownloadManager download = DataDownloadManager.getInstance();
                download.downloadSpeakers(response.body().getId());
                download.downloadTracks(response.body().getId());
                download.downloadMicrolocations(response.body().getId());
                download.downloadSession(response.body().getId());
                download.downloadSponsors(response.body().getId());
                counterRequests += 5;

            } else {
                DataDownloadManager download = DataDownloadManager.getInstance();
                if (dbSingleton.getVersionIds().getEventVer() != version.getEventVer()) {
                    dbSingleton.insertQuery(event_query);
                    Timber.d("Downloading EVENT");
                }
                if (dbSingleton.getVersionIds().getSpeakerVer() != version.getSpeakerVer()) {
                    download.downloadSpeakers(response.body().getId());
                    Timber.d("Downloading SPEAKERS");
                    counterRequests++;

                }
                if (dbSingleton.getVersionIds().getSponsorVer() != version.getSponsorVer()) {
                    download.downloadSponsors(response.body().getId());
                    Timber.d("Downloading Sponsor");
                    counterRequests++;

                }
                if (dbSingleton.getVersionIds().getTracksVer() != version.getTracksVer()) {
                    download.downloadTracks(response.body().getId());

                    Timber.d("Downloading TRACKS");
                    counterRequests++;

                }
                if (dbSingleton.getVersionIds().getSessionVer() != version.getSessionVer()) {
                    download.downloadSession(response.body().getId());

                    Timber.d("Downloading SESSIONS");
                    counterRequests++;

                }
                if (dbSingleton.getVersionIds().getMicrolocationsVer() != version.getMicrolocationsVer()) {
                    download.downloadMicrolocations(response.body().getId());
                    Timber.d("Downloading microlocations");
                    counterRequests++;

                }
                if (counterRequests == 0) {
                    Timber.d("Data fresh");
                }
            }
            CounterEvent counterEvent = new CounterEvent(counterRequests);
            OpenEventApp.postEventOnUIThread(counterEvent);

            CommonTaskLoop.getInstance().post(new Runnable() {
                @Override
                public void run() {
                }
            });
        } else {
            OpenEventApp.postEventOnUIThread(new RetrofitResponseEvent(response.code()));
        }

    }

    @Override
    public void onFailure(Call<Event> call, Throwable t) {
        OpenEventApp.postEventOnUIThread(new RetrofitError(t));
    }
}