package io.flutter.plugins.googlemaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

public class CozyMarkerBuilder {

    private final Bitmap defaultClusterMarker;
    private final Paint textPaint;

    CozyMarkerBuilder(int size) {
        defaultClusterMarker = getClusterBitmap(size);
        textPaint = setTextPaint(size);
    }

    @NonNull
    private static Paint setTextPaint(int size) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(size / 3f);
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
        paint.setStrokeWidth(4);
        paint.setAntiAlias(true);
        return paint;
    }

    private static Bitmap getClusterBitmap(int size) {
        Bitmap marker = Bitmap.createBitmap(size + 24, size + 24, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(marker);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, getBackgroundColor());
        int shadowWidth = size + 2;
        canvas.drawCircle(shadowWidth / 2f, shadowWidth / 2f, (shadowWidth / 2f) - 2f, getShadowPaint());
        return marker;
    }

    public Bitmap addCountMarkerText(String text) {

        // TODO: a price one OR bitmap sending through method channel
        Bitmap marker = Bitmap.createBitmap(this.defaultClusterMarker);
        Canvas canvas = new Canvas(marker);
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        float dx = (marker.getWidth() / 2f) - (bounds.width() / 2f) - bounds.left;
        float dy = (marker.getHeight() / 2f) + (bounds.height() / 2f) - bounds.bottom;
        canvas.drawText(text, dx, dy, textPaint);
        return marker;
    }

}
