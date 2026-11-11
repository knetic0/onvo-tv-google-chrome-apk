package com.mehmetsolak.tvgoogle;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private static final float BASE_STEP_PX = 18f;
    private static final float MAX_STEP_PX = 90f;
    private static final float ACCEL_PER_REPEAT = 6f;

    private WebView webView;
    private CursorView cursorView;
    private float cursorX;
    private float cursorY;
    private boolean touchDown;
    private long touchDownTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://www.google.com");

        cursorView = new CursorView(this);
        FrameLayout.LayoutParams cursorParams = new FrameLayout.LayoutParams(
                cursorView.getDisplayWidthPx(), cursorView.getDisplayHeightPx());
        cursorParams.gravity = Gravity.TOP | Gravity.START;

        FrameLayout root = new FrameLayout(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(cursorView, cursorParams);
        setContentView(root);

        webView.post(() -> {
            cursorX = webView.getWidth() / 2f;
            cursorY = webView.getHeight() / 2f;
            updateCursorPosition();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        CookieManager.getInstance().flush();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * WebView consumes DPAD keys itself (native spatial navigation) before
     * Activity.onKeyDown ever sees them, so cursor keys must be intercepted
     * here, ahead of the view hierarchy.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean isCursorKey = keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER;

        if (isCursorKey) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                handleCursorKeyDown(keyCode, event);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                handleCenterKeyUp();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void handleCursorKeyDown(int keyCode, KeyEvent event) {
        float step = Math.min(BASE_STEP_PX + event.getRepeatCount() * ACCEL_PER_REPEAT, MAX_STEP_PX);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                moveCursor(-step, 0);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                moveCursor(step, 0);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                moveCursor(0, -step);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                moveCursor(0, step);
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (!touchDown) {
                    touchDownTime = SystemClock.uptimeMillis();
                    dispatchTouch(MotionEvent.ACTION_DOWN, touchDownTime);
                    touchDown = true;
                }
                break;
        }
    }

    private void handleCenterKeyUp() {
        if (touchDown) {
            dispatchTouch(MotionEvent.ACTION_UP, SystemClock.uptimeMillis());
            touchDown = false;
        }
    }

    /**
     * Plain cursor move: scrolls the page itself when the cursor is pinned
     * against an edge. While a touch is held down (see {@link #touchDown}),
     * the drag below drives scrolling instead, so edge-scrolling is skipped.
     */
    private void moveCursor(float dx, float dy) {
        if (touchDown) {
            cursorX = clamp(cursorX + dx, 0, webView.getWidth());
            cursorY = clamp(cursorY + dy, 0, webView.getHeight());
            updateCursorPosition();
            dispatchTouch(MotionEvent.ACTION_MOVE, SystemClock.uptimeMillis());
            return;
        }

        if (dx != 0) {
            cursorX = moveAxis(cursorX, dx, webView.getWidth(), true);
        }
        if (dy != 0) {
            cursorY = moveAxis(cursorY, dy, webView.getHeight(), false);
        }
        updateCursorPosition();
    }

    /**
     * Moves the cursor along one axis. Scrolling only kicks in when the
     * cursor is pushed further into the edge it's already pinned against
     * (e.g. pressing further up while already at the top) — reversing
     * direction always moves the cursor itself, so it can detach from the
     * edge and click whatever is on screen instead of being stuck scrolling.
     * The scroll call itself is still guarded by canScroll so pushing past
     * the real end of the page can't trigger WebView's overscroll stretch.
     */
    private float moveAxis(float position, float delta, float viewportSize, boolean horizontal) {
        boolean pushingIntoEdge = (delta < 0 && position <= 0) || (delta > 0 && position >= viewportSize);
        if (pushingIntoEdge) {
            int scrollDirection = delta > 0 ? 1 : -1;
            boolean canScroll = horizontal
                    ? webView.canScrollHorizontally(scrollDirection)
                    : webView.canScrollVertically(scrollDirection);
            if (canScroll) {
                if (horizontal) {
                    webView.scrollBy((int) delta, 0);
                } else {
                    webView.scrollBy(0, (int) delta);
                }
                return position;
            }
        }
        return clamp(position + delta, 0, viewportSize);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateCursorPosition() {
        cursorView.setX(cursorX - cursorView.getHotspotXPx());
        cursorView.setY(cursorY - cursorView.getHotspotYPx());
    }

    private void dispatchTouch(int action, long eventTime) {
        MotionEvent event = MotionEvent.obtain(touchDownTime, eventTime, action, cursorX, cursorY, 0);
        webView.dispatchTouchEvent(event);
        event.recycle();
    }
}