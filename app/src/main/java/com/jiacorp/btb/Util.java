package com.jiacorp.btb;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.jiacorp.btb.parse.Position;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by jitse on 11/4/15.
 */
public class Util {

    private static DateFormat mDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static int randomColor() {
        Random rnd = new Random();
        int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        return color;
    }

    public static String getInitials(String name) {
        String words[] = name.split(" ");

        String rval = "";
        for (int i=0; i<words.length; i++) {
            rval += words[i].charAt(0);
        }

        return rval;
    }

    public static String getFirstName(String name) {
        if (!name.contains(" ")) {
            return name;
        } else {
            return name.split(" ")[0];
        }
    }


    public static Position getNewPosition(Location location) {
        if (location == null) {
            return null;
        }
        Position r = new Position();
        r.setLatitude(location.getLatitude());
        r.setLongitude(location.getLongitude());
        r.setTimestamp(mDateFormatter.format(new Date()));
        return r;
    }

    public static String changeImageSize(String url, int newSize) {
        int idx = url.indexOf("sz=");
        if (idx > 0) {
            return url.substring(0, idx) + "sz=" + newSize;
        } else {
            return url;
        }
    }

    public static boolean isNullOrEmpty(List list) {
        return list == null || list.isEmpty();
    }
}
