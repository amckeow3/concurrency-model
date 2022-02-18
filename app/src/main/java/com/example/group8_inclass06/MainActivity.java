

//  In Class 06
//  Group8_InClass06
//  Adrianna McKeown

package com.example.group8_inclass06;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    Handler handler;
    TextView textViewComplexity;
    SeekBar seekBarComplexity;
    Button buttonGenerate;
    ExecutorService threadPool;
    ProgressBar progressBar;
    TextView textViewProgress;
    TextView textViewAverage;
    ListView listView;
    public ArrayList<Double> numbers = new ArrayList<>();
    ArrayAdapter<Double> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewComplexity = findViewById(R.id.textViewComplexity);
        seekBarComplexity = findViewById(R.id.seekBarComplexity);
        buttonGenerate = findViewById(R.id.buttonGenerate);
        textViewProgress = findViewById(R.id.textViewProgress); // Hidden when app starts
        textViewAverage = findViewById(R.id.textViewAverage); // Hidden when app starts
        progressBar = findViewById(R.id.progressBar); // Hidden when app starts
        listView = findViewById(R.id.listView);

        seekBarComplexity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //  TextView showing the selected complexity number is updated whenever the user moves the SeekBar.
                String text = progress + " Times";
                textViewComplexity.setText(text);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        //  The number generation happens in threads in pool of size 2
        threadPool = Executors.newFixedThreadPool(2);

        //  Clicking the “Generate” button starts the execution of a background Thread in order to compute a
        //  list of numbers by repeatedly calling the getNumber() method based on the selected complexity.
        buttonGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start worker
                threadPool.execute(new DoWork());
            }
        });

        // All UI updates are performed on the main thread
        // Handler class used to exchange messages between child and main thread
        handler = new Handler(new Handler.Callback() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch(msg.what) {
                    case DoWork.STATUS_START:
                        buttonGenerate.setEnabled(false); // You cannot click the "Generate" button while threads are running
                        seekBarComplexity.setOnTouchListener((v, event) -> true); // You cannot move or change the progress bar while threads are running
                        progressBar.setVisibility(ProgressBar.VISIBLE); // Progress bar is made visible while thread are running
                        progressBar.setProgress(0); // Progress starts at 0 each time
                        progressBar.setMax(msg.getData().getInt(DoWork.COMPLEXITY_KEY));
                        Log.d("handleMessage", "Starting . . . . . . . . .");
                        Log.d("handleMessage", "Complexity " + msg.getData().getInt(DoWork.COMPLEXITY_KEY));
                        break;

                    //  For each retrieved number, the progress is reported using the progress bar at the top of the screen, the current average
                    //  is recomputed, and the ListView includes all the numbers retrieved so far including the newly retrieved number.
                    case DoWork.STATUS_PROGRESS:
                        progressBar.setProgress(msg.getData().getInt(DoWork.PROGRESS_KEY));

                        textViewProgress.setText(msg.getData().getInt(DoWork.PROGRESS_KEY) + "/" + msg.getData().getInt(DoWork.COMPLEXITY_KEY)); // Shows count of numbers generated so far.
                        textViewAverage.setText("Average: " + msg.getData().getDouble(DoWork.AVERAGE_KEY));
                        numbers.clear();
                        numbers.addAll((Collection<? extends Double>) msg.getData().getSerializable(DoWork.ARRAY_KEY));
                        adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, android.R.id.text1, numbers);
                        listView.setAdapter(adapter);
                        Log.d("handleMessage", "Progress . . . . . . . . . " + msg.getData().getInt(DoWork.PROGRESS_KEY));
                        Log.d("handleMessage", "Generated Number: " + msg.getData().getDouble(DoWork.NUMBER_KEY));
                        Log.d("handleMessage", "Average: " + msg.getData().getDouble(DoWork.AVERAGE_KEY));
                        break;

                    case DoWork.STATUS_STOP:
                        Log.d("handleMessage", "Stopping . . . . . . . . .");
                        buttonGenerate.setEnabled(true); // The "Generate" button is made clickable when threads are no longer running
                        seekBarComplexity.setOnTouchListener((v, event) -> false); // Progress bar can be moved once threads are no longer running
                        break;

                }
                return false;
            }
        });
    }

    // The Worker Thread:
    //  i. Receives the complexity number as input.
    //  ii. Reports progress as an integer which is the progress or count of numbers generated so far.
    //  iii. Returns an ArrayList of doubles to hold all the generated numbers.
    public class DoWork implements Runnable {
        static final int STATUS_START = 0x00;
        static final int STATUS_PROGRESS = 0x01;
        static final int STATUS_STOP = 0x02;
        static final String PROGRESS_KEY = "PROGRESS";
        static final String NUMBER_KEY = "NUMBER";
        static final String AVERAGE_KEY = "AVERAGE";
        static final String COMPLEXITY_KEY = "COMPLEXITY";
        static final String ARRAY_KEY = "ARRAY";
        ArrayList<Double> generatedNumbers;

        public void run() {
            //  background work to be done
            generatedNumbers = new ArrayList<>();
            Message startMessage = new Message();
            startMessage.what = STATUS_START;
            Bundle bundle = new Bundle();
            bundle.putInt(COMPLEXITY_KEY, seekBarComplexity.getProgress());
            startMessage.setData(bundle);
            handler.sendMessage(startMessage);

            double generatedNumber;
            int runningCount;
            double currentAverage;
            double sum = 0;

            // The selected number of the seekbar defines the number of times the getNumber() method
            // is executed. getNumber() returns a random number each time it is called.

            for (int i = 0; i < seekBarComplexity.getProgress(); i++) {
                runningCount = i + 1; // Count of numbers generated so far
                generatedNumber = HeavyWork.getNumber(); // getNumber() generates a new random number
                generatedNumbers.add(generatedNumber); // Adds the generated number to the Array List
                sum += generatedNumber;
                currentAverage = sum/runningCount; // Average starts at 0 each time and is recalculated one by one

                Message message = new Message();
                message.what = STATUS_PROGRESS;
                bundle.putInt(PROGRESS_KEY, runningCount);
                bundle.putDouble(NUMBER_KEY, generatedNumber);
                bundle.putDouble(AVERAGE_KEY, currentAverage);
                bundle.putSerializable(ARRAY_KEY, generatedNumbers);
                message.setData(bundle);
                handler.sendMessage(message);
            }

            Message stopMessage = new Message();
            stopMessage.what = STATUS_STOP;
            handler.sendMessage(stopMessage);
        }
    }
}