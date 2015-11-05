package com.jiacorp.btb;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jiacorp.btb.parse.Trip;

import java.util.List;

/**
 * Created by jitse on 11/4/15.
 */
public class MyListAdapter extends ArrayAdapter<Trip> {

    private static final String TAG = MyListAdapter.class.getName();

    private final Activity mContext;
    private List<Trip> items;
    private View.OnClickListener mClickListener;

    public MyListAdapter(Activity context, List<Trip> items, View.OnClickListener mListener) {
        super(context, R.layout.list_item, items);
        this.items = items;
        mContext = context;
        mClickListener = mListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            rowView = inflater.inflate(R.layout.list_item, null);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mTextView = (TextView) rowView.findViewById(R.id.trip_name);
            viewHolder.position = position;
            rowView.setTag(viewHolder);
            rowView.setOnClickListener(mClickListener);
        }

        // fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();
        final Trip item = items.get(position);
        holder.mTextView.setText(item.getName());

        return rowView;
    }

    public static class ViewHolder {
        public TextView mTextView;
        public int position;
    }
}