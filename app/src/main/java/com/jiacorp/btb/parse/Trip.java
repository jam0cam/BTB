package com.jiacorp.btb.parse;

import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseQuery;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

/**
 * Created by jitse on 10/30/15.
 */
@ParseClassName("Trip")
public class Trip extends CancelableParseObject implements Serializable {

    private static final String TAG = Trip.class.getName();

    public String getName() {
        try {
            return fetchIfNeeded().getString("name");
        } catch (ParseException e) {
            return null;
        }
    }

    public void setName(String name) {
        put("name", name);
    }

    public Driver getDriver() {
        return (Driver) getParseObject("driver");
    }

    public void setDriver(Driver driver) {
        if (driver == null) {
            put("driver", JSONObject.NULL);
        } else {
            put("driver", driver);
        }
    }

    public List<Position> getPositions() {
        return getList("Positions");
    }

    public void setPositions(List<Position> positions) {
        put("Positions", positions);
    }

    public static void findTrip(String objectId, final FindCallback<Trip> callback) {
        final ParseQuery<Trip> query = ParseQuery.getQuery(Trip.class);
        query.whereEqualTo("objectId", objectId);
        query.findInBackground(new FindCallback<Trip>() {
            @Override
            public void done(List<Trip> objects, ParseException e) {
                if (callback != null) {
                    callback.done(objects, e);
                }
                queries.remove(query);
            }
        });

        queries.add(query);
    }

    public static void findTripWithDriver(String plusUrl, final FindCallback<Trip> callback) {
        Log.d(TAG, "findTripWithDriver");

        ParseQuery innerQuery = new ParseQuery("Driver");
        innerQuery.whereEqualTo("plusUrl", plusUrl);

        final ParseQuery<Trip> query = new ParseQuery("Trip");
        query.whereMatchesQuery("driver", innerQuery);
        query.findInBackground(new FindCallback<Trip>() {
            @Override
            public void done(List<Trip> objects, ParseException e) {
                if (callback != null) {
                    callback.done(objects, e);
                }

                queries.remove(query);
            }
        });

        queries.add(query);

    }


}
