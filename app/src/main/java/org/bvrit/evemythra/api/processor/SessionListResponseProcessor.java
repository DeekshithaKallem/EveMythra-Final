package org.bvrit.evemythra.api.processor;

import org.bvrit.evemythra.OpenEventApp;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.data.parsingExtra.Microlocation;
import org.bvrit.evemythra.dbutils.DbContract;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.SessionDownloadEvent;
import org.bvrit.evemythra.utils.CommonTaskLoop;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;


/**
 * User: MananWason
 * Date: 27-05-2015
 */
public class SessionListResponseProcessor implements Callback<List<Session>> {

    @Override
    public void onResponse(Call<List<Session>> call, final Response<List<Session>> response) {
        if (response.isSuccessful()) {
            CommonTaskLoop.getInstance().post(new Runnable() {

                @Override
                public void run() {
                    DbSingleton dbSingleton = DbSingleton.getInstance();
                    ArrayList<String> queries = new ArrayList<String>();
                    for (int i = 0; i < response.body().size(); i++) {
                        Session session = response.body().get(i);
                        if(session.getMicrolocation() == null){
                            session.setMicrolocation(new Microlocation(0,""));
                        }
                        session.setStartDate(session.getStartTime().split("T")[0]);
                        String query = session.generateSql();
                        queries.add(query);
                        Timber.d(query);
                    }

                    dbSingleton.clearTable(DbContract.Sessions.TABLE_NAME);
                    dbSingleton.insertQueries(queries);
                    OpenEventApp.postEventOnUIThread(new SessionDownloadEvent(true));
                }
            });
        } else {
            OpenEventApp.getEventBus().post(new SessionDownloadEvent(false));
        }
    }

    @Override
    public void onFailure(Call<List<Session>> call, Throwable t) {
        OpenEventApp.getEventBus().post(new SessionDownloadEvent(false));
    }
}