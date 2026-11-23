package com.krank.power;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.util.*;

public class AppUsageWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){refresh(c);}
    public static void refresh(Context c){
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        int[] ids=m.getAppWidgetIds(new ComponentName(c,AppUsageWidgetProvider.class));
        if(ids.length==0)return;
        new Thread(()->{
            boolean allowed=AppUsageRepository.hasUsageAccess(c);
            List<AppUsageRepository.AppRow> rows=allowed?AppUsageRepository.byData(AppUsageRepository.load(c)):new ArrayList<>();
            for(int id:ids){
                Bundle options=m.getAppWidgetOptions(id);
                int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,180);
                int height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,100);
                int max=height<90?1:height<140?2:4;
                android.content.SharedPreferences p=c.getSharedPreferences("prefs",0);
                boolean dark=p.getBoolean("dark",true);int accent=p.getInt("accent",0xFFFF5252);
                RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.app_usage_widget);
                v.setInt(R.id.usage_widget_root,"setBackgroundColor",dark?0xFF20242E:0xFFFFFFFF);
                v.setTextColor(R.id.usage_widget_title,accent);v.setTextColor(R.id.usage_widget_rows,dark?0xFFFFFFFF:0xFF20232B);
                StringBuilder out=new StringBuilder();
                if(!allowed)out.append("Tap to grant Usage access");
                else {
                    int n=0;
                    for(AppUsageRepository.AppRow r:rows){if(r.totalBytes()==0)continue;if(n>0)out.append("\n");
                        out.append(r.name).append("  ").append(AppUsageRepository.bytes(r.totalBytes()));
                        if(++n>=max)break;
                    }
                    if(n==0)out.append("No per-app data reported");
                }
                v.setTextViewText(R.id.usage_widget_rows,out.toString());
                v.setTextViewText(R.id.usage_widget_subtitle,allowed?"Top data users • last 24h":"Permission needed");
                Intent i=new Intent(c,AppUsageActivity.class);
                PendingIntent pi=PendingIntent.getActivity(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                v.setOnClickPendingIntent(R.id.usage_widget_root,pi);m.updateAppWidget(id,v);
            }
        }).start();
    }
}