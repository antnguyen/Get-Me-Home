package cs117.getmehome;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.net.URISyntaxException;

import static android.content.Intent.getIntent;
import static android.content.Intent.parseUri;

public class Direction extends AppCompatActivity {
    TextView direction;
    String[] dir;
    String destination;
    String output = "";
    LocationService locationService;
    Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("nav", "start direction");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);

        destination = MainActivity.destination;
        direction = (TextView) findViewById(R.id.textView);
        Intent intent = getIntent();
        dir = intent.getStringArrayExtra(SmsReceiver.MESSAGE);
        for (int i = 0; i < dir.length; i++) {
            output += dir[i];
        }
        direction.setText(output);

        locationService = new LocationService(this, 1000 * 60 * 2, 10);
        location = locationService.getLocation();
        dostuff(location);
    }

    public void dostuff(Location location) {
    /* navigate
        get next turn
        calculate distance to next turn
        call text to speech
        if missed a turn
            somehow resent sms to server with new location <- location background service?, call Main.sentSMS?
            restart this activity onStart() or onCreate()
     */
    }

}
