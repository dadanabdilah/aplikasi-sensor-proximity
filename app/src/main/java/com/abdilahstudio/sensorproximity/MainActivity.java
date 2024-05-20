package com.abdilahstudio.sensorproximity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private RelativeLayout layout;
    private TextView warningTextView;
    private Switch switchLampu;
    private Vibrator vibrator;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean flashOn = false;
    private Handler handler = new Handler();
    private Runnable flashRunnable;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        layout = findViewById(R.id.layout);
        warningTextView = findViewById(R.id.warningTextView);
        switchLampu = findViewById(R.id.switchLampu);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.beep);

        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception e) {
            e.printStackTrace();
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (proximitySensor == null) {
            warningTextView.setText("Proximity sensor not available");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proximitySensor != null) {
            sensorManager.registerListener( this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (proximitySensor != null) {
            sensorManager.unregisterListener( this);
        }
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            if (distance < proximitySensor.getMaximumRange()) {
                // Object is close
                layout.setBackgroundColor(Color.RED);
                warningTextView.setText("Object terlalu dekat!");

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(500); // Vibrate for 500 milliseconds for older devices
                    }
                }
                startFlash(); // Start flashing the flashlight
            } else {
                // Object is far
                layout.setBackgroundColor(Color.WHITE);
                warningTextView.setText("Proximity Sensor Demo \n Dekatkan objek");
                stopFlash(); // Stop flashing the flashlight
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this example
    }

    private void startFlash() {
        flashRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (switchLampu.isChecked()) {
                            cameraManager.setTorchMode(cameraId, true);
                            flashOn = true;
                            // Schedule to turn off the flash after 1000 milliseconds
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        cameraManager.setTorchMode(cameraId, false);
                                        flashOn = false;
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 1000);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.post(flashRunnable);
    }

    private void stopFlash() {
        handler.removeCallbacks(flashRunnable);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false);
                flashOn = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (flashOn) {
            stopFlash();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flashOn) {
            stopFlash();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}