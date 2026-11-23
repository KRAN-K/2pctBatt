package com.krank.power;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Locale;

public class NetworkActivity extends Activity {
    private TextView local,publicIp,speed,status;
    private Button test;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(18,20,26));
        getWindow().setNavigationBarColor(Color.rgb(18,20,26));
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,22,20,20);root.setBackgroundColor(Color.rgb(18,20,26));setContentView(root);
        TextView title=label("NETWORK STATUS",22,Color.rgb(255,138,101));title.setTypeface(null,1);root.addView(title);
        local=label("Local IP: checking…",18,Color.WHITE);root.addView(local);
        publicIp=label("Public IP: checking…",16,Color.LTGRAY);root.addView(publicIp);
        speed=label("Download: — Mbps\nUpload: — Mbps",18,Color.WHITE);root.addView(speed);
        status=label("Speed test uses Cloudflare's public test endpoint. Results are approximate.",13,Color.LTGRAY);root.addView(status);
        Button refresh=new Button(this);refresh.setText("Refresh IP addresses");root.addView(refresh);
        test=new Button(this);test.setText("Run internet speed test");root.addView(test);
        refresh.setOnClickListener(v->refreshIps());test.setOnClickListener(v->runSpeedTest());
        refreshIps();
        String d=getSharedPreferences("network",0).getString("download_mbps","—");
        String u=getSharedPreferences("network",0).getString("upload_mbps","—");
        speed.setText("Download: "+d+" Mbps\nUpload: "+u+" Mbps");
    }
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,16,0,8);return t;}
    private void refreshIps(){
        local.setText("Local IP: "+NetworkWidgetProvider.getLocalIp(this));publicIp.setText("Public IP: checking…");
        new Thread(()->{
            String ip=NetworkWidgetProvider.fetchPublicIp();
            getSharedPreferences("network",0).edit().putString("public_ip",ip==null?"Unavailable":ip).apply();
            runOnUiThread(()->{publicIp.setText("Public IP: "+(ip==null?"Unavailable":ip));updateWidgets();});
        }).start();
    }
    private void runSpeedTest(){
        test.setEnabled(false);status.setText("Testing download and upload. Keep this screen open.");
        new Thread(()->{
            double d=-1,u=-1;String err=null;
            try{d=downloadTest();u=uploadTest();}catch(Exception e){err=e.getMessage();}
            final double fd=d,fu=u;final String fe=err;
            getSharedPreferences("network",0).edit().putString("download_mbps",d<0?"—":String.format(Locale.US,"%.1f",d)).putString("upload_mbps",u<0?"—":String.format(Locale.US,"%.1f",u)).apply();
            runOnUiThread(()->{
                test.setEnabled(true);
                if(fd>=0&&fu>=0){speed.setText(String.format(Locale.US,"Download: %.1f Mbps\nUpload: %.1f Mbps",fd,fu));status.setText("Test complete. VPN, Wi-Fi signal and server load affect results.");}
                else status.setText("Speed test failed: "+(fe==null?"Network unavailable":fe));
                updateWidgets();
            });
        }).start();
    }
    private double downloadTest() throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL("https://speed.cloudflare.com/__down?bytes=5000000").openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(25000);long start=System.nanoTime();int total=0;byte[] b=new byte[16384];
        try(InputStream in=c.getInputStream()){int n;while((n=in.read(b))!=-1)total+=n;}finally{c.disconnect();}
        double sec=(System.nanoTime()-start)/1e9;if(total==0||sec<=0)throw new Exception("No download data received");
        return total*8.0/sec/1000000.0;
    }
    private double uploadTest() throws Exception {
        byte[] data=new byte[1000000];new java.security.SecureRandom().nextBytes(data);
        HttpURLConnection c=(HttpURLConnection)new URL("https://speed.cloudflare.com/__up").openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(25000);c.setRequestMethod("POST");c.setDoOutput(true);c.setFixedLengthStreamingMode(data.length);c.setRequestProperty("Content-Type","application/octet-stream");
        long start=System.nanoTime();try(OutputStream out=c.getOutputStream()){out.write(data);}
        int code=c.getResponseCode();InputStream in=code>=200&&code<400?c.getInputStream():c.getErrorStream();
        if(in!=null){byte[] b=new byte[1024];while(in.read(b)!=-1){}in.close();}c.disconnect();
        double sec=(System.nanoTime()-start)/1e9;if(code<200||code>=400||sec<=0)throw new Exception("Upload endpoint HTTP "+code);
        return data.length*8.0/sec/1000000.0;
    }
    private void updateWidgets(){
        AppWidgetManager m=AppWidgetManager.getInstance(this);
        int[] ids=m.getAppWidgetIds(new ComponentName(this,NetworkWidgetProvider.class));
        NetworkWidgetProvider.updateAll(this,m,ids);
    }
}