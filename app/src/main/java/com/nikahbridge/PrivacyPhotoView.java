package com.nikahbridge;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Privacy-safe blurred silhouette. It contains no member image data. */
public final class PrivacyPhotoView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public PrivacyPhotoView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();
        c.drawColor(Color.rgb(236,238,233));
        paint.setColor(Color.rgb(176,188,181));
        paint.setMaskFilter(new BlurMaskFilter(Math.max(12f,w*.045f),BlurMaskFilter.Blur.NORMAL));
        c.drawCircle(w*.5f,h*.34f,Math.min(w,h)*.14f,paint);
        c.drawRoundRect(w*.23f,h*.52f,w*.77f,h*.94f,w*.14f,w*.14f,paint);
        paint.setMaskFilter(null);
        paint.setColor(Color.rgb(18,103,82));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.max(16f,w*.05f));
        c.drawText("Photo private",w*.5f,h*.88f,paint);
    }
}
