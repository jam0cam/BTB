package com.jiacorp.btb;

import android.app.Application;
import android.content.Context;

import com.jiacorp.btb.parse.Driver;
import com.jiacorp.btb.parse.Position;
import com.jiacorp.btb.parse.Trip;
import com.parse.Parse;
import com.parse.ParseObject;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

/**
 * Created by jitse on 10/30/15.
 */
public class MyApplication extends Application {
    private RefWatcher refWatcher;

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_application_id), getString(R.string.parse_client_key));

        ParseObject.registerSubclass(Driver.class);
        ParseObject.registerSubclass(Position.class);
        ParseObject.registerSubclass(Trip.class);

        refWatcher = LeakCanary.install(this);
    }

    public static RefWatcher getRefWatcher(Context context) {
        MyApplication application = (MyApplication) context.getApplicationContext();
        return application.refWatcher;
    }

}
