package com.jiacorp.btb;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jiacorp.btb.activities.MainActivity;
import com.jiacorp.btb.parse.Trip;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.util.List;

/**
 * Created by jitse on 11/4/15.
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = LocationService.class.getName();
    public static final String END_TRIP = "end-trip";
    public static final String RESUME_TRIP = "resume-trip";
    public static final String LOCATION_BROADCAST = "location-bc";
    public static final String END_TRIP_BROADCAST = "end-trip-bcbc";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_TRIP_ID = "trip-id";
    public static final String EXTRA_CHRONOMETER_BASE = "chrono-base";
    private static final int NOTIFICATION_ID = 234232;

    private GoogleApiClient mGoogleApiClient;
    private Trip mTrip;

    private long mChronoBase;
    private Location mLastLocation;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        //store the final location before ending trip
        onLocationChanged(mLastLocation);

        if (mGoogleApiClient.isConnected()) {
            Log.d(TAG, "disconnected mGoogleApiClient");
            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "startCommand");
        //if this service is already running, then this won't be null

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mGoogleApiClient.connect();
        } else {
            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }

        if (intent.getAction() == END_TRIP) {
            endTrip();
        } else {
            String tripId = intent.getStringExtra(EXTRA_TRIP_ID);
            Trip.findTrip(tripId, new FindCallback<Trip>() {
                @Override
                public void done(List<Trip> list, ParseException e) {
                    if (!Util.isNullOrEmpty(list)) {
                        mTrip = list.get(0);
                        Log.d(TAG, "received Trip: " + mTrip.getName() + " information and will update in the background");
                        startInForeGround();
                    }
                }
            });

            mChronoBase = intent.getLongExtra(EXTRA_CHRONOMETER_BASE, 0);
        }

        return Service.START_NOT_STICKY;
    }

    private void endTrip() {
        broadcastStop();
        stopForeground(true);
        stopSelf();
    }

    private void startInForeGround() {
        String message = "Finish Trip";

        //this intent is to perform a "endTrip" on the group
        Intent resetIntent = new Intent(this, LocationService.class);
        resetIntent.setAction(END_TRIP);
        PendingIntent pResetIntent = PendingIntent.getService(this, 0, resetIntent, 0);


        // This intent is fired when notification is clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(RESUME_TRIP);
        notificationIntent.putExtra(EXTRA_TRIP_ID, mTrip.getObjectId());
        notificationIntent.putExtra(EXTRA_CHRONOMETER_BASE, mChronoBase);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Use NotificationCompat.Builder to set up our notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle("BTB")
                .addAction(new NotificationCompat.Action(R.mipmap.ic_stop_white_48dp, message, pResetIntent))
                .setContentText("Your trip is being tracked.");

        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "#onConnected - GoogleApiClient connected!!");
        setupLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void setupLocationUpdates() {
        Log.d(TAG, "setting up location updates");
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10 * 1000);        //10 seconds
        mLocationRequest.setFastestInterval(5 * 1000);      //5 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(10);       //10 meters

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            onLocationChanged(location);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location.getLatitude() + "," + location.getLongitude());
        mLastLocation = location;
        broadcastLocation(location);
        updateTripLocation();
    }


    private void updateTripLocation() {
        if (mTrip != null) {
            mTrip.addUnique("Positions", Util.getNewPosition(mLastLocation));
            mTrip.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Log.d(TAG, "Successfully saved trip");
                    } else {
                        Log.d(TAG, "Saving trip: " + e.getMessage());
                    }
                }
            });
        }
    }

    private void broadcastLocation(Location location) {
        Intent newIntent = new Intent(LOCATION_BROADCAST);
        newIntent.putExtra(EXTRA_LOCATION, location);
        Log.d(TAG, "Broadcasting location and place");
        sendBroadcast(newIntent);
    }

    private void broadcastStop() {
        Intent newIntent = new Intent(END_TRIP_BROADCAST);
        Log.d(TAG, "Broadcasting endTrip");
        sendBroadcast(newIntent);
    }
}
