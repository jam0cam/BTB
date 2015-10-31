package com.jiacorp.btb.parse;

import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;

/**
 * Created by jitse on 8/27/15.
 */
public class CancelableParseObject extends ParseObject {
    protected static ArrayList<ParseQuery> queries = new ArrayList<>();

    public static void cancelQueries() {
        for (ParseQuery query : queries) {
            query.cancel();
        }

        queries.clear();
    }
}
