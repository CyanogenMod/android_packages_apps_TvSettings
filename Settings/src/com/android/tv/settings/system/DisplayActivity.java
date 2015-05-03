package com.android.tv.settings.system;

import android.app.AlertDialog;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.BaseSettingsActivity;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;

public class DisplayActivity extends BaseSettingsActivity implements ActionAdapter.Listener {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String OVERSCAN_APP_PACKAGE = "com.google.android.tungsten.overscan";
    private static final ComponentName mOverscanActivity =
            new ComponentName("com.google.android.tungsten.overscan",
                    "com.google.android.tungsten.overscan.CalibratorActivity");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected Object getInitialState() {
        return ActionType.DISPLAY_OVERVIEW;
    }

    @Override
    public void onActionClicked(Action action) {
        /*
         * For regular states
         */
        ActionKey<ActionType, ActionBehavior> actionKey = new ActionKey<ActionType, ActionBehavior>(
                ActionType.class, ActionBehavior.class, action.getKey());
        final ActionType type = actionKey.getType();
        switch (type) {
            case DISPLAY_OVERSCAN:
                Intent intent = new Intent();
                intent.setComponent(mOverscanActivity);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            case DISPALY_DENSITY:
                displayDensityMenu();
                return;
            default:
                break;
        }
    }

    @Override
    protected void updateView() {
        refreshActionList();
        switch ((ActionType) mState) {
            case DISPLAY_OVERVIEW:
                setView(R.string.system_display, R.string.settings_app_name, 0,
                        R.drawable.ic_settings_display);
                break;
            case DISPLAY_OVERSCAN:
                setView(R.string.system_display_overscan, R.string.system_display, 0, 0);
                break;
            case DISPALY_DENSITY:
                setView(R.string.system_display_density, R.string.system_display,
                        R.string.system_display_density_description, 0);
                break;
            default:
                break;
        }
    }

    @Override
    protected void refreshActionList() {
        mActions.clear();
        switch ((ActionType) mState) {
            case DISPLAY_OVERVIEW:
                if (DeveloperOptionsActivity.isPackageInstalled(this, OVERSCAN_APP_PACKAGE)) {
                    mActions.add(ActionType.DISPLAY_OVERSCAN.toAction(mResources));
                }
                mActions.add(ActionType.DISPALY_DENSITY.toAction(mResources,
                        updateLcdDensityPreferenceDescription()));
                break;
            default:
                break;
        }
    }

    @Override
    protected void setProperty(boolean enable) {

    }

    private void displayDensityMenu() {
        int defaultDensity = DisplayMetrics.DENSITY_DEVICE;
        int currentDensity = DisplayMetrics.DENSITY_CURRENT;
        if (currentDensity < 10 || currentDensity >= 1000) {
            // Unsupported value, force default
            currentDensity = defaultDensity;
        }

        int factor = defaultDensity >= 480 ? 40 : 20;
        int minimumDensity = defaultDensity - 4 * factor;
        int currentIndex = -1;
        String[] densityEntries = new String[7];
        String[] densityValues = new String[7];
        for (int idx = 0; idx < 7; ++idx) {
            int val = minimumDensity + factor * idx;
            int valueFormatResId = val == defaultDensity
                    ? R.string.lcd_density_default_value_format
                    : R.string.lcd_density_value_format;

            densityEntries[idx] = getString(valueFormatResId, val);
            densityValues[idx] = Integer.toString(val);
            if (currentDensity == val) {
                currentIndex = idx;
            }
        }
        /*
        try {
            int value = Integer.parseInt((String) objValue);
            writeLcdDensityPreference(preference.getContext(), value);
            updateLcdDensityPreferenceDescription(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist display density setting", e);
        }
        mLcdDensityPreference.setEntries(densityEntries);
        mLcdDensityPreference.setEntryValues(densityValues);
        if (currentIndex != -1) {
            mLcdDensityPreference.setValueIndex(currentIndex);
        }
        updateLcdDensityPreferenceDescription(currentDensity);
        
        TODO Create DialogBuilder here
        */
    }

    private void updateLcdDensityPreferenceDescription(int currentDensity) {
        final int summaryResId = currentDensity == DisplayMetrics.DENSITY_DEVICE
                ? R.string.lcd_density_default_value_format : R.string.lcd_density_value_format;
        summary = getString(summaryResId, currentDensity);
        return summary;
    }

    private void writeLcdDensityPreference(final Context context, int value) {
        try {
            SystemProperties.set("persist.sys.lcd_density", Integer.toString(value));
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to save LCD density");
            return;
        }
        final IActivityManager am = ActivityManagerNative.asInterface(
                ServiceManager.checkService("activity"));
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                ProgressDialog dialog = new ProgressDialog(context);
                dialog.setMessage(getResources().getString(R.string.restarting_ui));
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();
            }
            @Override
            protected Void doInBackground(Void... params) {
                // Give the user a second to see the dialog
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
                // Restart the UI
                try {
                    am.restart();
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to restart");
                }
                return null;
            }
        };
        task.execute();
    }

}

