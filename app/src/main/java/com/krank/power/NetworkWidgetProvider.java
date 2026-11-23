package com.krank.power;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.*;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.widget.RemoteViews;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class NetworkWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){updateAll(c,m,ids);new Thread(()->{String pub=fetchPublicIp();if(pub!=null)c.getSharedPreferences("network",0).edit().putString("public_ip",pub).apply();updateAll(c,m,ids);}).start();}
    public static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,NetworkWidgetProvider.class));updateAll(c,m,ids);}
    static void updateAll(Context context,AppWidgetManager manager,int[] ids) {
        String local=getLocalIp(context);android.content.SharedPreferences p=context.getSharedPreferences("network",0);
        String pub=p.getString("public_ip","Not checked"),down=p.getString("download_mbps","—"),up=p.getString("upload_mbps","—");
        android.content.SharedPreferences prefs=context.getSharedPreferences("prefs",0);boolean dark=prefs.getBoolean("dark",true);int accent=prefs.getInt("accent",0xFFFF5252);
        for(int id:ids) {
            Bundle o=manager.getAppWidgetOptions(id);int w=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,200),h=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,90);
            RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.network_widget);
            v.setInt(R.id.network_widget_root,"setBackgroundColor",dark?0xFF20242E:0xFFFFFFFF);
            v.setTextColor(R.id.network_widget_title,accent);int fg=dark?0xFFFFFFFF:0xFF20232B,muted=dark?0xFFC8CDD8:0xFF606875;
            v.setTextColor(R.id.network_local_ip,fg);v.setTextColor(R.id.network_public_ip,muted);v.setTextColor(R.id.network_speed,muted);v.setTextColor(R.id.network_widget_hint,accent);
            v.setTextViewText(R.id.network_local_ip,"Local IP: "+local);v.setTextViewText(R.id.network_public_ip,"Public IP: "+pub);
            v.setTextViewText(R.id.network_speed,"↓ "+down+" Mbps  ↑ "+up+" Mbps");
            v.setViewVisibility(R.id.network_public_ip,(w>=180&&h>=90)?android.view.View.VISIBLE:android.view.View.GONE);
            v.setViewVisibility(R.id.network_widget_hint,(w>=220&&h>=130)?android.view.View.VISIBLE:android.view.View.GONE);
            Intent i=new Intent(context,NetworkActivity.class);PendingIntent pi=PendingIntent.getActivity(context,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.network_widget_root,pi);manager.updateAppWidget(id,v);
        }
    }
    static String getLocalIp(Context c) {
        try {ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();LinkProperties lp=n==null?null:cm.getLinkProperties(n);
            if(lp!=null)for(LinkAddress a:lp.getLinkAddresses()){String ip=a.getAddress().getHostAddress();if(ip!=null&&ip.indexOf(':')<0&&!ip.startsWith("127."))return ip;}
        }catch(Exception ignored){}return "Unavailable";
    }
    static String fetchPublicIp() {
        HttpURLConnection c=null;try{c=(HttpURLConnection)new URL("https://api.ipify.org").openConnection();c.setConnectTimeout(7000);c.setReadTimeout(7000);
            if(c.getResponseCode()!=200)return null;BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));String s=r.readLine();r.close();return s;
        }catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}
    }
}