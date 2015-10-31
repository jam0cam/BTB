package com.jiacorp.btb.parse;

import com.parse.ParseClassName;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

/**
 * Created by jitse on 10/30/15.
 */
@ParseClassName("Trip")
public class Trip extends CancelableParseObject implements Serializable {

    public Driver getDriver() {
        return (Driver) getParseObject("driver");
    }

    public void setLeader(Driver driver) {
        if (driver == null) {
            put("driver", JSONObject.NULL);
        } else {
            put("driver", driver);
        }
    }

    public List<Route> getMembers() {
        return getList("routes");
    }

    public void setMembers(List<Route> members) {
        put("routes", members);
    }

}
