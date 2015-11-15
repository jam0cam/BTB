package com.jiacorp.btb.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.jiacorp.btb.CollectionUtils;
import com.jiacorp.btb.ImageUtils;
import com.jiacorp.btb.LocationService;
import com.jiacorp.btb.MyListAdapter;
import com.jiacorp.btb.R;
import com.jiacorp.btb.Util;
import com.jiacorp.btb.parse.Driver;
import com.jiacorp.btb.parse.ModelUtil;
import com.jiacorp.btb.parse.Trip;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = MainActivity.class.getName();

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    @Bind(R.id.name)
    TextView mName;

    @Bind(R.id.img_avatar)
    ImageView mImgAvatar;

    @Bind(R.id.img_cover)
    ImageView mImgCover;

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.swipe_container)
    SwipeRefreshLayout mSwipeContainer;

    @Bind(R.id.list_view)
    ListView mListView;

    @Bind(R.id.txt_empty_view)
    TextView mEmptyText;

    private ActionBarDrawerToggle mDrawerToggle;

    private boolean mIntentInProgress;
    GoogleMap mMap;
    MapFragment mapFragment;
    protected Location mLastLocation;
    private Person mPerson;
    private Driver mDriver;
    private Trip mThisTrip;
    private List<Trip> mMyTrips = new ArrayList<>();

    private MyListAdapter mListAdapter;

    /* Client used to interact with Google APIs. */
    protected GoogleApiClient mGoogleApiClient;

    private boolean mFirstTimeZoom;
    private boolean isTrackingStarted;
    private Intent mLocationServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();


        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addApi(LocationServices.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();

        mSwipeContainer.setOnRefreshListener(this);
        mSwipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        setupListView();
        setupMapFragment();
    }

    private void setupListView() {
        mListAdapter = new MyListAdapter(this, mMyTrips , new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Trip clicked");
            }
        });
        mListView.setAdapter(mListAdapter);
        mListView.setEmptyView(mEmptyText);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition = (mListView == null || mListView.getChildCount() == 0) ?
                        0 : mListView.getChildAt(0).getTop();
                mSwipeContainer.setEnabled((topRowVerticalPosition >= 0));
            }
        });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isTrackingStarted) {
            menu.findItem(R.id.action_start).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_start).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_start) {
            startTracking();
        } else if (item.getItemId() == R.id.action_stop) {
            stopTracking();
        }

        invalidateOptionsMenu();
        return true;
    }

    private void stopTracking() {

        //prompt user for trip name, and present option to discard trip
        final AlertDialog.Builder inputAlert = new AlertDialog.Builder(this);
        inputAlert.setTitle("Ending Trip");
        inputAlert.setMessage("Please provide trip name");
        final EditText userInput = new EditText(this);
        inputAlert.setView(userInput);
        inputAlert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mThisTrip.setName(userInput.getText().toString().trim());
                mThisTrip.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Log.d(TAG, "stopTracking");
                        isTrackingStarted = false;
                        invalidateOptionsMenu();
                        mThisTrip = null;
                        stopBackgroundService();
                    }
                });
                dialog.dismiss();
            }
        });
        inputAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = inputAlert.create();
        alertDialog.show();

    }


    public void stopBackgroundService() {
        Log.d(TAG, "Stopping location background service");
        stopService(new Intent(this, LocationService.class));
        mLocationServiceIntent = null;
    }


    public void startBackgroundService() {
        Log.d(TAG, "Launching location background service");
        mLocationServiceIntent = new Intent(this, LocationService.class);
        mLocationServiceIntent.putExtra(LocationService.EXTRA_TRIP_ID, mThisTrip.getObjectId());

        //note that multiple calls to start service doesn't actually start multiple services
        startService(mLocationServiceIntent);
    }


    private void startTracking() {
        Log.d(TAG, "startTracking");

        isTrackingStarted = true;

        mThisTrip = new Trip();
        mThisTrip.setDriver(mDriver);
        mThisTrip.setPositions(new ArrayList<>(Arrays.asList(Util.getNewPosition(mLastLocation))));
        mThisTrip.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Log.d(TAG, "new trip saved");
                startBackgroundService();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        mDrawerToggle.syncState();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mIntentInProgress = false;
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
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

        locationServicesCheck();
        setupLocationUpdates();

        userLoggedIn();
    }

    private void userLoggedIn() {
        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            mPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

            updateWithPerson(mPerson);
            Log.i(TAG, "name=" + mPerson.getDisplayName());
            Log.i(TAG, "profileUrl=" + mPerson.getUrl());

            mDriver = ModelUtil.fromPerson(mPerson);
            Driver.findOrCreateDriver(mPerson, new FindCallback<Driver>() {
                @Override
                public void done(List<Driver> objects, ParseException e) {
                    mDriver = objects.get(0);
                }
            });
        } else {
            plusProfileFailed();
        }
    }

    public void updateWithPerson(@NonNull Person person) {
        mName.setText(person.getDisplayName());

        if (person.getImage() != null && !TextUtils.isEmpty(person.getImage().getUrl())) {
            ImageUtils.loadImage(this, Util.changeImageSize(person.getImage().getUrl(), 500), mImgAvatar);
        } else {
            ImageUtils.loadImage(this, R.drawable.person, mImgAvatar);
        }

        if (person.getCover() != null && person.getCover().getCoverPhoto() != null && !TextUtils.isEmpty(person.getCover().getCoverPhoto().getUrl())) {
            Log.d(TAG, "loading cover photo:" + person.getCover().getCoverPhoto().getUrl());
            Glide.with(this)
                    .load(person.getCover().getCoverPhoto().getUrl())
                    .centerCrop()
                    .crossFade()
                    .into(mImgCover);
        } else {
            Glide.with(this)
                    .load(R.drawable.default_cover)
                    .centerCrop()
                    .crossFade()
                    .into(mImgCover);
        }
    }

    private void setupLocationUpdates() {
        Log.d(TAG, "setting up location updates");
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setFastestInterval(5 * 1000);      //10 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(10);       //10 meters

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            onLocationChanged(location);
        }
    }



    public void locationServicesCheck() {
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( !manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ) {
            buildAlertMessageNoGps();
        }
    }


    protected void zoomToMyLocation() {
        Log.d(TAG, "Moving camera to show my last location");
        // Showing the current location in Google Map
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));

        // Zoom in the Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
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
        Log.d(TAG, "onConnectionFailed");
        if (!mIntentInProgress && result.hasResolution()) {
            // The user has already clicked 'sign-in' so we attempt to resolve all
            // errors until the user is signed in, or they cancel.
            try {
                result.startResolutionForResult(this, RC_SIGN_IN);
                mIntentInProgress = true;
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
        }
    }
    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This app requires GPS to be enabled.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,  final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();

    }

    public void plusProfileFailed() {
        Log.e(TAG, "plus profile failed - returned null");
        Toast.makeText(this, "Unable to get user information from G+. Aborting", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "onRefresh");
        if (mDriver != null) {
            Trip.findTripWithDriver(mDriver.getPlusUrl(), new FindCallback<Trip>() {
                @Override
                public void done(List<Trip> objects, ParseException e) {
                    if (!CollectionUtils.isNullOrEmpty(objects)) {
                        Log.d(TAG, "found " + objects.size() + " trips");
                        mMyTrips = objects;
                        mListAdapter.notifyDataSetChanged();
                    }
                    mSwipeContainer.setRefreshing(false);
                }
            });
        }
    }
}
