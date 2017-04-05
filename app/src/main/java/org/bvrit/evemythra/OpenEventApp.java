package org.bvrit.evemythra;

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;

import org.bvrit.evemythra.BuildConfig;
import org.bvrit.evemythra.api.Urls;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.events.ConnectionCheckEvent;
import org.bvrit.evemythra.events.ShowNetworkDialogEvent;
import org.bvrit.evemythra.modules.MapModuleFactory;
import org.bvrit.evemythra.receivers.NetworkConnectivityChangeReceiver;
import org.bvrit.evemythra.utils.ConstantStrings;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import timber.log.Timber;

/**
 * User: MananWason
 * Date: 02-06-2015
 */
public class OpenEventApp extends Application {

    public static final String API_LINK = "Api_Link";
    public static final String EMAIL = "Email";
    public static final String APP_NAME = "App_Name";
    public static String sDefSystemLanguage;
    static Handler handler;
    private static EventBus eventBus;
    private static Context context;
    private MapModuleFactory mapModuleFactory;
    SharedPreferences sharedPreferences;

    public static EventBus getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus();
        }
        return eventBus;
    }

    public static void postEventOnUIThread(final Object event) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                getEventBus().post(event);
            }
        });
    }

    public static Context getAppContext() {
        return OpenEventApp.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(getApplicationContext());
        handler = new Handler(Looper.getMainLooper());
        OpenEventApp.context = getApplicationContext();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getAppContext());
        sDefSystemLanguage = Locale.getDefault().getDisplayLanguage();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
        DbSingleton.init(this);
        mapModuleFactory = new MapModuleFactory();
        registerReceiver(new NetworkConnectivityChangeReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        getEventBus().register(this);

        String json = null;
        try {
            InputStream inputStream = getAssets().open("config.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            String email = jsonObject.has(EMAIL) ? jsonObject.getString(EMAIL) : "";
            String app_name = jsonObject.has(APP_NAME) ? jsonObject.getString(APP_NAME) : "";
            String api_link = jsonObject.has(API_LINK) ? jsonObject.getString(API_LINK) : "";

            Urls.setBaseUrl(api_link);

            sharedPreferences.edit().putString(ConstantStrings.EMAIL, email).apply();
            sharedPreferences.edit().putString(ConstantStrings.APP_NAME, app_name).apply();
            sharedPreferences.edit().putString(ConstantStrings.BASE_API_URL, api_link).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @Subscribe
    public void onConnectionChangeReact(ConnectionCheckEvent event) {
        if (event.connState()) {
            Timber.d("[NetNotif] %s", "Connected to Internet");
        } else {
            Timber.d("[NetNotif] %s", "Not connected to Internet");
            postEventOnUIThread(new ShowNetworkDialogEvent());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        sDefSystemLanguage = newConfig.locale.getDisplayLanguage();
    }


    public MapModuleFactory getMapModuleFactory() {
        return mapModuleFactory;
    }

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }

            FakeCrashLibrary.log(priority, tag, message);

            if (t != null) {
                if (priority == Log.ERROR) {
                    FakeCrashLibrary.logError(t);
                } else if (priority == Log.WARN) {
                    FakeCrashLibrary.logWarning(t);
                }
            }
        }
    }
}
