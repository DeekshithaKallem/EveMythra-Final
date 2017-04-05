package org.bvrit.evemythra.dbutils;

import org.bvrit.evemythra.api.APIClient;
import org.bvrit.evemythra.api.processor.EventListResponseProcessor;
import org.bvrit.evemythra.api.processor.MicrolocationListResponseProcessor;
import org.bvrit.evemythra.api.processor.SessionListResponseProcessor;
import org.bvrit.evemythra.api.processor.SpeakerListResponseProcessor;
import org.bvrit.evemythra.api.processor.SponsorListResponseProcessor;
import org.bvrit.evemythra.api.processor.TrackListResponseProcessor;

/**
 * User: MananWason
 * Date: 31-05-2015
 * <p/>
 * A singleton to keep track of download
 */
public final class DataDownloadManager {
    private static DataDownloadManager instance;

    private APIClient client = new APIClient();

    private DataDownloadManager() {
    }

    public static DataDownloadManager getInstance() {
        if (instance == null) {
            instance = new DataDownloadManager();
        }
        return instance;
    }

    public void downloadEvents(int eventId) {
        client.getOpenEventAPI().getEvent(eventId).enqueue(new EventListResponseProcessor());
    }

    public void downloadSpeakers(int eventId) {
        client.getOpenEventAPI().getSpeakers(eventId).enqueue(new SpeakerListResponseProcessor());
    }

    public void downloadSponsors(int eventId) {
        client.getOpenEventAPI().getSponsors(eventId).enqueue(new SponsorListResponseProcessor());
    }

    public void downloadSession(int eventId) {
        client.getOpenEventAPI().getSessions(eventId,"start_time.asc").enqueue(new SessionListResponseProcessor());
    }

    public void downloadTracks(int eventId) {
        client.getOpenEventAPI().getTracks(eventId).enqueue(new TrackListResponseProcessor());
    }

    public void downloadMicrolocations(int eventId) {
        client.getOpenEventAPI().getMicrolocations(eventId).enqueue(new MicrolocationListResponseProcessor());
    }


}
