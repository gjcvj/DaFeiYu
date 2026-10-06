package com.coopanion.pet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

/**
 * FloatPetService —— 极简悬浮窗服务。
 * 职责只有两件事：
 *   1. 用 WindowManager 创建一个「背景透明 + 开启 JS」的 WebView 悬浮窗；
 *   2. 用 WebViewAssetLoader 加载 assets/index.html（安全域名 https://appassets.androidplatform.net/assets/index.html，
 *      与 file:///android_asset/index.html 等价，但能避免 file:// 的跨域限制，网页 JS 可正常请求 DeepSeek API）。
 *   3. 悬浮窗支持手指拖拽移动（拖动时网页 JS 同时会弹出「被提起」台词）。
 * 不建议在本文件里写任何渲染/物理逻辑——形象渲染全部在网页端用原版素材完成。
 */
public class FloatPetService extends Service {

    private static final int NOTIFY_ID = 1;
    private static final String CHANNEL_ID = "pet_float";

    private WindowManager wm;
    private WindowManager.LayoutParams winParams;
    private WebView webView;

    // 拖拽状态
    private float downRawX, downRawY;
    private int downWinX, downWinY;

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForeground();
        // 没有悬浮窗权限就不硬来
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingWebView();
    }

    /** 前台服务：通知渠道 + 常驻通知（Android 8+ 必须） */
    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(getString(R.string.notification_channel_desc));
            nm.createNotificationChannel(ch);
        }
        Intent tap = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = b
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(NOTIFY_ID, notification);
    }

    /** 创建悬浮窗：透明 WebView + WebViewAssetLoader 加载 index.html */
    private void createFloatingWebView() {
        webView = new WebView(getApplicationContext());
        webView.setBackgroundColor(Color.TRANSPARENT);
        // 悬浮窗整体透明 + JS 开启 + localStorage（网页缓存 API Key / 聊天记录等）
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);   // 只允许通过 asset loader 访问 assets

        // JS 桥：网页聊天时从这里读取保存的 DeepSeek API Key（Prefs 单点存储）
        webView.addJavascriptInterface(new JsBridge(), "AndroidBridge");

        // WebViewAssetLoader：把 assets/ 映射到安全域名，网页 JS fetch 不受 file:// 跨域限制
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });

        // 悬浮窗拖拽：onTouch 只负责移动窗口本身，不消费事件（返回 false），
        // 这样网页 JS 仍能收到 mousedown/mousemove/mouseup 去判定单击/拖拽/双击。
        webView.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    downWinX = winParams.x;
                    downWinY = winParams.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (winParams == null) break;
                    int dx = (int) (event.getRawX() - downRawX);
                    int dy = (int) (event.getRawY() - downRawY);
                    winParams.x = downWinX + dx;
                    winParams.y = downWinY + dy;
                    try {
                        wm.updateViewLayout(webView, winParams);
                    } catch (Exception ignore) {
                    }
                    break;
                default:
                    break;
            }
            return false; // 不消费，事件继续给 WebView → 网页 JS
        });

        winParams = new WindowManager.LayoutParams(
                dp(120), dp(200),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // API 26+ 悬浮窗类型
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS  // 可超出屏幕边缘
                        // ALT_FOCUSABLE_IM：窗口平时不抢焦点，但右击双击聊天输入框时软键盘能正常弹出
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);
        winParams.gravity = Gravity.TOP | Gravity.START;
        winParams.x = dp(8);
        winParams.y = dp(140);
        try {
            wm.addView(webView, winParams);
        } catch (Exception e) {
            stopSelf();
            return;
        }
        // 加载 assets/index.html（等价 file:///android_asset/index.html）
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    /** JS 桥：网页 <script> 中通过 window.AndroidBridge.getApiKey() 读取 Key */
    private class JsBridge {
        @JavascriptInterface
        public String getApiKey() {
            return Prefs.of(FloatPetService.this).getApiKey();
        }
    }

    @Override
    public void onDestroy() {
        if (webView != null && wm != null) {
            try {
                wm.removeView(webView);
            } catch (Exception ignore) {
            }
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}