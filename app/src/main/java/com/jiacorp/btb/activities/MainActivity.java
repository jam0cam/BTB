package com.jiacorp.btb.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.jiacorp.btb.CollectionUtils;
import com.jiacorp.btb.Constants;
import com.jiacorp.btb.ImageUtils;
import com.jiacorp.btb.LocationService;
import com.jiacorp.btb.R;
import com.jiacorp.btb.Util;
import com.jiacorp.btb.parse.Driver;
import com.jiacorp.btb.parse.ModelUtil;
import com.jiacorp.btb.parse.Trip;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseMapActivity {
    private static final String TAG = MainActivity.class.getName();


    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;


    @Bind(R.id.name)
    TextView mName;

    @Bind(R.id.img_avatar)
    ImageView mImgAvatar;

    @Bind(R.id.img_cover)
    ImageView mImgCover;

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    private TextView mTimerText;

    @Bind(R.id.meter)
    Chronometer mChronometer;

    private boolean mIntentInProgress;
    private long mStartTime;

    private ActionBarDrawerToggle mDrawerToggle;

    private Person mPerson;
    private Driver mDriver;
    private Trip mThisTrip;

    private boolean isTrackingStarted;
    private Intent mLocationServiceIntent;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcast receiver: onReceive");
            Bundle bundle = intent.getExtras();
            if (intent.getAction().equals(LocationService.LOCATION_BROADCAST)) {
                Log.d(TAG, "received location broadcast");
                mLastLocation = (Location) bundle.get(LocationService.EXTRA_LOCATION);
                onLocationChanged(mLastLocation);
            } else if (intent.getAction().equals(LocationService.END_TRIP_BROADCAST)) {
                Log.d(TAG, "received stop broadcast");
                stopTracking();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setupMapFragment();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        locationServicesCheck();

        if (getIntent().getAction() == LocationService.RESUME_TRIP) {
            final String tripId = getIntent().getStringExtra(LocationService.EXTRA_TRIP_ID);
            Trip.findTrip(tripId, new FindCallback<Trip>() {
                @Override
                public void done(List<Trip> objects, ParseException e) {
                    if (!CollectionUtils.isNullOrEmpty(objects)) {
                        isTrackingStarted = true;
                        mThisTrip = objects.get(0);
                        mStartTime = getIntent().getLongExtra(LocationService.EXTRA_CHRONOMETER_BASE, SystemClock.elapsedRealtime());
                        startTimer();
                        invalidateOptionsMenu();
                    } else {
                        Log.e(TAG, "no trip found with id:" + tripId + ".  Cannot resume");
                    }
                }
            });
        }
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

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        Log.d(TAG, "stopTracking");
        isTrackingStarted = false;
        invalidateOptionsMenu();
        mThisTrip = null;
        mChronometer.stop();
        stopBackgroundService();
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
        mLocationServiceIntent.putExtra(LocationService.EXTRA_CHRONOMETER_BASE, mStartTime);

        //note that multiple calls to start service doesn't actually start multiple services
        startService(mLocationServiceIntent);
    }


    private void startTracking() {
        //prompt user for trip name, and present option to discard trip
        final AlertDialog.Builder inputAlert = new AlertDialog.Builder(this);
        inputAlert.setTitle("Start New Trip");
        inputAlert.setMessage("Please provide trip name");
        final EditText userInput = new EditText(this);
        inputAlert.setView(userInput);
        inputAlert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isTrackingStarted = true;
                mThisTrip = new Trip();
                mThisTrip.setDriver(mDriver);
                mThisTrip.setPositions(new ArrayList<>(Arrays.asList(Util.getNewPosition(mLastLocation))));
                mThisTrip.setName(userInput.getText().toString().trim());
                mThisTrip.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Log.d(TAG, "startTracking, new trip saved");
                        mStartTime = SystemClock.elapsedRealtime();
                        startBackgroundService();
                        startTimer();
                        invalidateOptionsMenu();
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

    private void startTimer() {
        Log.d(TAG, "starting ticker");
        if (mChronometer != null) {
            mChronometer.setBase(mStartTime);
            mChronometer.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume, registering receiver for location broadcasts");

        mDrawerToggle.syncState();
        IntentFilter intentFilter = new IntentFilter(LocationService.LOCATION_BROADCAST);
        intentFilter.addAction(LocationService.END_TRIP_BROADCAST);
        registerReceiver(mReceiver, intentFilter);
        if (isTrackingStarted) {
            startTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mReceiver != null) {
            Log.d(TAG, "unregistering receiver for location broadcasts");
            unregisterReceiver(mReceiver);
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

    public void locationServicesCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }



    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);

        userLoggedIn();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);

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
                    public void onClick(final DialogInterface dialog, final int id) {
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

    @OnClick(R.id.ll_trips)
    public void tripsClicked() {
        if (mDriver != null) {
            Intent i = new Intent(this, TripActivity.class);
            i.putExtra(Constants.EXTRA_PLUS_URL, mDriver.getPlusUrl());
            startActivity(i);
        }
    }

}
