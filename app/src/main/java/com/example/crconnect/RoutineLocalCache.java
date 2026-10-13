package com.example.crconnect;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RoutineLocalCache {
    private static final String PREF_NAME = "bubt_routine_local_cache";
    private static final String KEY_CACHED_ROUTINE = "cached_routine_json";

    public static void saveRoutineLocally(Context context, List<RoutineClassItem> items) {
        if (context == null || items == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = gson.toJson(items);
            prefs.edit().putString(KEY_CACHED_ROUTINE, json).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<RoutineClassItem> loadRoutineLocally(Context context) {
        if (context == null) return new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_CACHED_ROUTINE, null);
            if (json == null || json.isEmpty()) return new ArrayList<>();
            Gson gson = new Gson();
            Type type = new TypeToken<List<RoutineClassItem>>(){}.getType();
            List<RoutineClassItem> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void clearRoutineLocally(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_CACHED_ROUTINE).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
