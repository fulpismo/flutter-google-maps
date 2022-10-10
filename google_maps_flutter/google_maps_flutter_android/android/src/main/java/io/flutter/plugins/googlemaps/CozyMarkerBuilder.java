package io.flutter.plugins.googlemaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

public class CozyMarkerBuilder {
    private final int bubblePointSize;
    private final Bitmap defaultClusterMarker;
    private final Paint clusterTextPaint;
    private final Paint bubbleTextPaint;
    private final Rect clusterRect;

    CozyMarkerBuilder(int size, int bubblePointSize, Context context) {
        this.bubblePointSize = bubblePointSize;
        clusterRect = new Rect();
        defaultClusterMarker = getClusterBitmap(size);
        clusterTextPaint = setTextPaint(size / 3f, context);
        bubbleTextPaint = setTextPaint(size / 4f, context);
    }

    @NonNull
    private static Paint setTextPaint(float size, Context context) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.oatmealpro2_semibold));
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.LEFT);
        return paint;
    }

    @NonNull
    private static Paint getBackgroundColor() {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        return paint;
    }

    @NonNull
    private static Paint getShadowPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(25);
        paint.setStrokeWidth(6);
        paint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
        paint.setAntiAlias(true);
        return paint;
    }

    private static Bitmap getClusterBitmap(int size) {
        Bitmap marker = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(marker);
        int shadowWidth = size + 2;
        canvas.drawCircle(shadowWidth / 2f, (shadowWidth / 2f) + 2.5f, (shadowWidth / 2.5f), getShadowPaint());
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, getBackgroundColor());
        return marker;
    }

    private Path getBubblePoint(Bitmap marker) {
        Path pointer = new Path();
        pointer.setFillType(Path.FillType.EVEN_ODD);
        float width = marker.getWidth();
        float height = marker.getHeight() - bubblePointSize;
        pointer.moveTo(width / 2f - bubblePointSize, height);
        pointer.lineTo(width / 2f + bubblePointSize, height);
        pointer.lineTo(width / 2f, height + bubblePointSize);
        pointer.lineTo(width / 2f - bubblePointSize, height);
        pointer.close();
        return pointer;
    }


    private Bitmap getBubbleBitmap(RectF bubbleRect) {
        float height = bubbleRect.height();
        Bitmap marker = Bitmap.createBitmap((int)bubbleRect.width() + 40, (int)(height + bubblePointSize), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(marker);
        RectF shadow = new RectF(bubbleRect.left + 2.5f, bubbleRect.top + 2.5f, bubbleRect.top + 2.5f, bubbleRect.bottom + 2.5f);
        canvas.drawRoundRect(shadow, 10, 10, getShadowPaint());
        canvas.drawRoundRect(bubbleRect, 10, 10, getBackgroundColor());
        return marker;
    }

    public Bitmap addClusterMarkerText(String text) {
        Bitmap marker = Bitmap.createBitmap(this.defaultClusterMarker);
        Canvas canvas = new Canvas(marker);
        clusterTextPaint.getTextBounds(text, 0, text.length(), clusterRect);
        float width = marker.getWidth();
        float height = marker.getHeight();
        float dx = (width / 2f) - (clusterRect.width() / 2f) - clusterRect.left;
        float dy = (height / 2f) + (clusterRect.height() / 2f) - clusterRect.bottom;
        canvas.drawText(text, dx, dy, clusterTextPaint);
        return marker;
    }

    public Bitmap addBubbleMarkerText(String text) {
        Rect r = new Rect();
        bubbleTextPaint.getTextBounds(text, 0, text.length(), r);
        Bitmap marker = getBubbleBitmap(new RectF(r));
        Canvas canvas = new Canvas(marker);
        float width = marker.getWidth();
        float height = marker.getHeight();
        float dx = (width / 2f) - (r.width() / 2f) - r.left;
        float dy = (height / 2.5f) + (r.height() / 2.5f) - r.bottom;
        canvas.drawText(text, dx, dy, bubbleTextPaint);
        canvas.drawPath(getBubblePoint(marker), getBackgroundColor());
        return marker;
    }


}
