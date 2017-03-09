package cs117.getmehome;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.pm.ActivityInfoCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.Serializable;

import static android.support.v4.content.PermissionChecker.PERMISSION_DENIED;
import static java.lang.System.exit;

import java.security.Provider;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    public static final String PHONENO = "5556";
    private static final int MY_PERMISSIONS_SEND_SMS = 123;
    private static final int MY_PERMISSIONS_RECEIVE_SMS = 321;
    private static final int MY_PERMISSIONS_GPS = 456;
    Button send;
    EditText street;
    EditText city;
    EditText state;
    EditText zip;
    String address;
    public static String destination;
    LocationManager locationManager;
    LocationListener locationListener;
    Double lat;
    Double longt;
    boolean alert;
    BroadcastReceiver smsReceiver;
    private IntentFilter myFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("starting", "app starts");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        alert = false;
        send = (Button) findViewById(R.id.send);
        street = (EditText) findViewById(R.id.street1);
        city = (EditText) findViewById(R.id.city1);
        state = (EditText) findViewById(R.id.state1);
        zip = (EditText) findViewById(R.id.zip1);

        smsReceiver = new SmsReceiver();
        myFilter = new IntentFilter();
        myFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, myFilter);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                String msg = "Updated Location: " +
                        Double.toString(location.getLatitude()) + "," +
                        Double.toString(location.getLongitude());
                Log.d("listener", msg);
                lat = location.getLatitude();
                longt = location.getLongitude();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {
                Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
            }

            public void onProviderDisabled(String provider) {
                Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
            }
        };
        getLocation();

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = street.getText().toString();
                String c = city.getText().toString();
                String st = state.getText().toString();
                String z = zip.getText().toString();
                locationManager.removeUpdates(locationListener);
                
                if (s.length() > 0 && c.length() > 0 && st.length() > 0 && z.length() > 0) {
                    Log.d("Hello", "button clicked");
                    destination = s + ", " + c + ", " + st + " " + z;
                    Log.d("before", destination);
                    address = destination + "\n" + Double.toString(lat) + " " + Double.toString(longt);
                    Log.d("after", address);
                    requestSMSPermission();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter full address",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // Unregister the SMS receiver
        unregisterReceiver(smsReceiver);
    }

    protected void sendSMS(String message) {
        String SENT = "SMS_SENT"; String DELIVERED = "SMS_DELIVERED";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(PHONENO, null, message, sentPI, deliveredPI);
    }

    private void requestSMSPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_SEND_SMS);
            }
        } else {
            sendSMS(address);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECEIVE_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECEIVE_SMS},
                        MY_PERMISSIONS_RECEIVE_SMS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_SEND_SMS: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    sendSMS(address);
                } else {
                    // permission denied
                }
                break;
            }
            case MY_PERMISSIONS_GPS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                }
                break;
            }
        }
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
        Log.d("location enable?", String.valueOf(isLocationEnabled()));
        if (!isLocationEnabled()) {
            showAlert();
        }
        Log.d("after alert", String.valueOf(isLocationEnabled()));
        Log.d("loc", locationManager.toString());
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation != null) {
            lat = lastKnownLocation.getLatitude();
            longt = lastKnownLocation.getLongitude();
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
