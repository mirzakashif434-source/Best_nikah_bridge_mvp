package com.nikahbridge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared premium 2030 visual language. Pure presentation; no backend/data behavior. */
final class Premium2030Ui {
    static final int GREEN = Color.rgb(11,91,70);
    static final int GREEN_DARK = Color.rgb(7,60,49);
    static final int GOLD = Color.rgb(187,145,60);
    static final int GOLD_SOFT = Color.rgb(232,216,177);
    static final int CREAM = Color.rgb(250,247,239);
    static final int SURFACE = Color.WHITE;
    static final int TEXT = Color.rgb(29,46,42);
    static final int MUTED = Color.rgb(103,111,106);

    private Premium2030Ui(){}

    static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}

    static GradientDrawable rounded(Context c,int fill,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c,radius));
        return g;
    }

    static GradientDrawable outlined(Context c,int fill,int stroke,int radius){
        GradientDrawable g=rounded(c,fill,radius);
        g.setStroke(dp(c,1),stroke);
        return g;
    }

    static GradientDrawable premiumGradient(Context c){
        GradientDrawable g=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{GREEN_DARK,GREEN,Color.rgb(20,117,88)}
        );
        g.setCornerRadius(dp(c,24));
        return g;
    }

    static TextView title(Context c,String text){
        TextView t=new TextView(c);
        t.setText(LanguageManager.tr(c,text));t.setTextSize(29);t.setTextColor(TEXT);
        t.setTypeface(Typeface.SERIF,Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,8));
        return t;
    }

    static TextView subtitle(Context c,String text){
        TextView t=new TextView(c);
        t.setText(LanguageManager.tr(c,text));t.setTextSize(14);t.setTextColor(MUTED);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c,12),0,dp(c,12),dp(c,16));
        return t;
    }

    static TextView section(Context c,String text){
        TextView t=new TextView(c);
        t.setText(LanguageManager.tr(c,text));t.setTextSize(18);t.setTextColor(TEXT);
        t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(dp(c,4),dp(c,14),dp(c,4),dp(c,8));
        return t;
    }

    static LinearLayout card(Context c){
        LinearLayout card=new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c,16),dp(c,14),dp(c,16),dp(c,14));
        card.setBackground(outlined(c,SURFACE,GOLD_SOFT,20));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(c,7),0,dp(c,7));
        card.setLayoutParams(lp);
        card.setElevation(dp(c,2));
        return card;
    }

    static Button primary(Context c,String label){
        Button b=new Button(c);
        b.setText(LanguageManager.tr(c,label));b.setAllCaps(false);b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackground(premiumGradient(c));
        b.setMinHeight(dp(c,58));
        return b;
    }

    static Button secondary(Context c,String label){
        Button b=new Button(c);
        b.setText(LanguageManager.tr(c,label));b.setAllCaps(false);b.setTextSize(15);
        b.setTextColor(GREEN);
        b.setBackground(outlined(c,Color.WHITE,GREEN,18));
        b.setMinHeight(dp(c,56));
        return b;
    }

    static TextView chip(Context c,String label){
        TextView t=new TextView(c);
        t.setText(LanguageManager.tr(c,label));t.setTextSize(12);t.setTextColor(GREEN_DARK);
        t.setPadding(dp(c,10),dp(c,5),dp(c,10),dp(c,5));
        t.setBackground(rounded(c,Color.rgb(238,246,242),16));
        return t;
    }

    static TextView heroLine(Context c,String text){
        TextView t=new TextView(c);
        t.setText(LanguageManager.tr(c,text));t.setTextColor(Color.WHITE);t.setTextSize(20);
        t.setTypeface(Typeface.SERIF,Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c,18),dp(c,18),dp(c,18),dp(c,18));
        t.setBackground(premiumGradient(c));
        return t;
    }

    static void addButton(LinearLayout root,Button b){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(root.getContext(),58));
        lp.setMargins(0,dp(root.getContext(),5),0,dp(root.getContext(),5));
        root.addView(b,lp);
    }

    static void elevate(View v){v.setElevation(dp(v.getContext(),2));}
}
