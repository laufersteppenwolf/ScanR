package com.laufersteppenwolf.resistorscanner;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.laufersteppenwolf.resistorscanner.ResistorImageProcessor.colorBands;
import static com.laufersteppenwolf.resistorscanner.ResistorImageProcessor.getColorFromKey;
import static com.laufersteppenwolf.resistorscanner.ResistorImageProcessor.getValues;

public class ResultScreen extends ActionBarActivity {

    public int getPopularElement(int[] a)
    {
        int count = 1, tempCount;
        int popular = a[0];
        int temp = 0;
        for (int i = 0; i < (a.length - 1); i++)
        {
            temp = a[i];
            tempCount = 0;
            for (int j = 1; j < a.length; j++)
            {
                if (temp == a[j])
                    tempCount++;
            }
            if (tempCount > count)
            {
                popular = temp;
                count = tempCount;
            }
        }
        return popular;
    }

    public List<Integer> arrayToList(int[] ints) {
        List<Integer> intList = new ArrayList<Integer>();
        for (int index = 0; index < ints.length; index++)
        {
            intList.add(ints[index]);
        }
        return intList;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    int[] values;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_screen);
        final TextView resultTV = (TextView) findViewById(R.id.result);
        final TextView accuracyTV = (TextView) findViewById(R.id.occurrences);
        final Button reset = (Button) findViewById(R.id.reset);
        final ImageView band1 = (ImageView) findViewById(R.id.imageView1);
        final ImageView band2 = (ImageView) findViewById(R.id.imageView2);
        final ImageView band3 = (ImageView) findViewById(R.id.imageView3);
        final ImageView band4 = (ImageView) findViewById(R.id.imageView4);

        values = getValues();

        int result = getPopularElement(values);

        double occurrences  = Collections.frequency(arrayToList(values), result);

        double accuracy = (round(occurrences / ResistorImageProcessor.counterMax, 2)) * 100;

        String resultStr;
        if (result >= 1e3 && result < 1e6)
            resultStr = String.valueOf(result / 1e3) + " KOhm";
        else if (result >= 1e6)
            resultStr = String.valueOf(result / 1e6) + " MOhm";
        else
            resultStr = String.valueOf(result) + " Ohm";

        resultTV.setText(resultStr);
        accuracyTV.setText(Double.toString(accuracy) + "%");

        // Set color bands
        Log.d("ResistorScanner", "colorBands[0]: " + colorBands[0]);
        band1.setBackgroundColor(getColorFromKey(colorBands[0]));

        Log.d("ResistorScanner", "colorBands[1]: " + colorBands[1]);
        band2.setBackgroundColor(getColorFromKey(colorBands[1]));

        Log.d("ResistorScanner", "colorBands[2]: " + colorBands[2]);
        band3.setBackgroundColor(getColorFromKey(colorBands[2]));

        Log.d("ResistorScanner", "colorBands[3]: " + colorBands[3]);
        band4.setBackgroundColor(getColorFromKey(colorBands[3]));

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResistorImageProcessor.reset();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_result_screen, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
