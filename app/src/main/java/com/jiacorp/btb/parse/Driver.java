package com.jiacorp.btb.parse;

import com.google.android.gms.plus.model.people.Person;
import com.jiacorp.btb.CollectionUtils;
import com.parse.FindCallback;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jitse on 10/30/15.
 */

/**
 * Created by jitse on 4/22/15.
 */
@ParseClassName("Driver")
public class Driver extends CancelableParseObject implements Serializable {

    public Driver(String name, String url) {
        setName(name);
        setPlusUrl(url);
    }

    public Driver() {}

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


    public String getDriverId() {
        try {
            return fetchIfNeeded().getString("driverId");
        } catch (ParseException e) {
            return null;
        }
    }

    public void setDriverId(String id) {
        put("driverId", id);
    }

    public String getPlusUrl() {
        try {
            return fetchIfNeeded().getString("plusUrl");
        } catch (ParseException e) {
            return null;
        }
    }

    public void setPlusUrl(String plusUrl) {
        put("plusUrl", plusUrl);
    }

    public String getAvatarUrl() {
        try {
            return fetchIfNeeded().getString("avatarUrl").replace("sz=50", "sz=256");
        } catch (Exception e) {
            return null;
        }
    }

    public void setAvatarUrl(String avatarUrl) {
        put("avatarUrl", avatarUrl);
    }

    public static void findDriver(String plusUrl, final FindCallback<Driver> callback) {
        final ParseQuery<Driver> query = ParseQuery.getQuery(Driver.class);
        query.whereEqualTo("plusUrl", plusUrl);
        query.findInBackground(new FindCallback<Driver>() {
            @Override
            public void done(List<Driver> objects, ParseException e) {
                if (callback != null) {
                    callback.done(objects, e);
                }

                queries.remove(query);
            }
        });

        queries.add(query);
    }

    public static void findOrCreateDriver(final Person person, final FindCallback<Driver> callback) {
        findDriver(person.getUrl(), new FindCallback<Driver>() {
            @Override
            public void done(List<Driver> objects, ParseException e) {

                //if this driver doesn't exist, then create a new one
                if (CollectionUtils.isNullOrEmpty(objects)) {
                    final Driver d = ModelUtil.fromPerson(person);
                    d.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                List<Driver> drivers = new ArrayList<>();
                                drivers.add(d);
                                callback.done(drivers, e);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Driver) {
            if (this.getName().equals(((Driver) o).getName()) &&
                    this.getPlusUrl().equals(((Driver) o).getPlusUrl())) {
                return true;
            } else {
                return false;
            }
        } else {
            return super.equals(o);
        }
    }

    public DriverModel toDriverModel() {
        DriverModel rval = new DriverModel(getName());
        rval.avatar = getAvatarUrl();
        rval.plusUrl = getPlusUrl();
        return rval;
    }
}

