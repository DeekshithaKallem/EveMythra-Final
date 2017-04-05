package org.bvrit.evemythra.api.network;

import org.bvrit.evemythra.data.Event;
import org.bvrit.evemythra.data.Microlocation;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.data.Speaker;
import org.bvrit.evemythra.data.Sponsor;
import org.bvrit.evemythra.data.Track;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * User: mohit
 * Date: 25/5/15
 */
public interface OpenEventAPI {

    String basePath = "/api/v1/events";
    String eventPath = basePath + "/{eventId}/";
    String getSpeakers = eventPath + "speakers";
    String getSponsors = eventPath + "sponsors";
    String getSessions = eventPath + "sessions";
    String getMicrolocations = eventPath + "microlocations";
    String getTracks = eventPath + "tracks";
    String getEvent = eventPath + "event";
    String getEvents = basePath;

    @GET(getSpeakers)
    Call<List<Speaker>> getSpeakers(@Path("eventId") int eventId);

    @GET(getSponsors)
    Call<List<Sponsor>> getSponsors(@Path("eventId") int eventId);

    @GET(getSessions)
    Call<List<Session>> getSessions(@Path("eventId") int eventId,@Query("order_by") String orderBy);

    @GET(getEvent)
    Call<Event> getEvent(@Path("eventId") int eventId);

    @GET(getEvents)
    Call<List<Event>> getEvents();

    @GET(getMicrolocations)
    Call<List<Microlocation>> getMicrolocations(@Path("eventId") int eventId);

    @GET(getTracks)
    Call<List<Track>> getTracks(@Path("eventId") int eventId);

}