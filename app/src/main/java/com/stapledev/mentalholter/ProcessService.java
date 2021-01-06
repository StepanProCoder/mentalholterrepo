package com.stapledev.mentalholter;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class ProcessService extends Service {

    final int CAMERA_CALIBRATION_DELAY = 500;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BPP = 16;
    final String TAG = "myLog";
    final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
    long cameraCaptureStartTime;
    CameraDevice cameraDevice;
    CameraCaptureSession session;
    ImageReader imageReader;
    NotificationManager manager;
    FaceDetector faceDetector;
    BitmapFactory.Options bitmapFatoryOptions;
    Matrix matrix;
    int facecount = 0;
    MediaPlayer found;
    MediaPlayer sendsound;
    boolean check = true;
    MediaRecorder mediaRecorder;

    int bufferSizeInBytes;
    int numberOfReadBytes = 0;
    byte audioBuffer[];
    boolean recording = false;
    float tempFloatBuffer[] = new float[3];
    int tempIndex = 0;
    int totalReadBytes = 0;
    byte totalByteBuffer[]  = new byte[60 * 44100 * 2];
    AudioRecord audioRecorder;
    Properties properties;
    private Session mailsession;
    int analyzecount = 0;



    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {
            ProcessService.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
                cameraCaptureStartTime = System.currentTimeMillis ();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }


        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable");
            Image img = reader.acquireLatestImage();
            if (img != null) {
                if (System.currentTimeMillis () > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
                    processImage(img);
                }
                img.close();
            }
        }
    };

    public void readyCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.d(TAG, "imageReader created");
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand flags " + flags + " startId " + startId);

        readyCamera();

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");

        mailsession = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Utils.EMAIL, Utils.PASSWORD);
            }
        });

        File root = new File("storage/emulated/0","MentalHolter");
        if(!root.exists()) {
            root.mkdirs();
        }

        bufferSizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
        );
        audioBuffer = new byte[bufferSizeInBytes];
        audioRecorder = new AudioRecord( MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                bufferSizeInBytes
        );
        // Start Recording.
        audioRecorder.startRecording();

        matrix = new Matrix();
        matrix.postRotate(90);
        bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        faceDetector = new
                FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                .build();
        found = MediaPlayer.create(getApplicationContext(), R.raw.personfound);
        sendsound = MediaPlayer.create(getApplicationContext(), R.raw.strangeactivity);
        showNotification(this,"Работает","Идет обработка", new Intent());

    }

    public void showNotification(Context context, String title, String body, Intent intent) {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            manager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(body);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        manager.notify(notificationId, mBuilder.build());
        startForeground(notificationId,mBuilder.build());
    }

    public void actOnReadyCameraDevice()
    {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
        session.close();
    }


    private void processImage(Image image){

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bitmapFatoryOptions);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        Log.d("FACES", faces.size()+"");

        if (faces.size() > facecount) {
            if (!found.isPlaying()) {
                found.start();
            }
        }

        facecount = faces.size();

        bitmap.recycle();

        if(facecount == 0)
        {
            AnalyseSound();
        }



//        Log.d("sender", "Broadcasting message");
//        Intent intent = new Intent("com.stapledev.mentalholter.intent.action.img");
//        intent.putExtra("bytes", bytes);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }



    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void AnalyseSound()
    {
        float totalAbsValue = 0.0f;
        short sample = 0;

        numberOfReadBytes = audioRecorder.read( audioBuffer, 0, bufferSizeInBytes );

        // Analyze Sound.
        for( int i=0; i<bufferSizeInBytes; i+=2 )
        {
            sample = (short)( (audioBuffer[i]) | audioBuffer[i + 1] << 8 );
            totalAbsValue += (float)Math.abs( sample ) / ((float)numberOfReadBytes/(float)2);
        }

        // Analyze temp buffer.
        tempFloatBuffer[tempIndex%3] = totalAbsValue;
        float temp                   = 0.0f;
        for( int i=0; i<3; ++i )
            temp += tempFloatBuffer[i];

        if( (temp >=0 && temp <= 350) && recording == false )
        {
            Log.i("TAG", "1");
            tempIndex++;

        }

        if( temp > 350 && recording == false )
        {
            Log.i("TAG", "2");
            recording = true;
        }

        if( (temp >= 0 && temp <= 350) && recording == true)
        {
            Log.i("VOICE", "VOICE IS NEAR");
            if(check) {
                check = false;
                startRecording();

                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        stopRecording();

                        if (!sendsound.isPlaying()) {
                            sendsound.start();
                        }

                        //todo if analyzecount
                        SendEmail();
                        check = true;
                        analyzecount = 0;
                    }
                };

                new Timer().schedule(task, 5000);

                tempIndex++;
                recording = false;
            }
            else
            {
                analyzecount++;
            }

        }



        // -> Recording sound here.
        Log.i( "TAG", "Recording Sound." );
        for( int i=0; i<numberOfReadBytes; i++)
            totalByteBuffer[totalReadBytes + i] = audioBuffer[i];
        totalReadBytes += numberOfReadBytes;

        tempIndex++;
    }

    private void SendEmail()
    {
        String email = loadText("address");
        String subject = getString(R.string.subject);
        String filename = getString(R.string.file);

        BodyPart messageBodyPart = new MimeBodyPart();
        Multipart multipart = new MimeMultipart();
        MimeMessage mimeMessage = new MimeMessage(mailsession);
        DataSource source = new FileDataSource(filename);

        try {

            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filename);
            multipart.addBodyPart(messageBodyPart);

            mimeMessage.setFrom(new InternetAddress(Utils.EMAIL));
            mimeMessage.addRecipients(Message.RecipientType.TO, String.valueOf(new InternetAddress(email)));
            mimeMessage.setSubject(subject);
            mimeMessage.setContent(multipart);

            Transport.send(mimeMessage);
            source.getInputStream().close();
            source.getOutputStream().close();
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    String loadText(String name) {
        SharedPreferences sPref;
        sPref = getApplicationContext().getSharedPreferences(name, MainActivity.MODE_PRIVATE);
        String savedText = sPref.getString(name, "");
        return savedText;
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(getString(R.string.file));
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "startRecording: ", e);
        }
        mediaRecorder.start();
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

}
