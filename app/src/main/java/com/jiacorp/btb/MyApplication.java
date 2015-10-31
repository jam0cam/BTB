package com.jiacorp.btb;

import android.app.Application;

import com.jiacorp.btb.parse.Driver;
import com.jiacorp.btb.parse.Route;
import com.jiacorp.btb.parse.Trip;
import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by jitse on 10/30/15.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_application_id), getString(R.string.parse_client_key));

        ParseObject.registerSubclass(Driver.class);
        ParseObject.registerSubclass(Route.class);
        ParseObject.registerSubclass(Trip.class);
    }
}
