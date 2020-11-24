package com.stapledev.mentalholter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class MainActivity extends Activity {

    //ImageView img;
    Button startbtn;
    Intent intentService;
    //Matrix matrix;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        checkOptimization();

//        matrix = new Matrix();
//        matrix.postRotate(90);

        if(loadText("address").isEmpty())
        {
            setContentView(R.layout.start_layout);
            Button startbtn = findViewById(R.id.startbtn);
            final EditText addressfield = findViewById(R.id.address);

            startbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!addressfield.getText().toString().isEmpty()) {
                        saveText(addressfield.getText().toString(),"address");
                        Toast.makeText(getApplicationContext(),"ГОТОВО",Toast.LENGTH_SHORT).show();
                        saveText("true","start");
                        finish();

                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),"НЕТ ДАННЫХ",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else
        {

            //Toast.makeText(getApplicationContext(),loadText("address").split("/")[0],Toast.LENGTH_SHORT).show();
            setContentView(R.layout.activity_main);
            //img = findViewById(R.id.img);
            startbtn = findViewById(R.id.startbtn);
            //stopbtn = findViewById(R.id.stopbtn);
            startbtn.setEnabled(Boolean.parseBoolean(loadText("start")));
            //stopbtn.setEnabled(!Boolean.parseBoolean(loadText("start")));
            //LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("com.stapledev.mentalholter.intent.action.img"));


            startbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //saveText("false", "start");
                    //startbtn.setEnabled(false);
                    //stopbtn.setEnabled(true);

                    if(!isMyServiceRunning(ProcessService.class)) {
                        intentService = new Intent(getApplicationContext(), ProcessService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intentService);
                            finish();

                        } else {
                            startService(intentService);
                        }
                    }

                }
            });

//            stopbtn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    saveText("true", "start");
//                    startbtn.setEnabled(true);
//                    stopbtn.setEnabled(false);
//
//                    if(isMyServiceRunning(ProcessService.class)) {
//                        intentService = new Intent(getApplicationContext(), ProcessService.class);
//                        stopService(intentService);
//                    }
//
//
//                }
//            });
//
//            if(!Boolean.parseBoolean(loadText("start")))
//            {
//                saveText("false", "start");
//                startbtn.setEnabled(false);
//                stopbtn.setEnabled(true);
//
//                if(!isMyServiceRunning(ProcessService.class)) {
//                    intentService = new Intent(getApplicationContext(), ProcessService.class);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        startForegroundService(intentService);
//
//                    } else {
//                        startService(intentService);
//                    }
//                }
//            }


        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    @SuppressLint({"NewApi", "BatteryLife"})
    private void checkOptimization() {
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(packageName))
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        else {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
        }
        startActivity(intent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    void saveText(String value,String name) {
        SharedPreferences sPref;
        sPref = getApplicationContext().getSharedPreferences(name,Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(name, value);
        ed.commit();

    }

    String loadText(String name) {
        SharedPreferences sPref;
        sPref = getApplicationContext().getSharedPreferences(name, MainActivity.MODE_PRIVATE);
        String savedText = sPref.getString(name, "");
        return savedText;
    }

//    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            if(img != null) {
//                byte[] bytes = intent.getByteArrayExtra("bytes");
//                Log.d("BYTES", bytes.length + "");
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
//                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//
//                try {
//                    ((BitmapDrawable) img.getDrawable()).getBitmap().recycle();
//                }
//                catch (NullPointerException e){}
//
//                img.setImageBitmap(bitmap);
//
//
//            }
//
//        }
//    };


}
