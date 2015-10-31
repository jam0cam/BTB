package com.jiacorp.btb.parse;

import com.google.android.gms.plus.model.people.Person;

/**
 * Created by jitse on 10/30/15.
 */
public class ModelUtil {
    private static final String DEFAULT_AVATAR_URL = "https://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg?sz=192";


    public static Driver fromPerson(Person person) {
        Driver d = new Driver();
        if (person.hasImage()) {
            d.setAvatarUrl(person.getImage().getUrl());
        } else {
            d.setAvatarUrl(DEFAULT_AVATAR_URL);
        }

        d.setName(person.getDisplayName());
        d.setPlusUrl(person.getUrl());

        return d;
    }
}
