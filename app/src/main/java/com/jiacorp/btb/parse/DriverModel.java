package com.jiacorp.btb.parse;

import java.io.Serializable;

/**
 * Created by jitse on 8/31/15.
 */
public class DriverModel implements Serializable {
    public String name;
    public String avatar;
    public String plusUrl;

    public DriverModel(String name) {
        this.name = name;
    }
}
