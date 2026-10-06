package com.coopanion.pet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * BootReceiver —— 开机自启。
 * 重启手机后，如果用户已经开启过悬浮窗权限，就自动恢复大肥鱼悬浮窗。
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        // 权限都没开过就不自启（避免悄悄弹窗）
        if (!Settings.canDrawOverlays(context)) {
            Log.i(TAG, "无悬浮窗权限，跳过开机自启");
            return;
        }
        try {
            Intent svc = new Intent(context, FloatPetService.class);
            svc.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startForegroundService(svc);
        } catch (Exception e) {
            Log.w(TAG, "开机自启失败: " + e.getMessage());
        }
    }
}