package com.jiacorp.btb.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.jiacorp.btb.CollectionUtils;
import com.jiacorp.btb.Constants;
import com.jiacorp.btb.MyListAdapter;
import com.jiacorp.btb.R;
import com.jiacorp.btb.parse.Trip;
import com.parse.FindCallback;
import com.parse.ParseException;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TripActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private static final String TAG = TripActivity.class.getName();

    @Bind(R.id.swipe_container)
    SwipeRefreshLayout mSwipeContainer;

    @Bind(R.id.list_view)
    ListView mListView;

    @Bind(R.id.txt_empty_view)
    TextView mEmptyText;

    private String mPlusUrl;

    private List<Trip> mMyTrips = new ArrayList<>();

    private MyListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        ButterKnife.bind(this);

        mPlusUrl = getIntent().getStringExtra(Constants.EXTRA_PLUS_URL);
        setupListView();
        mSwipeContainer.setRefreshing(true);
        loadTrips();
    }

    private void setupListView() {
        mSwipeContainer.setOnRefreshListener(this);
        mSwipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        mListAdapter = new MyListAdapter(this, mMyTrips, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyListAdapter.ViewHolder holder = (MyListAdapter.ViewHolder) v.getTag();

                Trip t = mMyTrips.get(holder.position);
                Log.d(TAG, "Trip clicked:" + t.getName());

                Intent i = new Intent(TripActivity.this, TripDetailActivity.class);
                i.putExtra(Constants.EXTRA_TRIP_ID, t.getObjectId());
                startActivity(i);
            }
        });
        mListView.setAdapter(mListAdapter);
        mListView.setEmptyView(mEmptyText);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition = (mListView == null || mListView.getChildCount() == 0) ?
                        0 : mListView.getChildAt(0).getTop();
                mSwipeContainer.setEnabled((topRowVerticalPosition >= 0));
            }
        });
    }

    private void loadTrips() {
        Trip.findTripWithDriver(mPlusUrl, new FindCallback<Trip>() {
            @Override
            public void done(List<Trip> objects, ParseException e) {
                if (!CollectionUtils.isNullOrEmpty(objects)) {
                    Log.d(TAG, "found " + objects.size() + " trips");
                    mMyTrips.clear();
                    mMyTrips.addAll(objects);
                    mListAdapter.notifyDataSetChanged();
                }
                mSwipeContainer.setRefreshing(false);
            }
        });
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "onRefresh");
        if (mPlusUrl != null) {
            loadTrips();
        }
    }
}
