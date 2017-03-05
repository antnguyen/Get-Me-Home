package cs117.getmehome;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.net.URISyntaxException;

import static android.content.Intent.getIntent;
import static android.content.Intent.parseUri;

public class Direction extends AppCompatActivity implements LocationListener {
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 2; // 2 minutes
    TextView direction;
    String[] dir;
    LocationManager locationManager;
    String destination;
    String output = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);

        destination = MainActivity.destination;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        direction = (TextView) findViewById(R.id.textView);
        Intent intent = getIntent();
        dir = intent.getStringArrayExtra(SmsReceiver.MESSAGE);
        for (int i = 0; i < dir.length; i++) {
            output += dir[i];
        }
        direction.setText(output);

        /* check for most accurate location
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
        MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
        MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        */

    }

    @Override
    public void onLocationChanged(Location location) {
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
}
