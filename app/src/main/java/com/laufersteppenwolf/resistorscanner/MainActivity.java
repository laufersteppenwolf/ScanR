package com.laufersteppenwolf.resistorscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;


public class MainActivity extends Activity implements CvCameraViewListener2 {

    static {
        OpenCVLoader.initDebug();
    }
    public static final String INSTRUCTIONS_SHOWN = "instructions_shown";
    public static final String ZOOM_LEVEL = "zoom_level";
    public static final String TORCH_STATE = "torch_state";
    public static final String FOUR_BAND_MODE = "four_band_mode";
    public static final String VERSION = "version";
    public static final String NUMBER_OF_SCANS = "number_of_scans";
    public static final String SAVE_TORCH_STATE = "save_torch_state";
    public static final String DEFAULT_TORCH_STATE = "default_torch_state";
    public static final String ADVANCED_RESULT_SCREEN = "advanced_result_screen";
    public static final String SHOW_FPS = "show_fps";
    public static final String DEBUG = "debug";
    public static final String MODE = "mode";
    public static final String BANDS = "bands";
    public static final String ACCURACY = "accuracy";

    private ResistorCameraView _resistorCameraView;
    private ResistorImageProcessor _resistorProcessor;
    private static Context mContext;
    public static boolean torchState;
    public static boolean fourBandMode;
    public static boolean debuggingMode;
    public static Intent resultIntent;
    public static Integer zoomLevel;
    public static SharedPreferences myPreferences;
    public static SharedPreferences.Editor editor;
    private static boolean instructionsShown;


    public static Context getContext() {
        return mContext;
    }

    public static void initPreferences(){
        myPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = myPreferences.edit();
        setPreferences(VERSION, BuildConfig.VERSION_NAME); // Update the build version for About settings
    }

    private static void getPreferences() {
        instructionsShown = myPreferences.getBoolean(INSTRUCTIONS_SHOWN, false);
        zoomLevel = myPreferences.getInt(ZOOM_LEVEL, 0);
        if (myPreferences.getBoolean(SAVE_TORCH_STATE, true)) {
            torchState = myPreferences.getBoolean(TORCH_STATE, true);
        } else {
            torchState = myPreferences.getBoolean(DEFAULT_TORCH_STATE, false);
        }
        fourBandMode = myPreferences.getBoolean(FOUR_BAND_MODE, false);
        debuggingMode = myPreferences.getBoolean(DEBUG, false);
    }

    public static void setPreferences(String key, Boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void setPreferences(String key, Integer value) {
        editor.putInt(key, value);
        editor.apply();
    }

    public static void setPreferences(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public static int getPreferenceForKey(String key, int defaultValue) {
        return Integer.parseInt(myPreferences.getString(key, String.valueOf(defaultValue)));
    }

    public static boolean getPreferenceForKey(String key, boolean defaultValue) {
        return myPreferences.getBoolean(key, defaultValue);
    }

    private BaseLoaderCallback _loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    _resistorCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        initPreferences();
        setContentView(R.layout.activity_main);
        final ToggleButton flashSwitch = (ToggleButton) findViewById(R.id.flashSwitch);
        final ToggleButton modeSwitch = (ToggleButton) findViewById(R.id.modeSwitch);
        final Button manualButton = (Button) findViewById(R.id.manualButton);
        final Button scan = (Button) findViewById(R.id.scan);
        resultIntent = new Intent(MainActivity.this, ResultScreen.class);

        _resistorCameraView = (ResistorCameraView) findViewById(R.id.ResistorCameraView);
        _resistorCameraView.setVisibility(SurfaceView.VISIBLE);
        _resistorCameraView.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        _resistorCameraView.setCvCameraViewListener(this);

        _resistorProcessor = new ResistorImageProcessor();

        getPreferences();

        if (getPreferenceForKey(SHOW_FPS, false))
            _resistorCameraView.enableFpsMeter();

        if(!instructionsShown)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_message)
                    .setTitle(R.string.dialog_title)
                    .setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
            setPreferences(INSTRUCTIONS_SHOWN, true);
        }

        flashSwitch.setChecked(torchState);

        flashSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flashSwitch.isChecked()) {
                    _resistorCameraView.activateFlash(true);
                } else {
                    _resistorCameraView.activateFlash(false);
                }
            }
        });

        modeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modeSwitch.isChecked()) {
                    fourBandMode = true;
                    setPreferences(FOUR_BAND_MODE, true);
                } else {
                    fourBandMode = false;
                    setPreferences(FOUR_BAND_MODE, false);
                }
            }
        });

        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ManualDetection.class);
                intent.putExtra(MODE, "manual");
                startActivity(intent);
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResistorImageProcessor.buttonStart = true;
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (_resistorCameraView != null)
            _resistorCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (_resistorCameraView != null)
            _resistorCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return _resistorProcessor.processFrame(inputFrame);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        _loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        getPreferences();
        ResistorImageProcessor.reset();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
