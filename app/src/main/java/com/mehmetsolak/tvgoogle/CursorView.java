package com.mehmetsolak.tvgoogle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;

public class CursorView extends View {

    private static final float DISPLAY_WIDTH_DP = 28f;

    /** Fraction of the source image where the arrow tip (click point) sits. */
    private static final float HOTSPOT_FRACTION_X = 29f / 286f;
    private static final float HOTSPOT_FRACTION_Y = 15f / 429f;

    private final Bitmap bitmap;
    private final int displayWidthPx;
    private final int displayHeightPx;

    public CursorView(Context context) {
        super(context);
        Bitmap source = BitmapFactory.decodeResource(getResources(), R.drawable.cursor_icon);
        float density = context.getResources().getDisplayMetrics().density;
        displayWidthPx = Math.round(DISPLAY_WIDTH_DP * density);
        displayHeightPx = Math.round(displayWidthPx * (source.getHeight() / (float) source.getWidth()));
        bitmap = Bitmap.createScaledBitmap(source, displayWidthPx, displayHeightPx, true);
    }

    public int getDisplayWidthPx() {
        return displayWidthPx;
    }

    public int getDisplayHeightPx() {
        return displayHeightPx;
    }

    public float getHotspotXPx() {
        return displayWidthPx * HOTSPOT_FRACTION_X;
    }

    public float getHotspotYPx() {
        return displayHeightPx * HOTSPOT_FRACTION_Y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, null);
    }
}
