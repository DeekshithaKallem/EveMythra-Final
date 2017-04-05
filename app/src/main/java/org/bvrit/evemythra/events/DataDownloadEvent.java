package org.bvrit.evemythra.events;

/**
 * User: shivenmian
 * Date: 03/01/16
 */
public class DataDownloadEvent {
    public int getEventId() {
        return eventId;
    }

    int eventId;
    public DataDownloadEvent(int eventId){
        this.eventId = eventId;
    }

}
