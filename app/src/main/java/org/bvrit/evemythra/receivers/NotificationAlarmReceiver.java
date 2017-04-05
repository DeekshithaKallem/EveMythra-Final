package org.bvrit.evemythra.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.bvrit.evemythra.services.BookmarkAlarmService;
import org.bvrit.evemythra.utils.ConstantStrings;

/**
 * Created by Manan Wason on 21/08/15.
 */
public class NotificationAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int sessionId = intent.getIntExtra(ConstantStrings.SESSION, 0);
        String sessionTimings = intent.getStringExtra(ConstantStrings.SESSION_TIMING);

        Intent service1 = new Intent(context, BookmarkAlarmService.class);
        service1.putExtra(ConstantStrings.SESSION, sessionId);
        service1.putExtra(ConstantStrings.SESSION_TIMING, sessionTimings);
        context.startService(service1);

    }
}