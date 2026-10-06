package com.coopanion.pet;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Prefs —— 本地配置存取。
 * 刻意极简：只保存 DeepSeek API Key（网页聊天发请求时通过 JS 桥获取）。
 */
public class Prefs {

    private static final String FILE = "coopanion_pet_prefs";
    private static final String KEY_API_KEY = "api_key";

    private final SharedPreferences sp;

    private Prefs(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static Prefs of(Context ctx) {
        return new Prefs(ctx);
    }

    public String getApiKey() {
        return sp.getString(KEY_API_KEY, "").trim();
    }

    public void setApiKey(String key) {
        sp.edit().putString(KEY_API_KEY, key == null ? "" : key.trim()).apply();
    }
}