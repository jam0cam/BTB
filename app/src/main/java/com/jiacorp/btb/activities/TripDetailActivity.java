package com.jiacorp.btb.activities;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jiacorp.btb.CollectionUtils;
import com.jiacorp.btb.Constants;
import com.jiacorp.btb.R;
import com.jiacorp.btb.parse.Position;
import com.jiacorp.btb.parse.Trip;
import com.parse.FindCallback;
import com.parse.ParseException;

import java.util.List;

import butterknife.ButterKnife;

public class TripDetailActivity extends BaseMapActivity {

    private static final String TAG = TripDetailActivity.class.getName();

    private Trip mTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        ButterKnife.bind(this);

        setupMapFragment();

        String tripId = getIntent().getStringExtra(Constants.EXTRA_TRIP_ID);
        Trip.findTrip(tripId, new FindCallback<Trip>() {
            @Override
            public void done(List<Trip> objects, ParseException e) {
                if (!CollectionUtils.isNullOrEmpty(objects)) {
                    mTrip = objects.get(0);
                    if (mMap != null) {
                        plotTrip();
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        super.onMapReady(map);
        if (mTrip != null) {
            plotTrip();
        }
    }

    private void markStartAndEnd() {
        Position first = mTrip.getPositions().get(0);
        Position last = mTrip.getPositions().get(mTrip.getPositions().size() - 1);
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(first.getLatitude(), first.getLongitude()))
        );

        mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(last.getLatitude(), last.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        );
    }

    private void plotTrip() {
        Log.d(TAG, "plotting trip with " + mTrip.getPositions().size() + " points");

        markStartAndEnd();

        PolylineOptions options = new PolylineOptions();
        options.color(ContextCompat.getColor(this, R.color.red));
        options.geodesic(true);

        for (int i=0; i<mTrip.getPositions().size(); i++) {
            Position p = mTrip.getPositions().get(i);
            options.add(new LatLng(p.getLatitude(), p.getLongitude()));
        }

        Log.d(TAG, "Plotting points onto map");
        mMap.addPolyline(options);
        zoomToShowEntireRoute();
    }

    private void zoomToShowEntireRoute() {
        Log.d(TAG, "zoomToShowEntireRoute");

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Position p : mTrip.getPositions()) {
            builder.include(new LatLng(p.getLatitude(), p.getLongitude()));
        }

        LatLngBounds bounds = builder.build();

        int padding = 130; // offset from edges of the map in pixels. Need to do this so markers don't get chopped off.
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cu);
    }

}
