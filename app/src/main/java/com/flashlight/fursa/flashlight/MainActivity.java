package com.flashlight.fursa.flashlight;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera.Parameters;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.io.IOException;
import java.security.Policy;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener{
    private int sound;
    private SoundPool soundPool;
    private android.hardware.Camera camera;
    Parameters parameters;
    private Switch mySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createSoundPoolWithBuilder();
        }
        else {
            createSoundPoolWithConstructor();
        }

        soundPool.setOnLoadCompleteListener(this);
        sound = soundPool.load(this, R.raw.click, 1);


        mySwitch = (Switch)findViewById(R.id.my_switch);
        mySwitch.setChecked(true);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    setFlashOn();
                }
                else {
                    setFlashOff();
                }
            }
        });

        boolean isCameraFlash = getApplicationContext().
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if(!isCameraFlash) {
            showCameraAlert();
        }
        else {
            camera = android.hardware.Camera.open();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        mySwitch.setChecked(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(camera == null) {
            camera = android.hardware.Camera.open();
        }
        else {
            setFlashOn();
        }

        mySwitch.setChecked(true);
    }

    private void showCameraAlert() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_title)
                .setMessage(R.string.error_text)
                .setPositiveButton(R.string.exit_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setFlashOff() {
        soundPool.play(sound, 1,1,0,0,1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(camera != null) {
                    parameters.setFlashMode(Parameters.ANTIBANDING_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();
                }
            }
        }).start();
    }

    private void setFlashOn() {
        soundPool.play(sound, 1,1,0,0,1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(camera != null) {
                    parameters = camera.getParameters();

                    if(parameters != null) {
                        List supportedFlashModes = parameters.getSupportedFlashModes();

                        if(supportedFlashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        } else if(supportedFlashModes.contains(Parameters.FLASH_MODE_ON)) {
                            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                        } else camera = null;

                        if(camera != null) {
                            camera.setParameters(parameters);
                            camera.startPreview();

                            try {
                                camera.setPreviewTexture(new SurfaceTexture(0));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    @SuppressWarnings("deprecation")
    private void createSoundPoolWithConstructor() {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createSoundPoolWithBuilder() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(1).build();

    }

    private void releaseCamera() {
        if(camera != null) {
            camera.release();
            camera = null;
        }
    }



    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

    }
}
