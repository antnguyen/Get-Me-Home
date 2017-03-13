package cs117.getmehome;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import static android.content.Intent.getIntent;
import static android.content.Intent.parseUri;
import static java.lang.System.exit;
import static java.lang.System.out;

public class Direction extends AppCompatActivity implements LocationListener {
    private static final int MY_PERMISSIONS_GPS = 456;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1; // 1 secs
    TextView direction;
    TextView nextDir;
    LocationManager locationManager;
    Location location;
    String destination;
    String output = "";
    String toSpeak;
    boolean alert = false;
    int check;
    SendSms sendSms;
    LinkedList<Instruction> instructions = new LinkedList<Instruction>();

    private double _eQuatorialEarthRadius = 6378.1370;
    private double _d2r = (Math.PI / 180);

    private TextToSpeech myTTS;
    //status check code
    private int MY_DATA_CHECK_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);
        toSpeak = "starting navigation";

        sendSms = new SendSms(this);
        destination = MainActivity.destination;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        direction = (TextView) findViewById(R.id.textView);
        nextDir = (TextView) findViewById(R.id.textView2);

        Intent intent = getIntent();
        output = intent.getStringExtra(SmsReceiver.MESSAGE);
        Log.d("output", output);

        String[] dir = output.split("\\|\\|");

        for (int i = 0; i < dir.length; ++i) {
            String s = dir[i];
            Log.d("dir ", s);
            if(s.equals("")){
                continue;
            }
            String[] parse = s.split("\\^\\^");
            Log.d("after split", parse[0] + "\n" + parse[1] + "\n" + parse[2] + "\n" + parse[3] +
            "\n" + parse[4]);
            instructions.add(new Instruction(Double.valueOf(parse[0]), Double.valueOf(parse[1]),
                    Double.valueOf(parse[2]), Double.valueOf(parse[3]), parse[4]));
        }

        for(Instruction i : instructions){
            Log.d("Instruction", i.getInstruction());
        }

        updateScreen();

        /*//check for TTS data
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);*/

        getLocation();
    }

    @Override
    public void onPause(){
        if(myTTS !=null){
            myTTS.stop();
            myTTS.shutdown();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (alert) {
            getLocation();
        }
        alert = false;
        super.onResume();
    }

    @Override
    public void onStop() {
        locationManager.removeUpdates(this);
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location loc) {
        Log.d("in change", loc.getProvider());
        if (isBetterLocation(loc, location)) {
            location = loc;
        }
        /* navigate
        calculate distance to next turn
        if close
            pop instruction
            call text to speech
        if missed a turn
            resent sms to server with new location
            restart this activity onStart() or onCreate()
     */
        if(instructions.size() == 1){
            Instruction current = instructions.get(0);
            double dist = HaversineInM(location.getLatitude(), location.getLongitude(),
                    current.getEnd().lat, current.getEnd().lng);
            if (dist <= 30) {
                //MAKE NOISE
                endUpdateScreen();
            }
            return;
        }
        Instruction current = instructions.get(1);
        double dist = HaversineInM(location.getLatitude(), location.getLongitude(),
                current.getStart().lat, current.getStart().lng);
        if (dist <= 30) {
            //MAKE NOISE
            instructions.removeFirst(); //POP off queue
            updateScreen();
        }
    }

    private void updateScreen(){
        direction.setText(instructions.peek().getInstruction());
        if(instructions.size() == 1){
            nextDir.setText("DESTINATION");
            toSpeak = instructions.peek().getInstruction();
        }
        else{
            nextDir.setText(instructions.get(1).getInstruction());
            toSpeak = instructions.peek().getInstruction() + "then" +
                    instructions.get(1).getInstruction();
        }
        //check for TTS data
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);

    }

    private void endUpdateScreen(){
        direction.setText(instructions.peek().getInstruction());
        nextDir.setText("DESTINATION");
        toSpeak = "Congratulations. You are at the destination. We hope you are still alive.";
        //Go back to home screen
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    public void getLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_GPS);
            }
        }
        Log.d("direction enable?", String.valueOf(isLocationEnabled()));
        if (!isLocationEnabled()) {
            showAlert();
        }
        Log.d("after alert", String.valueOf(isLocationEnabled()));
        Log.d("dir loc", locationManager.toString());
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation != null) {
            location = lastKnownLocation;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (location == null)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void showAlert() {
        Log.d("show", "inside show alert");
        Toast.makeText(this, "Please turn on GPS",
                Toast.LENGTH_SHORT).show();
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to Off.\n" +
                        "Please Enable Location to use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                        alert = true;
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        exit(1);
                    }
                });
        dialog.show();
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /** Text to speech */
    //act on result of TTS data check
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                //the user has the necessary data - create the TTS
                myTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        Log.d("testData", String.valueOf(status));
                        if (status == TextToSpeech.SUCCESS) {
                            if(myTTS.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE) {
                                myTTS.setLanguage(Locale.US);
                            }
                            speakText(toSpeak);
                        }
                        else if (status == TextToSpeech.ERROR) {
                            Toast.makeText(getApplicationContext(), "Sorry! Text To Speech failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
            else {
                //no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }
    }

    public void speakText(String toSpeak) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            check = myTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "instructions");
        } else {
            check = myTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        }
        if (check == myTTS.ERROR) {
            Toast.makeText(this, "Failed to speak message\n", Toast.LENGTH_SHORT).show();
        }
    }

    /** Methods to calculate distance */
    private double HaversineInM(double lat1, double long1, double lat2, double long2)
    {
        return 1000.0 * HaversineInKM(lat1, long1, lat2, long2);
    }

    private double HaversineInM(String lat1, String long1, String lat2, String long2){
        return HaversineInM(Double.parseDouble(lat1), Double.parseDouble(long1), Double.parseDouble(lat2), Double.parseDouble(long2));
    }

    private double HaversineInKM(double lat1, double long1, double lat2, double long2)
    {
        double dlong = (long2 - long1) * _d2r;
        double dlat = (lat2 - lat1) * _d2r;
        double a = Math.pow(Math.sin(dlat / 2.0), 2.0) + Math.cos(lat1 * _d2r) * Math.cos(lat2 * _d2r) * Math.pow(Math.sin(dlong / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double d = _eQuatorialEarthRadius * c;

        return d;
    }

    private double HaversineInKM(String lat1, String long1, String lat2, String long2) {
        return HaversineInKM(Double.parseDouble(lat1), Double.parseDouble(long2), Double.parseDouble(lat2), Double.parseDouble(long2));
    }
}
