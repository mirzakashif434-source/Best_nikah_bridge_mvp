package com.nikahbridge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;

/** Production language preference for the global audience. */
public final class LanguageManager {
    public static final String[] NAMES = {
        "English","اردو","العربية","বাংলা","हिन्दी","Türkçe",
        "Bahasa Indonesia","Melayu","ਪੰਜਾਬੀ","فارسی","Français","Deutsch","Español","Italiano"
    };
    public static final String[] CODES = {
        "en","ur","ar","bn","hi","tr","id","ms","pa","fa","fr","de","es","it"
    };
    private static final String PREFS="best_nikah_bridge_language";
    private static final String KEY="language_code";
    private LanguageManager(){}

    public static String currentCode(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"en");
    }
    public static int currentIndex(Context c){
        String code=currentCode(c);
        for(int i=0;i<CODES.length;i++) if(CODES[i].equals(code)) return i;
        return 0;
    }
    public static void select(Context c,int index){
        if(index<0||index>=CODES.length)return;
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,CODES[index]).apply();
    }
    public static void apply(Activity a){
        String code=currentCode(a);
        Locale locale=new Locale(code);
        Locale.setDefault(locale);
        Configuration config=new Configuration(a.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());
    }
    public static String currentName(Context c){return NAMES[currentIndex(c)];}
}
