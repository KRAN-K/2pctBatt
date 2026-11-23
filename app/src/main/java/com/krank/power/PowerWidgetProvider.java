package com.krank.power;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.*;
import android.os.BatteryManager;
import android.app.ActivityManager;
import android.os.StatFs;
import android.os.Environment;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.app.PendingIntent;

public class PowerWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){update(c,m,ids);}
    public static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,PowerWidgetProvider.class));update(c,m,ids);}
    private static void update(Context context,AppWidgetManager manager,int[] ids) {
        BatteryManager bm=(BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
        int level=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();am.getMemoryInfo(mi);
        long total=mi.totalMem,used=total-mi.availMem;
        StatFs sf=new StatFs(Environment.getDataDirectory().getPath());long st=sf.getTotalBytes(),su=st-sf.getAvailableBytes();
        android.content.SharedPreferences p=context.getSharedPreferences("prefs",0);
        boolean dark=p.getBoolean("dark",true);int accent=p.getInt("accent",0xFFFF5252);
        for(int id:ids){
            Bundle o=manager.getAppWidgetOptions(id);int w=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,160),h=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,70);
            RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.power_widget);
            v.setInt(R.id.widget_root,"setBackgroundColor",dark?0xFF20242E:0xFFFFFFFF);
            v.setTextColor(R.id.widget_title,accent);v.setTextColor(R.id.widget_battery,dark?0xFFFFFFFF:0xFF20232B);v.setTextColor(R.id.widget_details,dark?0xFFC8CDD8:0xFF606875);v.setTextColor(R.id.widget_extra,dark?0xFFC8CDD8:0xFF606875);
            v.setTextViewText(R.id.widget_battery,"Battery "+(level>=0?level+"%":"N/A"));
            v.setTextViewText(R.id.widget_details,"RAM "+fmt(used)+" / "+fmt(total));
            v.setTextViewText(R.id.widget_extra,"Storage "+pct(su,st)+"% used");
            v.setViewVisibility(R.id.widget_extra,(w>=180&&h>=90)?android.view.View.VISIBLE:android.view.View.GONE);
            Intent i=new Intent(context,MainActivity.class);
            PendingIntent pi=PendingIntent.getActivity(context,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.widget_root,pi);manager.updateAppWidget(id,v);
        }
    }
    private static String fmt(long n){return String.format(java.util.Locale.US,"%.1f GB",n/1073741824.0);}
    private static long pct(long a,long b){return b==0?0:a*100/b;}
}