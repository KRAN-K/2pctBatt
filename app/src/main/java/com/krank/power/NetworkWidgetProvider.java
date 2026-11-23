package com.krank.power;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
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
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateAll(context,manager,ids);
        new Thread(() -> {
            String pub=fetchPublicIp();
            if(pub!=null) context.getSharedPreferences("network",0).edit().putString("public_ip",pub).apply();
            updateAll(context,manager,ids);
        }).start();
    }
    static void updateAll(Context context,AppWidgetManager manager,int[] ids) {
        String local=getLocalIp(context);
        android.content.SharedPreferences p=context.getSharedPreferences("network",0);
        String pub=p.getString("public_ip","Not checked");
        String down=p.getString("download_mbps","—"), up=p.getString("upload_mbps","—");
        for(int id:ids) {
            RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.network_widget);
            v.setTextViewText(R.id.network_local_ip,"Local IP: "+local);
            v.setTextViewText(R.id.network_public_ip,"Public IP: "+pub);
            v.setTextViewText(R.id.network_speed,"Last test  ↓ "+down+" Mbps   ↑ "+up+" Mbps");
            Intent i=new Intent(context,NetworkActivity.class);
            PendingIntent pi=PendingIntent.getActivity(context,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.network_widget_root,pi);
            manager.updateAppWidget(id,v);
        }
    }
    static String getLocalIp(Context context) {
        try {
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n=cm.getActiveNetwork();
            LinkProperties lp=n==null?null:cm.getLinkProperties(n);
            if(lp!=null) for(LinkAddress a:lp.getLinkAddresses()) {
                String ip=a.getAddress().getHostAddress();
                if(ip!=null && ip.indexOf(':')<0 && !ip.startsWith("127.")) return ip;
            }
        } catch(Exception ignored) {}
        return "Unavailable";
    }
    static String fetchPublicIp() {
        HttpURLConnection c=null;
        try {
            c=(HttpURLConnection)new URL("https://api.ipify.org").openConnection();
            c.setConnectTimeout(7000); c.setReadTimeout(7000);
            if(c.getResponseCode()!=200)return null;
            BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));
            String s=r.readLine();r.close();return s;
        } catch(Exception e){return null;} finally{if(c!=null)c.disconnect();}
    }
}