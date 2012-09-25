/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.io.*;

/**
 * Generic storage settings.
 */
public class StorageSettings extends SettingsPreferenceFragment {

    private static final String TAG = "StorageSettings";

    private static final String SWITCH_STORAGE_PREF = "pref_switch_storage";

    private CheckBoxPreference mSwitchStoragePref;

    private static final String STORAGEFILE = "/data/system/storage.rc";

    private static final File f = new File(STORAGEFILE);

    private String EXTERNAL_STORAGE = System.getenv("EXTERNAL_STORAGE");

    private String SECONDARY_STORAGE = System.getenv("SECONDARY_STORAGE");

    private final boolean mHasSwitchableStorage = Resources.getSystem()
                          .getBoolean(com.android.internal.R.bool.config_hasSwitchableStorage);

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.storage_settings);
        root = getPreferenceScreen();

        mSwitchStoragePref = (CheckBoxPreference) root.findPreference(SWITCH_STORAGE_PREF);
        mSwitchStoragePref.setChecked((SystemProperties.getInt("persist.sys.init.switchstorage", 0) == 1));

        if (!mHasSwitchableStorage) {
            mSwitchStoragePref.setSummary(R.string.storage_switch_unavailable);
            mSwitchStoragePref.setEnabled(false);
        }

        return root;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        // Don't allow any changes to take effect as the USB host will be disconnected, killing
        // the monkeys
        if (Utils.isMonkeyRunning()) {
            return true;
        }
        if (preference == mSwitchStoragePref) {
            SystemProperties.set("persist.sys.init.switchstorage",
               mSwitchStoragePref.isChecked() ? "1" : "0");
        }

        if (mHasSwitchableStorage) {
            if (!f.exists()) {
                Log.d (TAG, "storage.rc doesn't exist. creating");
                try {
                   f.createNewFile();
                }catch(IOException e){
                    e.printStackTrace();
                }
             } else {
                Log.d (TAG, "storage.rc exists. Leaving alone");
             }
        }

        if ("1".equals(SystemProperties.get("persist.sys.init.switchstorage"))) {
            Log.d (TAG, "Storage Switcher PoC Enabled");
            try {
                   Log.d (TAG, "Attempting to write to storage.rc");
                   BufferedWriter output;
                   output = new BufferedWriter(new FileWriter(STORAGEFILE));
                   Log.d (TAG, "Writing External Storage env var as " + SECONDARY_STORAGE);
                   output.write("export EXTERNAL_STORAGE " + SECONDARY_STORAGE);
                   output.close();
                   output = new BufferedWriter(new FileWriter(STORAGEFILE, true));
                   output.newLine();
                   Log.d (TAG, "Writing Secondary Storage env var as " + EXTERNAL_STORAGE);
                   output.append("export SECONDARY_STORAGE " + EXTERNAL_STORAGE);
                   output.close();
            } catch(IOException e) {
                   e.printStackTrace();
            }
        } else {
            Log.d (TAG, "Storage Switcher PoC Disabled");
            try {
                   Log.d (TAG, "Attempting to write to storage.rc");
                   BufferedWriter output;
                   output = new BufferedWriter(new FileWriter(STORAGEFILE));
                   Log.d (TAG, "Writing External Storage env var as sdcard0");
                   output.write("export EXTERNAL_STORAGE /storage/sdcard0");
                   output.close();
                   output = new BufferedWriter(new FileWriter(STORAGEFILE, true));
                   output.newLine();
                   Log.d (TAG, "Writing Secondary Storage env var as sdcard1");
                   output.append("export SECONDARY_STORAGE /storage/sdcard1");
                   output.close();
            } catch(IOException e) {
                   e.printStackTrace();
            }
        }
        return true;
    }
}
