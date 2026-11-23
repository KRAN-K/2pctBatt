package com.krank.power;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.app.ActivityManager;
import android.os.StatFs;
import android.os.Environment;
import android.widget.RemoteViews;
import android.app.PendingIntent;
import java.io.File;

public class PowerWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        BatteryManager bm=(BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
        int level=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
        long total=mi.totalMem, used=total-mi.availMem;
        StatFs sf=new StatFs(Environment.getDataDirectory().getPath());
        long st=sf.getTotalBytes(), su=st-sf.getAvailableBytes();
        for(int id:ids){
            RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.power_widget);
            v.setTextViewText(R.id.widget_battery,"Battery "+(level>=0?level+"%":"N/A"));
            v.setTextViewText(R.id.widget_details,"RAM "+fmt(used)+" / "+fmt(total)+"  •  Storage "+pct(su,st)+"%");
            Intent i=new Intent(context,MainActivity.class);
            PendingIntent pi=PendingIntent.getActivity(context,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.widget_root,pi);
            manager.updateAppWidget(id,v);
        }
    }
    private static String fmt(long n){return String.format(java.util.Locale.US,"%.1f GB",n/1073741824.0);}
    private static long pct(long a,long b){return b==0?0:a*100/b;}
}
