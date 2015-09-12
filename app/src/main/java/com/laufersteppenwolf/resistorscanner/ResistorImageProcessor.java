package com.laufersteppenwolf.resistorscanner;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import static com.laufersteppenwolf.resistorscanner.MainActivity.getContext;
import static com.laufersteppenwolf.resistorscanner.MainActivity.getPreferenceForKey;

public class ResistorImageProcessor {

    private static final int NUM_CODES = 10;
    private static final String LOG_TAG = "ResistorImageProcessor";

    // HSV colour bounds
    private static final Scalar COLOR_BOUNDS[][] = {
        { new Scalar(0, 0, 0),   new Scalar(180, 250, 30) },    // black                            0
        { new Scalar(0, 30, 20), new Scalar(20, 250, 100) },    // brown                            1
        { new Scalar(0, 0, 0),   new Scalar(0, 0, 0) },         // red (defined by two bounds)      2
        { new Scalar(6, 150, 150), new Scalar(10, 250, 250) },  // orange                           3
        { new Scalar(20, 130, 100), new Scalar(30, 250, 160) }, // yellow                           4
        { new Scalar(45, 50, 60), new Scalar(72, 250, 150) },   // green                            5
        { new Scalar(110, 50, 50), new Scalar(130, 255, 255) }, // blue                             6
        { new Scalar(130, 40, 50), new Scalar(155, 250, 150) }, // purple                           7
        { new Scalar(0,0, 50), new Scalar(180, 50, 80) },       // gray                             8
        { new Scalar(0, 0, 90), new Scalar(180, 15, 140) }      // white                            9
    };

    // red wraps around in HSV --> 2 ranges
    private static Scalar LOWER_RED1 = new Scalar(0, 65, 100);
    private static Scalar UPPER_RED1 = new Scalar(2, 250, 150);
    private static Scalar LOWER_RED2 = new Scalar(171, 65, 50);
    private static Scalar UPPER_RED2 = new Scalar(180, 250, 150);

    private SparseIntArray _locationValues = new SparseIntArray(4);

    static int counter = 0;
    static int counterMax = (int) getPreferenceForKey(MainActivity.NUMBER_OF_SCANS, 20);
    int value = 0;
    static int[] values = new int[counterMax];
    static int[] colorBands = new int[4];
    static boolean valueSet = false;
    static boolean buttonStart = false;

    public static void reset() {
        counterMax = (int) getPreferenceForKey(MainActivity.NUMBER_OF_SCANS, 20);
        counter = 0;
        values = new int[counterMax];
        valueSet = false;
    }

    public static int[] getValues() {
        return values;
    }

    public static int getColorFromKey(int key){
        Context context = MainActivity.getContext();
        if (key < 0)
            return Color.TRANSPARENT;
        if (key > 9) {
            Log.e(LOG_TAG, "Key is out of range! Cannot get color for key: " + key);
            Toast.makeText(getContext(),
                    getContext().getString(R.string.toast_key_out_of_range),
                    Toast.LENGTH_LONG).show();
            return Color.TRANSPARENT;
        }
        int[] color = {context.getResources().getColor(R.color.Black),  // 0
                context.getResources().getColor(R.color.Brown),          // 1
                Color.RED,                                              // 2
                context.getResources().getColor(R.color.Orange),         // 3
                Color.YELLOW,                                           // 4
                Color.GREEN,                                            // 5
                Color.BLUE,                                             // 6
                context.getResources().getColor(R.color.Purple),         // 7
                Color.GRAY,                                             // 8
                Color.WHITE};                                           // 9
        return color[key];
    }

    public Mat processFrame(CvCameraViewFrame frame)
    {
        Mat imageMat = frame.rgba();
        int cols = imageMat.cols();
        int rows = imageMat.rows();


        Mat subMat = imageMat.submat(rows / 2, rows / 2 + 30, cols / 2 - 50, cols / 2 + 50);
        Mat filteredMat = new Mat();
        Imgproc.cvtColor(subMat, subMat, Imgproc.COLOR_RGBA2BGR);
        Imgproc.bilateralFilter(subMat, filteredMat, 5, 80, 80);
        Imgproc.cvtColor(filteredMat, filteredMat, Imgproc.COLOR_BGR2HSV);

        findLocations(filteredMat);

        Log.e("ResistorScanner", "Locations: " + _locationValues.size() + " 4-Band-Mode: " + MainActivity.fourBandMode);
        if((_locationValues.size() == 3) && (!MainActivity.fourBandMode)) {
            int k_tens = _locationValues.keyAt(0);
            int k_units = _locationValues.keyAt(1);
            int k_power =_locationValues.keyAt(2);
            colorBands[0] =_locationValues.get(k_tens);
            colorBands[1] =_locationValues.get(k_units);
            colorBands[2] =_locationValues.get(k_power);
            colorBands[3] = -1;

            if (_locationValues.get(k_power) < 7) {
                valueSet = true;

                value = 10 * _locationValues.get(k_tens) + _locationValues.get(k_units);
                value *= Math.pow(10, _locationValues.get(k_power));

                String valueStr;
                if (value >= 1e3 && value < 1e6)
                    valueStr = String.valueOf(value / 1e3) + " KOhm";
                else if (value >= 1e6)
                    valueStr = String.valueOf(value / 1e6) + " MOhm";
                else
                    valueStr = String.valueOf(value) + " Ohm";

                if (value <= 1e9 && MainActivity.debuggingMode)
                    Core.putText(imageMat, valueStr, new Point(10, 100), Core.FONT_HERSHEY_COMPLEX,
                            2, new Scalar(255, 0, 0, 255), 3);
            }else {
                Log.d("ResistorScanner", "Power too high!");
                valueSet = false;
            }
        } else if ((_locationValues.size() == 4) && (MainActivity.fourBandMode)) {
            int k_hundreds = _locationValues.keyAt(0);
            int k_tens = _locationValues.keyAt(1);
            int k_units = _locationValues.keyAt(2);
            int k_power = _locationValues.keyAt(3);

            colorBands[0] = _locationValues.get(k_hundreds);
            colorBands[1] = _locationValues.get(k_tens);
            colorBands[2] = _locationValues.get(k_units);
            colorBands[3] = _locationValues.get(k_power);

            if (_locationValues.get(k_power) < 7) {
                valueSet = true;
                value = 100 * _locationValues.get(k_hundreds) + 10 * _locationValues.get(k_tens) + _locationValues.get(k_units);
                value *= Math.pow(10, _locationValues.get(k_power));

                String valueStr;
                if (value >= 1e3 && value < 1e6)
                    valueStr = String.valueOf(value / 1e3) + " KOhm";
                else if (value >= 1e6)
                    valueStr = String.valueOf(value / 1e6) + " MOhm";
                else
                    valueStr = String.valueOf(value) + " Ohm";

                if (value <= 1e9 && MainActivity.debuggingMode)
                    Core.putText(imageMat, valueStr, new Point(10, 100), Core.FONT_HERSHEY_COMPLEX,
                            2, new Scalar(255, 0, 0, 255), 3);
            } else {
                Log.d("ResistorScanner", "Power too high!");
                valueSet = false;
            }
        } else {
                Log.d("ResistorScanner", "Not enough locations found!");
                valueSet = false;
        }

        if (buttonStart && valueSet) {
            if (counter < counterMax) {
                values[counter] = value;
                Log.d("ResistorScanner", "counter: " + counter + " counterMax: " + counterMax + " value: " + value);
                counter++;
            }
            if (counter == counterMax){
                buttonStart = false;
                MainActivity.getContext().startActivity(MainActivity.resultIntent);
            }
        }

        Scalar color = new Scalar(0, 0, 255, 255);
        Core.line(imageMat, new Point(cols/2 - 50, rows/2), new Point(cols/2 + 50, rows/2), color, 2);
        Core.line(imageMat, new Point(cols/2 - 50, rows/2 + 10), new Point(cols/2 - 50, rows/2 - 10), color, 2);
        Core.line(imageMat, new Point(cols/2 + 50, rows/2 + 10), new Point(cols/2 + 50, rows/2 - 10), color, 2);
        return imageMat;
    }

    private void findLocations(Mat searchMat)
    {
        _locationValues.clear();
        SparseIntArray areas = new SparseIntArray(4);

        for(int i = 0; i < NUM_CODES; i++)
        {
            Mat mask = new Mat();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();

            if(i == 2)
            {
                Core.inRange(searchMat, LOWER_RED1, UPPER_RED1, mask);
                Mat rmask2 = new Mat();
                Core.inRange(searchMat, LOWER_RED2, UPPER_RED2, rmask2);
                Core.bitwise_or(mask, rmask2, mask);
            }
            else
                Core.inRange(searchMat, COLOR_BOUNDS[i][0], COLOR_BOUNDS[i][1], mask);

            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            for (int contIdx = 0; contIdx < contours.size(); contIdx++)
            {
                int area;
                if ((area = (int)Imgproc.contourArea(contours.get(contIdx))) > 20)
                {
                    Moments M = Imgproc.moments(contours.get(contIdx));
                    int cx = (int) (M.get_m10() / M.get_m00());

                    boolean shouldStoreLocation = true;
                    for(int locIdx = 0; locIdx < _locationValues.size(); locIdx++)
                    {
                        if(Math.abs(_locationValues.keyAt(locIdx) - cx) < 10)
                        {
                            if (areas.get(_locationValues.keyAt(locIdx)) > area)
                            {
                                shouldStoreLocation = false;
                                break;
                            }
                            else
                            {
                                _locationValues.delete(_locationValues.keyAt(locIdx));
                                areas.delete(_locationValues.keyAt(locIdx));
                            }
                        }
                    }

                    if(shouldStoreLocation)
                    {
                        areas.put(cx, area);
                        _locationValues.put(cx, i);
                    }
                }
            }
        }
    }
}