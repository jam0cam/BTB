package com.jiacorp.btb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * Created by jitse on 11/4/15.
 */
public class ImageUtils {
    public static Bitmap getCircularBitmapImage(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);
        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        squaredBitmap.recycle();
        return bitmap;
    }

    public static void loadImage(Context context, String url, ImageView target) {
        Glide.with(context)
                .load(url)
                .centerCrop()
                .crossFade()
                .bitmapTransform(new CircleTransform(context))
                .placeholder(R.drawable.person)
                .into(target);
    }

    public static void loadImage(Context context, int drawable, ImageView target) {
        Glide.with(context)
                .load(drawable)
                .centerCrop()
                .crossFade()
                .placeholder(R.drawable.person)
                .bitmapTransform(new CircleTransform(context))
                .into(target);
    }


}
