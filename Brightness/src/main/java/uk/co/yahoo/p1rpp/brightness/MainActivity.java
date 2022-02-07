/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the one and only activity of the application.
 */

package uk.co.yahoo.p1rpp.brightness;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends Activity
    implements View.OnLongClickListener, Slider.OnValueChangeListener,
    View.OnClickListener, SensorEventListener
{

    private static final int REQUESTPERMISSION = 1;
    private static final int SETTINGSLABEL = 2;
    private static final int SETTINGSAUTO = 3;
    private static final int SETTINGSMANUAL = 4;
    private static final int SETTINGSBRIGHTNESSSLIDER = 5;
    private static final int SETTINGSBRIGHTNESSVALUE = 6;
    private static final int WINDOWLABEL = 7;
    private static final int WINDOWAUTO = 8;
    private static final int WINDOWMANUAL = 9;
    private static final int WINDOWBRIGHTNESSSLIDER = 10;
    private static final int WINDOWBRIGHTNESSVALUE = 11;
    private static final int OPACITYLABEL = 12;
    private static final int OPACITYSLIDER = 13;
    private static final int OPACITYVALUE = 14;
    private static final int LUXVALUE = 15;

    private TextView mBuildLabel;
    private TextView mSettingsLabel;
    private RadioButton mSettingsAuto;
    private RadioButton mSettingsManual;
    private Slider mSettingsSlider;
    private EditText mSettingsValue;
    private TextView mWindowLabel;
    private RadioButton mWindowAuto;
    private RadioButton mWindowManual;
    private Slider mWindowSlider;
    private EditText mWindowValue;
    private TextView mOpacityLabel;
    private Slider mOpacitySlider;
    private EditText mOpacityValue;
    private TextView mLux = null; // not editable

    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private DisplayMetrics mMetrics;
    private boolean mCanWrite; // true if we have permission to write settings
    private int mSettingsBrightness;
    private int mWindowBrightness;
    private int mOpacity = -1;

    private int mTempSettingsBrightness;
    private int mTempWindowBrightness;
    private int mTempOpacity;

    // true to avoid recursive call to TextWatcher
    private boolean mRecursive = false;

    protected void doToast(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case REQUESTPERMISSION:
                doToast(R.string.settingsbrightness);
                return true;
            case SETTINGSLABEL:
                doToast(R.string.settingsbrightnesshelp);
                return true;
            case SETTINGSAUTO:
                if (mCanWrite) {
                    doToast(R.string.settingsautohelpenabled);
                } else {
                    doToast(R.string.settingsautohelpdisabled);
                }
                return true;
            case SETTINGSMANUAL:
                if (mCanWrite) {
                    doToast(R.string.settingsmanualhelpenabled);
                } else {
                    doToast(R.string.settingsmanualhelpdisabled);
                }
                return true;
            case SETTINGSBRIGHTNESSSLIDER:
                if (mCanWrite) {
                    doToast(R.string.settingsbrightnesssliderhelpenabled);
                } else {
                    doToast(R.string.settingsbrightnesssliderhelpdisabled);
                }
                return true;
            case SETTINGSBRIGHTNESSVALUE:
                if (mCanWrite) {
                    doToast(R.string.settingsbrightnessvaluehelpenabled);
                } else {
                    doToast(R.string.settingsbrightnessvaluehelpdisabled);
                }
                return true;
            case WINDOWLABEL:
                doToast(R.string.windowbrightnesshelp);
                return true;
            case WINDOWAUTO:
                doToast(R.string.windowsettingshelp);
                return true;
            case WINDOWMANUAL:
                doToast(R.string.windowoverridehelp);
                return true;
            case WINDOWBRIGHTNESSSLIDER:
                doToast(R.string.windowbrightnesssliderhelp);
                return true;
            case WINDOWBRIGHTNESSVALUE:
                doToast(R.string.windowbrightnessvaluehelp);
            case OPACITYLABEL:
                doToast(R.string.opacityhelp);
                return true;
            case OPACITYSLIDER:
                doToast(R.string.opacitysliderhelp);
                return true;
            case OPACITYVALUE:
                doToast(R.string.opacityvaluehelp);
                return true;
            case LUXVALUE:
                doToast(R.string.luxhelp);
            default: return false; // we didn't handle it
        }
    }

    private void adjustOpacity(int value) {
        Log.d("magic", "adjustOpacity(" + value + ")");
        mOpacity = value;
        float alpha = value / 255F;
        mBuildLabel.setAlpha(alpha);
        mSettingsLabel.setAlpha(alpha);
        mSettingsAuto.setAlpha(alpha);
        mSettingsManual.setAlpha(alpha);
        mSettingsValue.setAlpha(alpha);
        mWindowLabel.setAlpha(alpha);
        mWindowAuto.setAlpha(alpha);
        mWindowManual.setAlpha(alpha);
        if (mWindowValue != null) {  // can be null in auto mode
            mWindowValue.setAlpha(alpha);
        }
        mOpacityLabel.setAlpha(alpha);
        mOpacityValue.setAlpha(alpha);
    }

    @Override
    public void onValueChanged(Slider slider, int value) {
        mRecursive = true;
        switch (slider.getId()) {
            case SETTINGSBRIGHTNESSSLIDER:
                if (mCanWrite) {
                    Log.d("magic",
                        "onValueChanged, mSettingsBrightness set to "
                            + value);
                    mSettingsBrightness = value;
                    Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, mSettingsBrightness);
                }
                mSettingsValue.setText(String.valueOf(mSettingsBrightness));
                mTempSettingsBrightness = mSettingsBrightness;
                break;
            case WINDOWBRIGHTNESSSLIDER:
                mWindowBrightness = value;
                Window w = getWindow();
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.screenBrightness = mWindowBrightness / 255F;
                w.setAttributes(lp);
                mWindowValue.setText(String.valueOf(mWindowBrightness));
                mTempWindowBrightness = mWindowBrightness;
                break;
            case OPACITYSLIDER:
                adjustOpacity(value);
                mOpacityValue.setText(String.valueOf(value));
                mTempOpacity = value;
                break;
        }
        mRecursive = false;
    }

    /* Called every second to catch changes in the global screen
     * brightness from elsewhere (e. g. the settings page in
     * multiwindow mode or a change in ambient light level in
     * auto brightness mode).
     * Also called from reLayout to initialise the slider and the value. */
    private void adjustSettings() {
        mSettingsBrightness = Settings.System.getInt(
            getContentResolver(),
            Settings.System.SCREEN_BRIGHTNESS, 255);
        Log.d("magic", "adjustSettings, mSettingsBrightness set to "
            + mSettingsBrightness);
        mSettingsSlider.setValue(mSettingsBrightness);
        mRecursive = true;
        mSettingsValue.setText(String.valueOf(mSettingsBrightness));
        mRecursive = false;
        if (Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ==
            Settings.System.getInt(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC))
        {
            mSettingsAuto.setChecked(true);
            mSettingsManual.setChecked(false);
        } else {
            mSettingsAuto.setChecked(false);
            mSettingsManual.setChecked(true);
        }
    }

    private final Handler mHandler = new Handler();
    private final Runnable mTicker = new Runnable() {
        public void run() {
            mHandler.removeCallbacks(this);
            adjustSettings();
            Calendar next = Calendar.getInstance();
            long now = next.getTimeInMillis();
            next.add(Calendar.SECOND, 1);
            next.set(Calendar.MILLISECOND, 0);
            long offset = next.getTimeInMillis() - now;
            if (offset <= 0) {
                // should be impossible, but set 1 second just in case
                offset = 1000;
            }
            mHandler.postDelayed(this, offset);
        }
    };

    /* This is to avoid setting a partially typed value while the user
     * is still typing. We reschedule the callback and recalculate the value
     * each time the user types or erases a digit. When the user stops typing,
     * this runnable will get called. It updates all the values because it's
     * easier than keeping track of which one has changed.
     */
    private final Runnable mDelayedInput = new Runnable() {
        public void run() {
            mRecursive = true;
            if (mTempSettingsBrightness < 0) { mTempSettingsBrightness = 0; }
            if (mTempSettingsBrightness > 255) { mTempSettingsBrightness = 255; }
            if (mCanWrite) {
                mSettingsBrightness = mTempSettingsBrightness;
                Log.d("magic",
                    "mDelayedInput, mSettingsBrightness set to "
                        + mSettingsBrightness);
                Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, mSettingsBrightness);
                mSettingsSlider.setValue(mSettingsBrightness);
            }
            mSettingsValue.setText(String.valueOf(mSettingsBrightness));
            if (mWindowSlider != null) {
                if (mTempWindowBrightness < 0) { mTempWindowBrightness = 0; }
                if (mTempWindowBrightness > 255) { mTempWindowBrightness = 255; }
                mWindowBrightness = mTempWindowBrightness;
                mWindowSlider.setValue(mWindowBrightness);
                Window w = getWindow();
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.screenBrightness = mWindowBrightness / 255F;
                w.setAttributes(lp);
                mWindowValue.setText(String.valueOf(mWindowBrightness));
            }
            if (mTempOpacity < 0) { mTempOpacity = 0; }
            if (mTempOpacity > 255) { mTempOpacity = 255; }
            mOpacitySlider.setValue(mTempOpacity);
            adjustOpacity(mTempOpacity);
            mOpacityValue.setText(String.valueOf(mTempOpacity));
            mRecursive = false;
        }
    };
    private static final int mInputDelay = 1000; // milliseconds

    // recursive version
    private void removeAllViews(View v) {
        if (v instanceof ViewGroup) {
            int n = ((ViewGroup)v).getChildCount();
            for ( int i = 0; i < n; ++i) {
                removeAllViews(((ViewGroup)v).getChildAt(i));
            }
            ((ViewGroup)v).removeAllViews();
        }
    }

    private void reLayout() {
        int hpad = (int)(5 * mMetrics.density);
        int vpad = (int)(20 * mMetrics.density);
        FrameLayout top = findViewById(R.id.genericlayout);
        top.setBackgroundColor(0xFF000000);
        removeAllViews(top);
        LinearLayout ll = new LinearLayout(this);
        ll.setBackgroundColor(0xFF000000);
        ll.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpmw = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams lpww = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams lpSlider = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        lpSlider.weight = 1.0F; // weighted layout for sliders
        LinearLayout.LayoutParams lpcentred = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lpcentred.gravity = Gravity.CENTER_HORIZONTAL;
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        sl.weight = 1.0F; // weighted layout for sliders
        mBuildLabel = new TextView(this);
        PackageManager pm = getPackageManager();
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.app_name));
        sb.append(" ");
        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName (), 0);
            sb.append(" ");
            sb.append(pi.versionName);
        } catch (PackageManager.NameNotFoundException ignore) {}
        sb.append(" built ");
        sb.append(getString(R.string.build_time));
        mBuildLabel.setText(sb.toString());
        ll.addView(mBuildLabel, lpmw);
        mCanWrite = Settings.System.canWrite(this);
        if (!mCanWrite)
        {
            Button b = new Button(this);
            b.setText(R.string.requestpermission);
            b.setId(REQUESTPERMISSION);
            b.setAllCaps(false);
            b.setOnClickListener(this);
            b.setOnLongClickListener(this);
            ll.addView(b, lpcentred);
        }
        mSettingsLabel = new TextView(this);
        mSettingsLabel.setId(SETTINGSLABEL);
        mSettingsLabel.setOnLongClickListener(this);
        mSettingsLabel.setText(R.string.settingsbrightness);
        ll.addView(mSettingsLabel, lpcentred);
        LinearLayout hl1 = new LinearLayout(this);
        mSettingsAuto = new RadioButton(this);
        mSettingsAuto.setId(SETTINGSAUTO);
        mSettingsAuto.setText(R.string.auto);
        mSettingsAuto.setOnClickListener(this);
        mSettingsAuto.setOnLongClickListener(this);
        mSettingsManual = new RadioButton(this);
        mSettingsManual.setId(SETTINGSMANUAL);
        mSettingsManual.setText(R.string.manual);
        mSettingsManual.setOnClickListener(this);
        mSettingsManual.setOnLongClickListener(this);
        hl1.addView(mSettingsAuto);
        hl1.addView(mSettingsManual);
        ll.addView(hl1);
        LinearLayout hl2 = new LinearLayout(this);
        hl2.setOrientation(LinearLayout.HORIZONTAL);
        hl2.setLayoutParams(lpmw);
        LinearLayout hl3 = new LinearLayout(this);
        hl3.setLayoutParams(lpSlider);
        hl3.setOrientation(LinearLayout.VERTICAL);
        mSettingsSlider = new Slider(this);
        mSettingsSlider.setId(SETTINGSBRIGHTNESSSLIDER);
        mSettingsSlider.setEnabled(mCanWrite);
        mSettingsSlider.setMin(0);
        mSettingsSlider.setMax(255);
        mSettingsSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { 0xFF000000, 0xFFFFFFFF}));
        mSettingsSlider.setThumbTintList(ColorStateList.valueOf(0xFF00FF00));
        mSettingsSlider.setOnLongClickListener(this);
        mSettingsSlider.setOnChangeListener(this);
        hl3.addView(mSettingsSlider);
        hl2.addView(hl3);
        LinearLayout hl4 = new LinearLayout(this);
        hl4.setLayoutParams(lpww);
        mSettingsValue = new EditText(this);
        mSettingsValue.setId(SETTINGSBRIGHTNESSVALUE);
        mSettingsValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        mSettingsValue.setPadding(hpad, 0, 0, 0);
        mSettingsValue.setOnLongClickListener(this);
        adjustSettings();
        mTempSettingsBrightness = mSettingsBrightness;
        mSettingsValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable e) {
                if (!mRecursive) {
                    mHandler.removeCallbacks(mDelayedInput);
                    if (e == null) { mTempSettingsBrightness = 0; }
                    else {
                        String s = e.toString();
                        if (s.isEmpty()) { mTempSettingsBrightness = 0; }
                        else {
                            try {
                                mTempSettingsBrightness = Integer.parseInt(s);
                            } catch (NumberFormatException ignore) {
                                mTempSettingsBrightness = 0;
                            }
                        }
                    }
                    mHandler.postDelayed(mDelayedInput, mInputDelay);
                }
            }
        });
        hl4.addView(mSettingsValue);
        hl2.addView(hl4);
        ll.addView(hl2);
        mWindowLabel = new TextView(this);
        mWindowLabel.setId(WINDOWLABEL);
        mWindowLabel.setText(R.string.windowbrightness);
        mWindowLabel.setOnLongClickListener(this);
        mWindowLabel.setPadding(0, vpad, 0, 0);
        ll.addView(mWindowLabel, lpcentred);
        LinearLayout hl5 = new LinearLayout(this);
        mWindowAuto = new RadioButton(this);
        mWindowAuto.setId(WINDOWAUTO);
        mWindowAuto.setText(R.string.settings);
        mWindowAuto.setOnClickListener(this);
        mWindowAuto.setOnLongClickListener(this);
        mWindowManual = new RadioButton(this);
        mWindowManual.setId(WINDOWMANUAL);
        mWindowManual.setText(R.string.override);
        mWindowManual.setOnClickListener(this);
        mWindowManual.setOnLongClickListener(this);
        hl5.addView(mWindowAuto);
        hl5.addView(mWindowManual);
        ll.addView(hl5);
        float windowBrightness = getWindow().getAttributes().screenBrightness;
        if (windowBrightness < 0) {
            mWindowAuto.setChecked(true);
            mWindowManual.setChecked(false);
            mWindowBrightness = mSettingsBrightness;
            mWindowSlider = null;
        } else {
            mWindowBrightness = (int)(windowBrightness * 255);
            mTempWindowBrightness = mWindowBrightness;
            mWindowAuto.setChecked(false);
            mWindowManual.setChecked(true);
            LinearLayout hl6 = new LinearLayout(this);
            hl6.setOrientation(LinearLayout.HORIZONTAL);
            hl6.setLayoutParams(lpmw);
            LinearLayout hl7 = new LinearLayout(this);
            hl7.setLayoutParams(lpSlider);
            hl7.setOrientation(LinearLayout.VERTICAL);
            mWindowSlider = new Slider(this);
            mWindowSlider.setId(WINDOWBRIGHTNESSSLIDER);
            mWindowSlider.setEnabled(true);
            mWindowSlider.setMin(0);
            mWindowSlider.setMax(255);
            mWindowSlider.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { 0xFF000000, 0xFFFFFFFF}));
            mWindowSlider.setThumbTintList(ColorStateList.valueOf(0xFF00FF00));
            mWindowSlider.setOnLongClickListener(this);
            mWindowSlider.setOnChangeListener(this);
            mWindowSlider.setValue(mWindowBrightness);
            hl7.addView(mWindowSlider, lpmw);
            hl6.addView(hl7);
            LinearLayout hl8 = new LinearLayout(this);
            hl8.setLayoutParams(lpww);
            mWindowValue = new EditText(this);
            mWindowValue.setId(WINDOWBRIGHTNESSVALUE);
            mWindowValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            mWindowValue.setPadding(hpad, 0, 0, 0);
            mWindowValue.setOnLongClickListener(this);
            mWindowValue.setText(String.valueOf(mWindowBrightness));
            mWindowValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(
                    CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable e) {
                    if (!mRecursive) {
                        mHandler.removeCallbacks(mDelayedInput);
                        if (e == null) { mTempWindowBrightness = 0; }
                        else {
                            String s = e.toString();
                            if (s.isEmpty()) { mTempWindowBrightness = 0; }
                            else {
                                try {
                                    mTempWindowBrightness = Integer.parseInt(s);
                                } catch (NumberFormatException ignore) {
                                    mTempWindowBrightness = 0;
                                }
                            }
                        }
                        mHandler.postDelayed(mDelayedInput, mInputDelay);
                    }
                }
            });
            hl8.addView(mWindowValue, sl);
            hl6.addView(hl8);
            ll.addView(hl6);
        }
        mOpacityLabel = new TextView(this);
        mOpacityLabel.setId(OPACITYLABEL);
        mOpacityLabel.setText(R.string.opacity);
        mOpacityLabel.setOnLongClickListener(this);
        mOpacityLabel.setPadding(0, vpad, 0, 0);
        ll.addView(mOpacityLabel, lpcentred);
        LinearLayout hl9 = new LinearLayout(this);
        hl9.setOrientation(LinearLayout.HORIZONTAL);
        hl9.setLayoutParams(lpmw);
        LinearLayout hl10 = new LinearLayout(this);
        hl10.setLayoutParams(lpSlider);
        hl10.setOrientation(LinearLayout.VERTICAL);
        hl10.setBackgroundResource(R.drawable.background);
        if (mOpacity < 0) {
            mOpacity = (int) (255 * mBuildLabel.getAlpha());
            Log.d("magic",
                "reLayout, mOpacity from mBuildLabel.getAlpha() = "
                    + mOpacity);
        }
        mTempOpacity = mOpacity;
        mOpacitySlider = new Slider(this);
        mOpacitySlider.setId(OPACITYSLIDER);
        mOpacitySlider.setEnabled(true);
        mOpacitySlider.setMin(0);
        mOpacitySlider.setMax(255);
        mOpacitySlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { 0x00000000, 0xFFFFFFFF}));
        mOpacitySlider.setThumbTintList(ColorStateList.valueOf(0xFF00FF00));
        mOpacitySlider.setOnLongClickListener(this);
        mOpacitySlider.setValue(mOpacity);
        mOpacitySlider.setOnChangeListener(this);
        hl10.addView(mOpacitySlider, lpmw);
        hl9.addView(hl10);
        LinearLayout hl11 = new LinearLayout(this);
        hl11.setLayoutParams(lpww);
        mOpacityValue = new EditText(this);
        mOpacityValue.setId(OPACITYVALUE);
        mOpacityValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        mOpacityValue.setPadding(hpad, 0, 0, 0);
        mOpacityValue.setOnLongClickListener(this);
        mOpacityValue.setText(String.valueOf(mOpacity));
        mOpacityValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable e) {
                if (!mRecursive) {
                    mHandler.removeCallbacks(mDelayedInput);
                    if (e == null) { mTempOpacity = 0; }
                    else {
                        String s = e.toString();
                        if (s.isEmpty()) { mTempOpacity = 0; }
                        else {
                            try {
                                mTempOpacity = Integer.parseInt(s);
                            } catch (NumberFormatException ignore) {
                                mTempOpacity = 0;
                            }
                        }
                    }
                    mHandler.postDelayed(mDelayedInput, mInputDelay);
                }
            }
        });
        hl11.addView(mOpacityValue, sl);
        hl9.addView(hl11);
        ll.addView(hl9);
        // The 1.3 is a fudge factor = I don't know why it is needed.
        int numberWidth =
            (int)(mSettingsValue.getPaint().measureText("000") * 1.3);
        mSettingsValue.setWidth(numberWidth);
        if (mWindowValue != null) {  // can be null in auto mode
            mWindowValue.setWidth(numberWidth);
        }
        mOpacityValue.setWidth(numberWidth);
        if (mLux != null) {
            mLux.setPadding(0, vpad, 0, 0);
            ll.addView(mLux);
        }
        top.addView(ll);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case REQUESTPERMISSION:
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS));
                break;
            case SETTINGSAUTO:
                if (mSettingsManual.isChecked()) {
                    if (mCanWrite) {
                        mSettingsManual.setChecked(false);
                        Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                    } else {
                        mSettingsAuto.setChecked(false);
                        doToast(R.string.needpermission);
                    }
                }
                break;
            case SETTINGSMANUAL:
                if (mSettingsAuto.isChecked()) {
                    if (mCanWrite) {
                        mSettingsAuto.setChecked(false);
                        Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    } else {
                        mSettingsManual.setChecked(false);
                        doToast(R.string.needpermission);
                    }
                }
                break;
            case WINDOWAUTO:
                if (mWindowAuto.isChecked()) {
                    mWindowManual.setChecked(false);
                    Window w = getWindow();
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.screenBrightness = -1;
                    w.setAttributes(lp);
                    reLayout();
                } else {
                    mWindowAuto.setChecked(true);
                }
                break;
            case WINDOWMANUAL:
                if (mWindowManual.isChecked()) {
                    mWindowAuto.setChecked(false);
                    Window w = getWindow();
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.screenBrightness = mSettingsBrightness / 255F;
                    w.setAttributes(lp);
                    reLayout();
                } else {
                    mWindowManual.setChecked(true);
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            if (mLux == null) {
                mLux = new TextView(this);
                mLux.setId(LUXVALUE);
                mLux.setOnLongClickListener(this);
                reLayout();
            }
            mLux.setText(getString(R.string.luxlabel) + event.values[0]);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_layout);
        mSensorManager =
            (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Resources res = getResources();
        mMetrics = res.getDisplayMetrics();
        if (mLightSensor != null) {
            mSensorManager.registerListener(
                this, mLightSensor, 1000000);
        }
        reLayout();
        mTicker.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTicker);
        mHandler.removeCallbacks(mDelayedInput);
        mSensorManager.unregisterListener(this);
        removeAllViews(findViewById(R.id.genericlayout));
    }
}
