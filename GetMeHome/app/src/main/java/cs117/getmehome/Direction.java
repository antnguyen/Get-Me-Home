package cs117.getmehome;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;

import static android.content.Intent.getIntent;
import static android.content.Intent.parseUri;
import static java.lang.System.exit;

public class Direction extends AppCompatActivity implements LocationListener {
    private static final int MY_PERMISSIONS_GPS = 456;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 2; // 2 minutes
    TextView direction;
    TextView gps;
    String[] dir;
    LocationManager locationManager;
    Location location;
    String destination;
    String output = "";
    boolean alert = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);

        destination = MainActivity.destination;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        direction = (TextView) findViewById(R.id.textView);
        gps = (TextView) findViewById(R.id.test);
        Intent intent = getIntent();
        dir = intent.getStringArrayExtra(SmsReceiver.MESSAGE);
        for (int i = 0; i < dir.length; i++) {
            output += dir[i];
        }
        direction.setText(output);

        getLocation();


        /* check for most accurate location
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
        MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
        MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        */

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
            gps.setText(location.getLatitude() +", "+ location.getLongitude());
        }
    /* navigate
        get next turn
        calculate distance to next turn
        call text to speech
        if missed a turn
            somehow resent sms to server with new location <- location background service?, call Main.sentSMS?
            restart this activity onStart() or onCreate()
     */
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

    @Override
    public void onResume() {
        if (alert) {
            getLocation();
        }
        alert = false;
        super.onResume();
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
}
