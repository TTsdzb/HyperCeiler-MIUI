/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver.PrefToUri;
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PrefsUtils {
    private static final String TAG = "PrefsUtils";
    public static SharedPreferences mSharedPreferences = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();

    public static String mPrefsPathCurrent = null;
    public static String mPrefsFileCurrent = null;
    public static String mPrefsName = "hyperceiler_prefs";
    public static final String REMOTE_PREFS_GROUP = mPrefsName + "_remote";
    public static String mPrefsPath = "/data/user_de/0/" + ProjectApi.mAppModulePkg + "/shared_prefs";
    public static String mPrefsFile = mPrefsPath + "/" + mPrefsName + ".xml";

    private static volatile SharedPreferences mRemotePrefs = null;
    private static volatile SharedPreferences sLocalPrefs = null;
    private static SharedPreferences.OnSharedPreferenceChangeListener sContentNotifyListener = null;

    private static final SharedPreferences EMPTY_PREFS = new SharedPreferences() {
        private final Editor mEmptyEditor = new Editor() {
            @Override
            public Editor putString(String key, @Nullable String value) {
                return this;
            }

            @Override
            public Editor putStringSet(String key, @Nullable Set<String> values) {
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                return this;
            }

            @Override
            public Editor remove(String key) {
                return this;
            }

            @Override
            public Editor clear() {
                return this;
            }

            @Override
            public boolean commit() {
                return false;
            }

            @Override
            public void apply() {
            }
        };

        @Override
        public Map<String, ?> getAll() {
            return Collections.emptyMap();
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            return defValue;
        }

        @Nullable
        @Override
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            return defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            return defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            return defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return defValue;
        }

        @Override
        public boolean contains(String key) {
            return false;
        }

        @Override
        public Editor edit() {
            return mEmptyEditor;
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }
    };

    private static final SharedPreferences.OnSharedPreferenceChangeListener sLocalPrefsListener = (sharedPreferences, key) -> {
        SharedPreferences remote = mRemotePrefs;
        if (remote == null) {
            return;
        }
        if (key == null) {
            syncAllToRemote();
            return;
        }
        try {
            Map<String, ?> all = sharedPreferences.getAll();
            SharedPreferences.Editor editor = remote.edit();
            if (all != null && all.containsKey(key)) {
                putToEditor(editor, key, all.get(key));
            } else {
                editor.remove(key);
            }
            editor.apply();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to sync preference key to remote: " + key, t);
        }
    };

    private static final SharedPreferences.OnSharedPreferenceChangeListener sRemotePrefsListener = (sharedPreferences, key) -> {
        synchronized (PrefsUtils.class) {
            Map<String, ?> all = sharedPreferences.getAll();
            if (key == null) {
                applyHookPrefsMap(all);
                return;
            }
            if (all != null && all.containsKey(key)) {
                mPrefsMap.put(key, normalizePrefValue(all.get(key)));
            } else {
                mPrefsMap.remove(key);
            }
        }
    };

    private static Object normalizePrefValue(Object value) {
        if (value instanceof Set<?> set) {
            Set<String> copy = new LinkedHashSet<>();
            for (Object item : set) {
                if (item != null) {
                    copy.add(String.valueOf(item));
                }
            }
            return copy;
        }
        return value;
    }

    private static void applyHookPrefsMap(Map<String, ?> all) {
        if (all == null) {
            mPrefsMap.clear();
            return;
        }
        mPrefsMap.keySet().retainAll(all.keySet());
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            mPrefsMap.put(entry.getKey(), normalizePrefValue(entry.getValue()));
        }
    }

    private static void putToEditor(SharedPreferences.Editor editor, String key, Object value) {
        if (value == null) {
            editor.remove(key);
        } else if (value instanceof String str) {
            editor.putString(key, str);
        } else if (value instanceof Boolean bool) {
            editor.putBoolean(key, bool);
        } else if (value instanceof Integer num) {
            editor.putInt(key, num);
        } else if (value instanceof Long num) {
            editor.putLong(key, num);
        } else if (value instanceof Float num) {
            editor.putFloat(key, num);
        } else if (value instanceof Set<?> set) {
            Set<String> copy = new HashSet<>();
            for (Object item : set) {
                if (item != null) {
                    copy.add(String.valueOf(item));
                }
            }
            editor.putStringSet(key, copy);
        }
    }

    public static synchronized void initAppRemoteSync(SharedPreferences localPrefs) {
        if (localPrefs == null) {
            return;
        }
        if (sLocalPrefs != null && sLocalPrefs != localPrefs) {
            try {
                sLocalPrefs.unregisterOnSharedPreferenceChangeListener(sLocalPrefsListener);
            } catch (Throwable ignored) {
            }
        }
        sLocalPrefs = localPrefs;
        if (mSharedPreferences == null) {
            mSharedPreferences = localPrefs;
        }
        localPrefs.registerOnSharedPreferenceChangeListener(sLocalPrefsListener);
    }

    public static synchronized void setRemotePrefs(SharedPreferences remote) {
        mRemotePrefs = remote;
        if (remote != null) {
            syncAllToRemote();
        }
    }

    public static synchronized void syncAllToRemote() {
        SharedPreferences local = mSharedPreferences != null ? mSharedPreferences : sLocalPrefs;
        SharedPreferences remote = mRemotePrefs;
        if (local == null || remote == null) {
            return;
        }
        try {
            Map<String, ?> localAll = local.getAll();
            Map<String, ?> remoteAll = remote.getAll();
            SharedPreferences.Editor editor = remote.edit();
            if (remoteAll != null) {
                for (String existingKey : remoteAll.keySet()) {
                    if (localAll == null || !localAll.containsKey(existingKey)) {
                        editor.remove(existingKey);
                    }
                }
            }
            if (localAll != null) {
                for (Map.Entry<String, ?> entry : localAll.entrySet()) {
                    putToEditor(editor, entry.getKey(), entry.getValue());
                }
            }
            editor.commit();
        } catch (Throwable t) {
            Log.e(TAG, "syncAllToRemote failed", t);
        }
    }

    public static synchronized void initForHook(SharedPreferences remote) {
        if (remote == null) {
            return;
        }
        if (mRemotePrefs != null && mRemotePrefs != remote) {
            try {
                mRemotePrefs.unregisterOnSharedPreferenceChangeListener(sRemotePrefsListener);
            } catch (Throwable ignored) {
            }
        }
        mRemotePrefs = remote;
        applyHookPrefsMap(remote.getAll());
        remote.registerOnSharedPreferenceChangeListener(sRemotePrefsListener);
    }

    public static SharedPreferences getHookPrefs() {
        SharedPreferences remote = mRemotePrefs;
        return remote != null ? remote : EMPTY_PREFS;
    }

    public static SharedPreferences getSharedPrefs(Context context, boolean multiProcess) {
        context = AppsTool.getProtectedContext(context);
        return context.getSharedPreferences(mPrefsName, multiProcess ? Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE : Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        return getSharedPrefs(context, false);
    }
    
    public static String getSharedPrefsPath() {
        if (mPrefsPathCurrent == null) try {
            Field mFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            mFile.setAccessible(true);
            mPrefsPathCurrent = ((File) mFile.get(mSharedPreferences)).getParentFile().getAbsolutePath();
            return mPrefsPathCurrent;
        } catch (Throwable t) {
            System.out.print("Test" + t);
            return mPrefsPath;
        }
        else return mPrefsPathCurrent;
    }

    public static String getSharedPrefsFile() {
        if (mPrefsFileCurrent == null) try {
            Field fFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            fFile.setAccessible(true);
            mPrefsFileCurrent = ((File) fFile.get(mSharedPreferences)).getAbsolutePath();
            System.out.println("Test: mPrefsFileCurrent");
            return mPrefsFileCurrent;
        } catch (Throwable t) {
            System.out.println("Test: mPrefsFile" + t);
            return mPrefsFile;
        }
        else
            System.out.println("Test: mPrefsFileCurrent2");
        return mPrefsFileCurrent;
    }


    public static boolean contains(String key) {
        return mSharedPreferences.contains(key);
    }

    public static SharedPreferences.Editor editor() {
        return mSharedPreferences.edit();
    }

    public static void putString(String key, String defValue) {
        mSharedPreferences.edit().putString(key, defValue).apply();
    }

    public static String getSharedStringPrefs(Context context, String name, String defValue) {
        Uri uri = PrefToUri.stringPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String prefValue = cursor.getString(0);
                cursor.close();
                return prefValue;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (mPrefsMap.containsKey(name))
            return (String) mPrefsMap.getObject(name, defValue);
        else return defValue;
    }

    public static Set<String> getSharedStringSetPrefs(Context context, String name) {
        Uri uri = PrefToUri.stringSetPrefToUri(name);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                Set<String> prefValue = new LinkedHashSet<>();
                while (cursor.moveToNext()) {
                    prefValue.add(cursor.getString(0));
                }
                cursor.close();
                return prefValue;
            } else {
                XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: null");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        LinkedHashSet<String> empty = new LinkedHashSet<>();
        if (mPrefsMap.containsKey(name)) {
            return (Set<String>) mPrefsMap.getObject(name, empty);
        } else {
            return empty;
        }
    }


    public static int getSharedIntPrefs(Context context, String name, int defValue) {
        Uri uri = PrefToUri.intPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (mPrefsMap.containsKey(name))
            return (int) mPrefsMap.getObject(name, defValue);
        else return defValue;
    }


    public static boolean getSharedBoolPrefs(Context context, String name, boolean defValue) {
        Uri uri = PrefToUri.boolPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue == 1;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (mPrefsMap.containsKey(name))
            return (boolean) mPrefsMap.getObject(name, false);
        else
            return defValue;
    }


    public static void registerOnSharedPreferenceChangeListener(Context context) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            Log.i("prefs", "Changed: " + key);
            AppsTool.requestBackup(context);
            Object val = sharedPreferences.getAll().get(key);
            String path = "";
            if (val instanceof String)
                path = "string/";
            else if (val instanceof Set<?>)
                path = "stringset/";
            else if (val instanceof Integer)
                path = "integer/";
            else if (val instanceof Boolean)
                path = "boolean/";

            ContentResolver resolver = context.getContentResolver();
            resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/" + path + key), null);
            if (!path.isEmpty()) resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/" + path + key), null);
        });
    }
}
