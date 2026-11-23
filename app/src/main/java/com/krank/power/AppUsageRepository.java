package com.krank.power;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.app.usage.NetworkStatsManager;
import android.app.usage.NetworkStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Process;
import java.util.*;

public class AppUsageRepository {
    public static class AppRow {
        public String name, packageName;
        public long foregroundMs, wifiBytes, mobileBytes;
        public AppRow(String n,String p){name=n;packageName=p;}
        public long totalBytes(){return wifiBytes+mobileBytes;}
    }
    public static boolean hasUsageAccess(Context c) {
        try {
            UsageStatsManager u=(UsageStatsManager)c.getSystemService(Context.USAGE_STATS_SERVICE);
            long now=System.currentTimeMillis();
            u.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,now-60000,now);
            return android.app.AppOpsManager.MODE_ALLOWED==((android.app.AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE))
                .checkOpNoThrow("android:get_usage_stats",Process.myUid(),c.getPackageName());
        } catch(Exception e){return false;}
    }
    public static List<AppRow> load(Context c) {
        long end=System.currentTimeMillis(), start=end-24L*60*60*1000;
        UsageStatsManager usage=(UsageStatsManager)c.getSystemService(Context.USAGE_STATS_SERVICE);
        NetworkStatsManager net=(NetworkStatsManager)c.getSystemService(Context.NETWORK_STATS_SERVICE);
        PackageManager pm=c.getPackageManager();
        Map<String,AppRow> rows=new HashMap<>();
        try {
            List<UsageStats> stats=usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,start,end);
            if(stats!=null) for(UsageStats s:stats) {
                if(s.getTotalTimeInForeground()<=0)continue;
                try {
                    ApplicationInfo ai=pm.getApplicationInfo(s.getPackageName(),0);
                    if(ai.uid==Process.myUid())continue;
                    String label=pm.getApplicationLabel(ai).toString();
                    AppRow row=rows.get(s.getPackageName());
                    if(row==null){row=new AppRow(label,s.getPackageName());rows.put(s.getPackageName(),row);}
                    row.foregroundMs=s.getTotalTimeInForeground();
                } catch(Exception ignored){}
            }
        } catch(Exception ignored){}
        List<AppRow> result=new ArrayList<>(rows.values());
        for(AppRow row:result) {
            try { row.wifiBytes=queryBytes(net,ConnectivityManager.TYPE_WIFI,null,start,end,pm.getApplicationInfo(row.packageName,0).uid); } catch(Exception ignored){}
            try { row.mobileBytes=queryBytes(net,ConnectivityManager.TYPE_MOBILE,null,start,end,pm.getApplicationInfo(row.packageName,0).uid); } catch(Exception ignored){}
        }
        return result;
    }
    private static long queryBytes(NetworkStatsManager n,int type,String subscriber,long start,long end,int uid) {
        NetworkStats stats=null; long total=0;
        try {
            stats=n.queryDetailsForUid(type,subscriber,start,end,uid);
            NetworkStats.Bucket b=new NetworkStats.Bucket();
            while(stats.hasNextBucket()) {
                if(!stats.getNextBucket(b))break;
                total+=b.getRxBytes()+b.getTxBytes();
            }
        } catch(Exception ignored){} finally {if(stats!=null)stats.close();}
        return total;
    }
    public static String bytes(long n) {
        if(n<1024)return n+" B";
        if(n<1048576)return String.format(Locale.US,"%.1f KB",n/1024.0);
        if(n<1073741824)return String.format(Locale.US,"%.1f MB",n/1048576.0);
        return String.format(Locale.US,"%.2f GB",n/1073741824.0);
    }
    public static String duration(long ms) {
        long mins=ms/60000;
        return mins>=60?(mins/60)+"h "+(mins%60)+"m":mins+"m";
    }
    public static List<AppRow> byData(List<AppRow> in) {
        List<AppRow> out=new ArrayList<>(in);Collections.sort(out,(a,b)->Long.compare(b.totalBytes(),a.totalBytes()));return out;
    }
    public static List<AppRow> byActivity(List<AppRow> in) {
        List<AppRow> out=new ArrayList<>(in);Collections.sort(out,(a,b)->Long.compare(b.foregroundMs,a.foregroundMs));return out;
    }
}