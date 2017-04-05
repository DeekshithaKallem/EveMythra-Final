package org.bvrit.evemythra.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.bvrit.evemythra.widget.BookmarkWidgetProvider;

/**
 * Created by Murad (free4murad) on 2/15/17.
 */

public class WidgetUpdater {
    public  static  void updateWidget(Context context){
        int widgetIds[] = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(new ComponentName(context.getApplicationContext(), BookmarkWidgetProvider.class));
        BookmarkWidgetProvider medicineWidgets = new BookmarkWidgetProvider();
        medicineWidgets.onUpdate(context.getApplicationContext(), AppWidgetManager.getInstance(context.getApplicationContext()),widgetIds);
        context.sendBroadcast(new Intent(BookmarkWidgetProvider.ACTION_UPDATE));
    }
}