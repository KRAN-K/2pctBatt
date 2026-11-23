package com.krank.power;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.BatteryManager;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, content, nav;
    private Handler handler=new Handler(Looper.getMainLooper());
    private String page="Dashboard";
    private boolean live=false;
    private int accent=Color.rgb(255,82,82);
    private boolean dark=true;
    private final Runnable ticker=new Runnable(){public void run(){if(live && !isFinishing()){render();handler.postDelayed(this,2000);}}};

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(18,20,26));getWindow().setNavigationBarColor(Color.rgb(18,20,26));loadPrefs();buildShell();render();}
    private void loadPrefs(){android.content.SharedPreferences p=getSharedPreferences("prefs",0);dark=p.getBoolean("dark",true);accent=p.getInt("accent",Color.rgb(255,82,82));}
    private int bg(){return dark?Color.rgb(18,20,26):Color.rgb(245,247,250);}
    private int card(){return dark?Color.rgb(32,36,46):Color.WHITE;}
    private int fg(){return dark?Color.WHITE:Color.rgb(28,32,40);}
    private int muted(){return dark?Color.rgb(177,185,199):Color.rgb(95,104,118);}
    private void buildShell(){
        root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg());setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setPadding(20,18,20,12);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("2%POWER",20,accent);brand.setTypeface(null,1);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView liveTag=text("● LIVE",12,muted());header.addView(liveTag);root.addView(header);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);content.setPadding(16,8,16,20);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setPadding(4,8,4,8);nav.setBackgroundColor(card());String[] tabs={"Home","Live","Apps","Diagnostics","Settings"};
        for(String t:tabs){TextView v=text(t,12,muted());v.setGravity(Gravity.CENTER);v.setPadding(4,12,4,12);v.setOnClickListener(w->{if(t.equals("Apps")){startActivity(new Intent(this,AppUsageActivity.class));return;}page=t.equals("Home")?"Dashboard":t;live=page.equals("Live");handler.removeCallbacks(ticker);if(live)handler.post(ticker);render();});nav.addView(v,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);
    }
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private void heading(String s){TextView t=text(s,22,fg());t.setTypeface(null,1);t.setPadding(2,10,2,12);content.addView(t);}
    private void sub(String s){TextView t=text(s,13,muted());t.setPadding(2,0,2,12);content.addView(t);}
    private void card(String title,String value,String detail,int pct){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(16,14,16,14);box.setBackgroundColor(card());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=10;content.addView(box,lp);
        TextView a=text(title.toUpperCase(Locale.US),12,muted());a.setTypeface(null,1);box.addView(a);
        TextView b=text(value,27,fg());b.setTypeface(null,1);b.setPadding(0,5,0,2);box.addView(b);
        TextView c=text(detail,12,muted());box.addView(c);
        if(pct>=0){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(Math.max(0,Math.min(100,pct)));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));bar.setPadding(0,8,0,0);box.addView(bar);}
    }
    private int battery(){BatteryManager b=(BatteryManager)getSystemService(BATTERY_SERVICE);return b.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);}
    private ActivityManager.MemoryInfo memory(){ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(m);return m;}
    private long[] storage(){android.os.StatFs s=new android.os.StatFs(Environment.getDataDirectory().getPath());return new long[]{s.getTotalBytes(),s.getAvailableBytes()};}
    private String gb(long n){return String.format(Locale.US,"%.2f GB",n/1073741824.0);}
    private String batteryState(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return "Status unavailable";int st=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1);String state=st==BatteryManager.BATTERY_STATUS_CHARGING?"Charging":st==BatteryManager.BATTERY_STATUS_FULL?"Full": "On battery";int temp=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);int volt=i.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);return state+" • "+(temp>=0?String.format(Locale.US,"%.1f°C",temp/10f):"Temp N/A")+" • "+(volt>0?volt+" mV":"Voltage N/A");}
    private void render(){
        if(content==null)return;content.removeAllViews();content.setBackgroundColor(bg());
        ActivityManager.MemoryInfo m=memory();long used=m.totalMem-m.availMem;long[] s=storage();int bat=battery();
        if(page.equals("Dashboard")){
            heading("Device overview");sub(Build.MANUFACTURER+" "+Build.MODEL+"  •  Android "+Build.VERSION.RELEASE);
            card("Battery",bat>=0?bat+"%":"Unavailable",batteryState(),bat);
            card("Memory (RAM)",gb(used)+" used",gb(m.availMem)+" available of "+gb(m.totalMem),(int)(used*100/Math.max(1,m.totalMem)));
            card("Internal storage",gb(s[0]-s[1])+" used",gb(s[1])+" available of "+gb(s[0]),(int)((s[0]-s[1])*100/Math.max(1,s[0])));
            card("Power",batteryState(),"Live electrical current: "+currentInfo(),-1);
            card("CPU",cpuInfo(),"Android-reported app process CPU time; system-wide CPU varies by device.",-1);
            button("Open Live View",()->{page="Live";live=true;handler.removeCallbacks(ticker);handler.post(ticker);render();});\n            button("App power & data usage",()->startActivity(new Intent(this,AppUsageActivity.class)));
        } else if(page.equals("Live")){
            heading("Live monitoring");sub("Refreshes every 2 seconds while this screen is open.");
            card("Battery",bat>=0?bat+"%":"Unavailable",batteryState(),bat);
            card("RAM",gb(used)+" used",gb(m.availMem)+" available",(int)(used*100/Math.max(1,m.totalMem)));
            card("Storage",gb(s[0]-s[1])+" used",gb(s[1])+" free",(int)((s[0]-s[1])*100/Math.max(1,s[0])));
            card("Battery current",currentInfo(),"Hardware support varies; negative values generally indicate discharge.",-1);
            card("CPU process",cpuInfo(),"This is app-process CPU time, not a privileged system-wide CPU meter.",-1);
            card("Device",Build.MODEL,"SDK "+Build.VERSION.SDK_INT+" • "+Build.FINGERPRINT,-1);
        } else if(page.equals("Diagnostics")){
            heading("Diagnostics center");sub("Checks are advisory. Android restricts access to some manufacturer battery-health and system power data.");
            int temp=getTemp();int storagePct=(int)((s[0]-s[1])*100/Math.max(1,s[0]));int ramPct=(int)(used*100/Math.max(1,m.totalMem));
            diagnostic("Battery temperature",temp>450?"WARNING":"OK",temp>450?"Battery temperature is above 45°C. Stop heavy use, unplug if safe, move the device out of heat and let it cool.":"No high battery temperature detected in the current reading.");
            diagnostic("Storage space",storagePct>90?"WARNING":"OK",storagePct>90?"Storage is almost full. Remove large downloads, unused apps and offline media; review Settings > Storage.":"Storage usage is within the normal range.");
            diagnostic("RAM pressure",ramPct>90?"WARNING":"OK",ramPct>90?"Memory use is high. Close unusually heavy apps, remove unused apps and reboot if the device remains sluggish.":"No critical RAM pressure reported by Android.");
            diagnostic("Charging",batteryState().startsWith("Charging")?"CHARGING":"INFO", "If charging is slow or intermittent, test a known-good cable and adapter, inspect the port for lint without using metal tools, and check for heat.");
            diagnostic("Performance", "INFO", "If the device is slow, check for updates, review battery/background usage and restart. Avoid 'RAM cleaner' apps.");
            diagnostic("Battery health","LIMITED","Android does not expose a universal battery-health percentage to ordinary apps. Check the manufacturer's diagnostics or service menu.");
            button("Run checks again",()->render());
        } else if(page.equals("Settings")){
            heading("Appearance & settings");sub("Choose the look of your dashboard.");
            button((dark?"✓ ":"")+"Dark theme",()->setTheme(true));
            button((!dark?"✓ ":"")+"Light theme",()->setTheme(false));
            heading("Accent color");
            String[] names={"Red","Blue","Green","Orange","Purple","Cyan"};int[] colors={0xFFFF5252,0xFF448AFF,0xFF4CAF50,0xFFFF9800,0xFFAB47BC,0xFF00BCD4};
            for(int x=0;x<names.length;x++){final int c=colors[x];button(names[x],()->{accent=c;getSharedPreferences("prefs",0).edit().putInt("accent",c).apply();refreshWidgets();buildShell();render();});}
            heading("About");sub("2%Power v1.2 • Device monitor\nBattery, RAM, storage, live metrics and diagnostics.\n\nSome readings depend on hardware and Android version. Widgets refresh periodically under Android's system scheduling.");
        }
    }
    private int getTemp(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return i==null?-1:i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);}
    private String currentInfo(){BatteryManager b=(BatteryManager)getSystemService(BATTERY_SERVICE);long ua=b.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);return ua==Long.MIN_VALUE?"Not reported":String.format(Locale.US,"%.0f mA",ua/1000.0);}
    private String cpuInfo(){return "Process CPU "+(android.os.Process.getElapsedCpuTime()/1000)+" s since process start";}
    private void diagnostic(String title,String status,String advice){LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(14,12,14,12);box.setBackgroundColor(card());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=9;content.addView(box,p);TextView h=text(title+"  •  "+status,15,status.equals("WARNING")?Color.rgb(255,150,50):accent);h.setTypeface(null,1);box.addView(h);TextView d=text(advice,13,fg());d.setPadding(0,6,0,0);box.addView(d);}
    private void button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(fg());b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(card()));b.setAllCaps(false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=7;content.addView(b,p);b.setOnClickListener(v->action.run());}
    private void setTheme(boolean d){dark=d;getSharedPreferences("prefs",0).edit().putBoolean("dark",d).apply();refreshWidgets();buildShell();render();}\n    private void refreshWidgets(){PowerWidgetProvider.refresh(this);NetworkWidgetProvider.refresh(this);AppUsageWidgetProvider.refresh(this);}
    @Override protected void onResume(){super.onResume();if(live){handler.removeCallbacks(ticker);handler.post(ticker);}}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(ticker);}
}
