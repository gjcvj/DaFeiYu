package com.coopanion.pet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity —— 唯一入口页。
 * 职责（刻意极简）：
 *   1. 引导用户去系统设置开启「悬浮窗权限」；
 *   2. 让用户填入 DeepSeek API Key（存入 Prefs，供网页聊天使用）；
 *   3. 启动 / 停止悬浮窗服务。
 */
public class MainActivity extends AppCompatActivity {

    private EditText etApiKey;
    private TextView tvOverlayStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiKey = findViewById(R.id.etApiKey);
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        Button btnOverlay = findViewById(R.id.btnOverlay);
        Button btnSaveKey = findViewById(R.id.btnSaveKey);
        Button btnStartPet = findViewById(R.id.btnStartPet);
        Button btnStopPet = findViewById(R.id.btnStopPet);

        // 回显已保存的 Key（只有自己可见，输入框为密码样式）
        etApiKey.setText(Prefs.of(this).getApiKey());

        btnOverlay.setOnClickListener(v -> openOverlaySettings());
        btnSaveKey.setOnClickListener(v -> {
            Prefs.of(this).setApiKey(etApiKey.getText().toString());
            Toast.makeText(this, R.string.saved_ok, Toast.LENGTH_SHORT).show();
        });
        btnStartPet.setOnClickListener(v -> startPet());
        btnStopPet.setOnClickListener(v -> {
            stopService(new Intent(this, FloatPetService.class));
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshOverlayStatus();
    }

    /** 打开系统「显示在其他应用上层」设置页 */
    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    /** 刷新悬浮窗权限状态文案 */
    private void refreshOverlayStatus() {
        boolean ok = Settings.canDrawOverlays(this);
        tvOverlayStatus.setText(ok ? R.string.overlay_ok : R.string.main_welcome);
    }

    private void startPet() {
        // 先保存一次输入框里的 Key，避免用户忘了点保存
        Prefs.of(this).setApiKey(etApiKey.getText().toString());
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show();
            openOverlaySettings();
            return;
        }
        Intent svc = new Intent(this, FloatPetService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        Toast.makeText(this, R.string.toast_started, Toast.LENGTH_SHORT).show();
    }
}