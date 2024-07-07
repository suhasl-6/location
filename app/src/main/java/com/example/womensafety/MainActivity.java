package com.example.womensafety;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int PERMISSIONS_REQUEST_SEND_SMS = 2;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private EditText phoneNumberEditText;
    private Button sendLocationButton;

    private DatabaseReference locationRef;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Database reference
        locationRef = FirebaseDatabase.getInstance().getReference("locations");

        // Initialize views
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber);
        sendLocationButton = findViewById(R.id.buttonSendLocation);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create()
                .setInterval(10000) // 10 seconds
                .setFastestInterval(5000) // 5 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Location callback to update database
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Location received: " + location.toString());

                    // Update Firebase Realtime Database with current location
                    updateLocationInFirebase(location);

                    // Send SMS with dynamic location link
                    sendLocationSMS(location);
                }
            }
        };

        // Button click listener to send SMS with dynamic link
        sendLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get phone number from EditText
                String phoneNumber = phoneNumberEditText.getText().toString().trim();

                if (!phoneNumber.isEmpty()) {
                    Log.d(TAG, "Phone number entered: " + phoneNumber);

                    // Check and request location permissions if not granted
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_REQUEST_LOCATION);
                    } else {
                        // Start location updates if permission is granted
                        startLocationUpdates();
                    }

                    // Check and request SMS permissions if not granted
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.SEND_SMS},
                                PERMISSIONS_REQUEST_SEND_SMS);
                    } else {
                        // Send SMS if permission is granted
                        // This will be triggered after location is updated
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void updateLocationInFirebase(Location location) {
        // Update location in Firebase Realtime Database
        locationRef.child("current_location")
                .child("latitude")
                .setValue(location.getLatitude());
        locationRef.child("current_location")
                .child("longitude")
                .setValue(location.getLongitude());
        Log.d(TAG, "Location updated in Firebase: " + location.toString());
    }


    private void sendLocationSMS(Location location) {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        if (!phoneNumber.isEmpty()) {
            // Construct dynamic link with current location data
            String dynamicLink = "https://women-safety-d5e85.web.app/?lat=" +
                    location.getLatitude() +
                    "&lng=" +
                    location.getLongitude();

            Log.d(TAG, "Dynamic link: " + dynamicLink);

            // Send SMS with dynamic link
            try {
                SmsManager.getDefault().sendTextMessage(phoneNumber, null, dynamicLink, null, null);
                Toast.makeText(this, "Location link sent via SMS", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "SMS sent to " + phoneNumber);
            } catch (SecurityException e) {
                Toast.makeText(this, "SMS sending permission denied", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "SMS sending failed", e);
            }
        } else {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No phone number entered");
        }
    }
}
