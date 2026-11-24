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
package com.sevtinge.hyperceiler;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Application extends android.app.Application implements XposedServiceHelper.OnServiceListener {

    private static final String TAG = "HyperCeilerApp";

    @Override
    protected void attachBaseContext(Context base) {
        PrefsUtils.mSharedPreferences = PrefsUtils.getSharedPrefs(base);
        PrefsUtils.initAppRemoteSync(PrefsUtils.mSharedPreferences);
        XposedServiceHelper.registerListener(this);
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onServiceBind(@NonNull XposedService service) {
        AppsTool.isModuleActive = true;
        try {
            AppsTool.XposedVersion = service.getApiVersion();
        } catch (Throwable t) {
            Log.w(TAG, "Failed to get Xposed API version", t);
        }
        DialogHelper.dismissXposedActivateDialogIfShowing();
        try {
            PrefsUtils.setRemotePrefs(service.getRemotePreferences(PrefsUtils.REMOTE_PREFS_GROUP));
        } catch (Throwable t) {
            Log.e(TAG, "Failed to get remote preferences", t);
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AppsTool.isModuleActive = false;
        PrefsUtils.setRemotePrefs(null);
    }
}
