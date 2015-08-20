package com.laufersteppenwolf.resistorscanner;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.widget.SeekBar;

import org.opencv.android.JavaCameraView;

import java.util.List;

public class ResistorCameraView extends JavaCameraView {

    public ResistorCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public ResistorCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected SeekBar _zoomControl;

    public void setZoomControl(SeekBar zoomControl)
    {
        _zoomControl = zoomControl;
    }

    protected void enableZoomControls(Camera.Parameters params)
    {
        final int maxZoom = params.getMaxZoom();
        params.setZoom(MainActivity.zoomLevel);

        if(_zoomControl == null) return;

        _zoomControl.setMax(maxZoom);
        _zoomControl.setProgress(MainActivity.zoomLevel);
        _zoomControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Camera.Parameters params = mCamera.getParameters();
                params.setZoom(progress);
                mCamera.setParameters(params);

                MainActivity.setPreferences(MainActivity.ZOOM_LEVEL, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    protected boolean initializeCamera(int width, int height)
    {
        boolean ret = super.initializeCamera(width, height);

        final Camera.Parameters params = mCamera.getParameters();

        List<String> FocusModes = params.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        else if(FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        List<String> FlashModes = params.getSupportedFlashModes();
        if(FlashModes != null && FlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH) && MainActivity.torchState)
        {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }

        if(params.isZoomSupported())
            enableZoomControls(params);

        mCamera.setParameters(params);

        return ret;
    }

    public void activateFlash(boolean state){
        final Camera.Parameters params = mCamera.getParameters();
        if (state){
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            MainActivity.setPreferences(MainActivity.TORCH_STATE, true);
        } else {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            MainActivity.setPreferences(MainActivity.TORCH_STATE, false);
        }
        mCamera.setParameters(params);
    }
}
