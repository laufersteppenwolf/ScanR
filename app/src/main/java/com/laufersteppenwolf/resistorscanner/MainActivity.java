package com.laufersteppenwolf.resistorscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    private ResistorCameraView _resistorCameraView;
    private ResistorImageProcessor _resistorProcessor;
    private static Context mContext;
    public static boolean torchState;
    public static boolean fourBandMode;
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
    }

    private static void getPreferences() {
        instructionsShown = myPreferences.getBoolean(INSTRUCTIONS_SHOWN, false);
        zoomLevel = myPreferences.getInt(ZOOM_LEVEL, 0);
        torchState = myPreferences.getBoolean(TORCH_STATE, true);
        fourBandMode = myPreferences.getBoolean(FOUR_BAND_MODE, false);
    }

    public static void setPreferences(String key, Boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void setPreferences(String key, Integer value) {
        editor.putInt(key, value);
        editor.apply();
    }

    private static void setPreferences(String key, String value) {
        editor.putString(key, value);
        editor.apply();
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
        setContentView(R.layout.activity_main);
        final ToggleButton flashSwitch = (ToggleButton) findViewById(R.id.flashSwitch);
        final ToggleButton modeSwitch = (ToggleButton) findViewById(R.id.modeSwitch);
        final Button scan = (Button) findViewById(R.id.scan);
        resultIntent = new Intent(MainActivity.this, ResultScreen.class);
        mContext = this;

        _resistorCameraView = (ResistorCameraView) findViewById(R.id.ResistorCameraView);
        _resistorCameraView.setVisibility(SurfaceView.VISIBLE);
        _resistorCameraView.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        _resistorCameraView.setCvCameraViewListener(this);

        _resistorProcessor = new ResistorImageProcessor();

        initPreferences();
        getPreferences();

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
    }
}
