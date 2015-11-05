package com.jiacorp.btb;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;
import java.util.Random;

/**
 * Created by jitse on 11/4/15.
 */
public class Util {

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


    public static void animateMarker(GoogleMap map, final Marker marker, final LatLng toPosition) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = map.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }


    /**
     * Ignores the sizing parameter at the end.
     * @param one
     * @param two
     * @return
     */
    public static boolean isAvatarUrlEqual(String one, String two) {
        if (one == null && two != null || two == null && one != null) {
            return false;
        }

        if (one == null && two == null) {
            return true;
        }

        if (one.contains("sz=") && two.contains("sz=")) {
            one = one.substring(0, one.indexOf("sz="));
            two = two.substring(0, two.indexOf("sz="));

            return one.equals(two);

        } else {
            return one.equals(two);
        }
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
