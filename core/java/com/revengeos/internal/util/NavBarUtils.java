/*
 *  Copyright (C) 2019 RevengeOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.revengeos.internal.util;

import static android.os.UserHandle.USER_CURRENT;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.util.Log;

import static com.revengeos.internal.util.hwkeys.DeviceKeysConstants.*;

public class NavBarUtils {

    private static final String TAG = "NavBarUtils";

    static final String SHARED_PREFERENCES_NAME = "system_navigation_settings_preferences";

    static final String PREFS_BACK_SENSITIVITY_KEY = "system_navigation_back_sensitivity";

    public static final String GESTURAL_NAV_OVERLAY_NARROW_BACK
            = "com.android.internal.systemui.gestural_nav.narrow_back";
    public static final String GESTURAL_NAV_OVERLAY_DEFAULT_BACK
            = "com.android.internal.systemui.gestural_nav.default_back";
    public static final String GESTURAL_NAV_OVERLAY_WIDE_BACK
            = "com.android.internal.systemui.gestural_nav.wide_back";
    public static final String GESTURAL_NAV_OVERLAY_EXTRA_WIDE_BACK
            = "com.android.internal.systemui.gestural_nav.extra_wide_back";

    public static final String[] BACK_GESTURE_INSET_OVERLAYS = {
            GESTURAL_NAV_OVERLAY_NARROW_BACK,
            GESTURAL_NAV_OVERLAY_DEFAULT_BACK,
            GESTURAL_NAV_OVERLAY_WIDE_BACK,
            GESTURAL_NAV_OVERLAY_EXTRA_WIDE_BACK
    };

    static int BACK_GESTURE_INSET_DEFAULT_OVERLAY = 1;

    public static boolean canDisableHwKeys(Context context) {
        final int deviceKeys = context.getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        return hasHomeKey && hasBackKey;
    }

    public static int getGesturalBackSensitivity(Context context, IOverlayManager overlayManager) {
        for (int i = 0; i < BACK_GESTURE_INSET_OVERLAYS.length; i++) {
            OverlayInfo info = null;
            try {
                info = overlayManager.getOverlayInfo(BACK_GESTURE_INSET_OVERLAYS[i], USER_CURRENT);
            } catch (RemoteException e) { /* Do nothing */ }
            if (info != null && info.isEnabled()) {
                return i;
            }
        }

        // If Gesture nav is not selected, read the value from shared preferences.
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getInt(PREFS_BACK_SENSITIVITY_KEY, BACK_GESTURE_INSET_DEFAULT_OVERLAY);
    }

    public static void setGesturalBackSensitivityPref(Context context, int sensitivity) {
        // Store the sensitivity level, to be able to restore when user returns to Gesture Nav mode
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putInt(PREFS_BACK_SENSITIVITY_KEY, sensitivity).apply();
    }

    public static boolean setGesturalBackSensitivityOverlay(IOverlayManager overlayManager,
            int sensitivity, boolean enable) {
        try {
            if (enable) {
                return overlayManager.setEnabledExclusiveInCategory(BACK_GESTURE_INSET_OVERLAYS[sensitivity], USER_CURRENT);
            } else {
                return overlayManager.setEnabled(BACK_GESTURE_INSET_OVERLAYS[sensitivity], false, USER_CURRENT);
            }
        } catch (RemoteException e) {
               Log.e(TAG, "Failed to update back gesture overlay for user " + USER_CURRENT);
               return false;
        }
    }
}
