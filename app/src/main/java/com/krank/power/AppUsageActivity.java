package com.krank.power;

import android.app.Activity;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

public class AppUsageActivity extends Activity {
    private LinearLayout root, list;
    private TextView status;
    private int accent,fg,muted,card;
    @Override public void onCreate(Bundle b){super.onCreate(b);loadColors();build();loadData();}
    private void loadColors(){
        android.content.SharedPreferences p=getSharedPreferences("prefs",0);
        boolean dark=p.getBoolean("dark",true);accent=p.getInt("accent",0xFFFF5252);
        fg=dark?Color.WHITE:Color.rgb(28,32,40);muted=dark?0xFFB1B9C7:0xFF5F6876;card=dark?0xFF20242E:Color.WHITE;
        getWindow().setStatusBarColor(dark?0xFF12141A:0xFFF5F7FA);getWindow().setNavigationBarColor(dark?0xFF12141A:0xFFF5F7FA);
    }
    private void build(){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,16,16,16);root.setBackgroundColor(getSharedPreferences("prefs",0).getBoolean("dark",true)?0xFF12141A:0xFFF5F7FA);
        setContentView(root);
        TextView title=text("APP POWER & DATA",22,accent);title.setTypeface(null,1);root.addView(title);
        TextView note=text("Last 24 hours • Android limits per-app power reporting.",12,muted);note.setPadding(0,8,0,12);root.addView(note);
        status=text("",13,muted);root.addView(status);
        Button access=new Button(this);access.setText("Grant usage access");access.setAllCaps(false);root.addView(access);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button refresh=new Button(this);refresh.setText("Refresh app statistics");refresh.setAllCaps(false);root.addView(refresh);refresh.setOnClickListener(v->loadData());
    }
    private TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);return t;}
    private void loadData(){
        status.setText("Reading app usage and network statistics…");
        new Thread(()->{
            boolean allowed=AppUsageRepository.hasUsageAccess(this);
            List<AppUsageRepository.AppRow> rows=allowed?AppUsageRepository.load(this):new ArrayList<>();
            runOnUiThread(()->show(rows,allowed));
        }).start();
    }
    private void show(List<AppUsageRepository.AppRow> rows,boolean allowed){
        list.removeAllViews();
        if(!allowed){
            status.setText("Usage access is required for per-app activity and network statistics. Enable 2%Power in Android's Usage access screen, then return and refresh.");
            return;
        }
        status.setText("App activity is a clue, not a direct battery measurement. Android does not provide ordinary apps with reliable per-app battery drain values. Data totals depend on Android and carrier reporting.");
        section("MOST ACTIVE APPS • POWER-USE CLUE");
        List<AppUsageRepository.AppRow> active=AppUsageRepository.byActivity(rows);
        int count=0;
        for(AppUsageRepository.AppRow r:active){if(r.foregroundMs<=0)continue;item(r.name,AppUsageRepository.duration(r.foregroundMs)+" foreground • activity estimate, not measured power");if(++count==8)break;}
        if(count==0)item("No app activity data","Android has not reported app usage for this period.");
        section("MOST DATA USED • WI-FI + MOBILE");
        List<AppUsageRepository.AppRow> data=AppUsageRepository.byData(rows);count=0;
        for(AppUsageRepository.AppRow r:data){if(r.totalBytes()<=0)continue;item(r.name,AppUsageRepository.bytes(r.totalBytes())+" total  •  Wi-Fi "+AppUsageRepository.bytes(r.wifiBytes)+"  •  Mobile "+AppUsageRepository.bytes(r.mobileBytes));if(++count==10)break;}
        if(count==0)item("No per-app data totals available","Check Usage access and try again. Some Android builds restrict network statistics.");
    }
    private void section(String s){TextView t=text(s,13,accent);t.setTypeface(null,1);t.setPadding(2,18,2,8);list.addView(t);}
    private void item(String name,String detail){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(14,12,14,12);box.setBackgroundColor(card);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=8;list.addView(box,p);
        TextView a=text(name,15,fg);a.setTypeface(null,1);box.addView(a);
        TextView b=text(detail,12,muted);b.setPadding(0,4,0,0);box.addView(b);
    }
    @Override protected void onResume(){super.onResume();if(root!=null)loadData();}
}