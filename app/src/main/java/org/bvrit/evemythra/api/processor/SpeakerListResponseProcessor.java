package org.bvrit.evemythra.api.processor;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.data.SessionSpeakersMapping;
import org.bvrit.evemythra.data.Speaker;
import org.bvrit.evemythra.dbutils.DbContract;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.SpeakerDownloadEvent;
import org.bvrit.evemythra.utils.CommonTaskLoop;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;


/**
 * User: mohit
 * Date: 25/5/15
 */
public class SpeakerListResponseProcessor implements Callback<List<Speaker>> {

    @Override
    public void onResponse(Call<List<Speaker>> call, final Response<List<Speaker>> response) {
        if (response.isSuccessful()) {
            CommonTaskLoop.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> queries = new ArrayList<>();

                    for (Speaker speaker : response.body()) {
                        for (int i = 0; i < speaker.getSession().size(); i++) {
                            SessionSpeakersMapping sessionSpeakersMapping = new SessionSpeakersMapping(speaker.getSession().get(i).getId(), speaker.getId());
                            String query_ss = sessionSpeakersMapping.generateSql();
                            queries.add(query_ss);
                        }
                        String query = speaker.generateSql();
                        queries.add(query);
                        Timber.d(query);
                    }

                    DbSingleton dbSingleton = DbSingleton.getInstance();
                    dbSingleton.clearTable(DbContract.Sessionsspeakers.TABLE_NAME);
                    dbSingleton.clearTable(DbContract.Speakers.TABLE_NAME);
                    dbSingleton.insertQueries(queries);

                    OpenEventApp.postEventOnUIThread(new SpeakerDownloadEvent(true));
                }
            });
        } else {
            OpenEventApp.getEventBus().post(new SpeakerDownloadEvent(false));
        }
    }

    @Override
    public void onFailure(Call<List<Speaker>> call, Throwable t) {
        OpenEventApp.getEventBus().post(new SpeakerDownloadEvent(false));
    }
}