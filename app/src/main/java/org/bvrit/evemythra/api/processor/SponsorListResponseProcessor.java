package org.bvrit.evemythra.api.processor;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.data.Sponsor;
import org.bvrit.evemythra.dbutils.DbContract;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.SponsorDownloadEvent;
import org.bvrit.evemythra.utils.CommonTaskLoop;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Created by MananWason on 26-05-2015.
 */
public class SponsorListResponseProcessor implements Callback<List<Sponsor>> {

    @Override
    public void onResponse(Call<List<Sponsor>> call, final Response<List<Sponsor>> response) {
        if (response.isSuccessful()) {
            CommonTaskLoop.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> queries = new ArrayList<>();

                    for (Sponsor sponsor : response.body()) {
                        sponsor.changeSponsorTypeToInt(sponsor.getType());
                        String query = sponsor.generateSql();
                        queries.add(query);
                        Timber.d(query);
                    }


                    DbSingleton dbSingleton = DbSingleton.getInstance();
                    dbSingleton.clearTable(DbContract.Sponsors.TABLE_NAME);
                    dbSingleton.insertQueries(queries);
                    OpenEventApp.postEventOnUIThread(new SponsorDownloadEvent(true));
                }
            });
        } else {
            OpenEventApp.getEventBus().post(new SponsorDownloadEvent(false));
        }

    }

    @Override
    public void onFailure(Call<List<Sponsor>> call, Throwable t) {
        OpenEventApp.getEventBus().post(new SponsorDownloadEvent(false));
    }
}