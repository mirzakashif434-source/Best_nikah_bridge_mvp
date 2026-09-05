package com.nikahbridge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Production Welcome artwork drawn locally so the screen remains crisp on every device.
 * This is visual-only; real Firebase actions stay in WelcomeActivity.
 */
public class WelcomeArtworkView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int green = Color.rgb(18, 103, 82);
    private final int dark = Color.rgb(30, 58, 48);
    private final int gold = Color.rgb(190, 145, 45);
    private final int cream = Color.rgb(250, 247, 239);

    public WelcomeArtworkView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = Math.round(w * 1.5f);
        setMeasuredDimension(w, h);
    }

    private void fill(Canvas c, int color) { p.setStyle(Paint.Style.FILL); p.setColor(color); }
    private void stroke(Canvas c, int color, float width) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width); p.setColor(color); }
    private void txt(Canvas c, String s, float x, float y, float size, int color, Paint.Align align, boolean bold) {
        p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); p.setTextAlign(align); p.setTypeface(android.graphics.Typeface.create("sans", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        c.drawText(s, x, y, p);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight(), s = w / 1024f;
        c.drawColor(cream);

        // Soft decorative leafy corners.
        fill(c, Color.rgb(225, 237, 224));
        c.drawCircle(30*s, 55*s, 115*s, p); c.drawCircle(98*s, 118*s, 95*s, p);
        c.drawCircle(w-25*s, 48*s, 120*s, p); c.drawCircle(w-100*s, 120*s, 92*s, p);
        fill(c, Color.rgb(196, 220, 198));
        c.drawOval(new RectF(-25*s, 105*s, 95*s, 260*s), p); c.drawOval(new RectF(945*s, 95*s, 1065*s, 250*s), p);
        fill(c, Color.rgb(170, 205, 177));
        c.drawOval(new RectF(65*s, 35*s, 115*s, 145*s), p); c.drawOval(new RectF(905*s, 35*s, 955*s, 145*s), p);

        // Language selector.
        fill(c, Color.WHITE); c.drawRoundRect(new RectF(755*s, 18*s, 980*s, 82*s), 28*s, 28*s, p);
        stroke(c, Color.rgb(215, 215, 205), 2*s); c.drawRoundRect(new RectF(755*s,18*s,980*s,82*s),28*s,28*s,p);
        stroke(c, green, 3*s); c.drawCircle(790*s,50*s,15*s,p); c.drawOval(new RectF(782*s,35*s,798*s,65*s),p); c.drawLine(775*s,50*s,805*s,50*s,p);
        txt(c,"English / اردو",820*s,58*s,22*s,dark,Paint.Align.LEFT,false); txt(c,"⌄",954*s,59*s,26*s,green,Paint.Align.CENTER,true);

        txt(c,"بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",w/2,158*s,34*s,dark,Paint.Align.CENTER,false);
        txt(c,"Welcome",w/2,220*s,58*s,dark,Paint.Align.CENTER,true);

        // Bridge + heart logo.
        stroke(c,gold,7*s); c.drawArc(new RectF(360*s,250*s,664*s,430*s),180,180,false,p);
        stroke(c,green,10*s); c.drawLine(370*s,342*s,654*s,342*s,p); c.drawLine(405*s,342*s,405*s,395*s,p); c.drawLine(619*s,342*s,619*s,395*s,p);
        fill(c,green);
        Path heart=new Path(); heart.moveTo(w/2,302*s); heart.cubicTo(466*s,272*s,425*s,318*s,w/2,362*s); heart.cubicTo(599*s,318*s,558*s,272*s,w/2,302*s); c.drawPath(heart,p);
        txt(c,"BEST NIKAH BRIDGE",w/2,425*s,38*s,green,Paint.Align.CENTER,true);
        txt(c,"A trusted Muslim matrimonial platform",w/2,466*s,23*s,dark,Paint.Align.CENTER,false);
        txt(c,"ایک معتبر مسلم رشتہ پلیٹ فارم",w/2,500*s,27*s,dark,Paint.Align.CENTER,false);

        drawCard(c,70*s,535*s,954*s,735*s,"Meaningful Matches","بامعنی رشتے","♡");
        drawCard(c,70*s,765*s,954*s,965*s,"Verified Profiles","تصدیق شدہ پروفائلز","✓");
        drawCard(c,70*s,995*s,954*s,1195*s,"Private & Safe","محفوظ اور پرائیویٹ","▣");

        // Button areas are intentionally left visually complete for real overlay controls.
        fill(c,green); c.drawRoundRect(new RectF(135*s,1230*s,889*s,1322*s),46*s,46*s,p);
        txt(c,"♡",190*s,1291*s,34*s,Color.WHITE,Paint.Align.CENTER,true);
        txt(c,"GET STARTED",512*s,1285*s,27*s,Color.WHITE,Paint.Align.CENTER,true);
        txt(c,"آغاز کریں",512*s,1310*s,24*s,Color.WHITE,Paint.Align.CENTER,false);
        stroke(c,green,3*s); c.drawRoundRect(new RectF(135*s,1350*s,889*s,1442*s),46*s,46*s,p);
        txt(c,"♙",190*s,1412*s,32*s,green,Paint.Align.CENTER,true);
        txt(c,"SIGN IN",512*s,1404*s,27*s,green,Paint.Align.CENTER,true);
        txt(c,"لاگ اِن کریں",512*s,1430*s,24*s,green,Paint.Align.CENTER,false);

        fill(c,Color.rgb(190,205,190));
        c.drawRect(0,1470*s,w,1536*s,p);
        // Simple mosque silhouettes.
        fill(c,Color.rgb(95,128,111));
        c.drawRect(0,1510*s,w,1536*s,p); c.drawRect(120*s,1475*s,210*s,1536*s,p); c.drawCircle(165*s,1474*s,28*s,p); c.drawRect(455*s,1490*s,570*s,1536*s,p); c.drawCircle(512*s,1488*s,34*s,p); c.drawRect(820*s,1475*s,900*s,1536*s,p); c.drawCircle(860*s,1474*s,28*s,p);
        txt(c,"Halal Nikah • Trust • Family • Privacy",w/2,1500*s,20*s,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawCard(Canvas c,float l,float t,float r,float b,String en,String ur,String icon){
        p.setShadowLayer(10,0,5,0x18000000); fill(c,Color.WHITE); c.drawRoundRect(new RectF(l,t,r,b),28*(getWidth()/1024f),28*(getWidth()/1024f),p); p.clearShadowLayer();
        fill(c,Color.rgb(232,242,234)); c.drawCircle(l+72*(getWidth()/1024f),(t+b)/2,42*(getWidth()/1024f),p);
        txt(c,icon,l+72*(getWidth()/1024f),(t+b)/2+13*(getWidth()/1024f),42*(getWidth()/1024f),green,Paint.Align.CENTER,true);
        txt(c,en,l+140*(getWidth()/1024f),t+82*(getWidth()/1024f),28*(getWidth()/1024f),dark,Paint.Align.LEFT,true);
        txt(c,ur,l+140*(getWidth()/1024f),t+125*(getWidth()/1024f),25*(getWidth()/1024f),green,Paint.Align.LEFT,false);
    }
}
