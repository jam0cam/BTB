package com.jiacorp.btb.activities;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.plus.Plus;
import com.jiacorp.btb.R;

/**
 * Created by jitse on 11/17/15.
 */
public class BaseMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = BaseMapActivity.class.getName();


    GoogleMap mMap;
    MapFragment mapFragment;

    /* Client used to interact with Google APIs. */
    protected GoogleApiClient mGoogleApiClient;

    private boolean mFirstTimeZoom;
    protected Location mLastLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addApi(LocationServices.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirstTimeZoom = true;
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart -- start mGoogleApiClient connect");
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mGoogleApiClient.isConnected()) {
            Log.d(TAG, "disconnected mGoogleApiClient");
            mGoogleApiClient.disconnect();
        }
    }

    protected void setupMapFragment() {
        //This assumes that the subclass already have setContentView with a map fragment
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        try {
            View mapView = mapFragment.getView();
            View btnMyLocation = ((View) mapView.findViewById(1).getParent()).findViewById(2);

            int width = getResources().getDimensionPixelOffset(R.dimen.my_location_button_height);
            int margin = getResources().getDimensionPixelOffset(R.dimen.default_outer_padding);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, width); // size of button in dp
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            params.setMargins(0, 0, margin, margin);
            btnMyLocation.setLayoutParams(params);
        } catch (Exception e) {
            Log.w(TAG, "can't find the 'my location' button. Must be an older version of GPS");
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");

        mMap = map;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMyLocationEnabled(true);

        //disable marker click events
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });

        if (mLastLocation != null) {
            onLocationChanged(mLastLocation);
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "#onConnected - GoogleApiClient connected!!");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null && mMap != null) {
            Log.d(TAG, "last known location found");
            onLocationChanged(mLastLocation);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location.getLatitude() + "," + location.getLongitude());
        mLastLocation = location;

        if (mFirstTimeZoom) {
            Log.d(TAG, "Moving camera view for the first time");
            mFirstTimeZoom = false;
            zoomToMyLocation();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        //no op
    }



    protected void zoomToMyLocation() {
        Log.d(TAG, "Moving camera to show my last location");
        // Showing the current location in Google Map
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));

        // Zoom in the Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }
}
