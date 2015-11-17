package com.jiacorp.btb.parse;

import com.parse.ParseClassName;
import com.parse.ParseException;

import java.io.Serializable;

/**
 * Created by jitse on 10/30/15.
 */
@ParseClassName("Position")
public class Position extends CancelableParseObject implements Serializable {

    public String getPositionId() {
        try {
            return fetchIfNeeded().getString("PositionId");
        } catch (ParseException e) {
            return null;
        }
    }

    public void setPositionId(String id) {
        put("PositionId", id);
    }


    public double getLatitude() {
        try {
            return fetchIfNeeded().getDouble("latitude");
        } catch (ParseException e) {
            return 0.0;
        }
    }

    public void setLatitude(double latitude) {
        put("latitude", latitude);
    }

    public double getLongitude() {
        try {
            return fetchIfNeeded().getDouble("longitude");
        } catch (ParseException e) {
            return 0.0;
        }
    }

    public void setLongitude(double longitude) {
        put("longitude", longitude);
    }

    public String getTimestamp() {
        try {
            return fetchIfNeeded().getString("timestamp");
        } catch (ParseException e) {
            return null;
        }
    }

    public void setTimestamp(String ts) {
        put("timestamp", ts);
    }
}
