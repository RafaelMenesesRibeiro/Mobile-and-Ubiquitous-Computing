package MobileAndUbiquitousComputing.P2Photos.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * ImageGridAdapter class responsible for showing the images in a grid.
 */
public class ImageGridAdapter extends BaseAdapter {
    private Context context;
    private Integer imageIdsArray[];

    public ImageGridAdapter(Context context, Integer imageIdsArray[]) {
        this.context = context;
        this.imageIdsArray = imageIdsArray;
    }

    @Override
    public int getCount() {
        return imageIdsArray.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(context);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(16, 16, 16, 16);
        imageView.setImageResource(imageIdsArray[position]);
        return imageView;
    }
}