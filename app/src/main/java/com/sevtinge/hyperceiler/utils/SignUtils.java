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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * Verifies the APK signing certificate SHA-256 fingerprint against this repository's release keystore.
 */
public final class SignUtils {

    private static final String EXPECTED_SHA256 =
            "D9:1E:BE:0B:7E:8A:4C:FC:DD:26:84:9E:98:E6:EC:E7:73:24:F8:AA:E4:B5:26:76:27:F0:BB:6D:F6:49:D2:94";

    private SignUtils() {
    }

    /**
     * Returns the primary APK signing certificate SHA-256 digest as colon-separated uppercase hex.
     */
    public static String getSHA256Signature(Context context) {
        if (context == null) return null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNING_CERTIFICATES
            );
            SigningInfo signingInfo = packageInfo != null ? packageInfo.signingInfo : null;
            if (signingInfo == null) return null;

            Signature[] signatures = signingInfo.getApkContentsSigners();
            if ((signatures == null || signatures.length == 0) && !signingInfo.hasMultipleSigners()) {
                signatures = signingInfo.getSigningCertificateHistory();
            }
            if (signatures == null || signatures.length == 0) return null;

            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
            StringBuilder sb = new StringBuilder(digest.length * 3 - 1);
            for (int i = 0; i < digest.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format(Locale.US, "%02X", digest[i] & 0xFF));
            }
            return sb.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Returns true when the installed APK's signing certificate matches {@link #EXPECTED_SHA256}.
     */
    public static boolean isSignCheckPass(Context context) {
        String actual = getSHA256Signature(context);
        return actual != null && EXPECTED_SHA256.equalsIgnoreCase(actual);
    }
}
