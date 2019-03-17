package MobileAndUbiquitousComputing.P2Photos;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class AlbumViewActivity extends AppCompatActivity {

    private final Integer imageIdsArray[] = {
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4,
            R.drawable.img5,
            R.drawable.img6,
            R.drawable.img7,
            R.drawable.img8,
            R.drawable.img9,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_view);

        GridView grid = findViewById(R.id.albumGrid);
        grid.setAdapter(new ImageGridAdapter(this));
    }

    /*
    *
    * ImageGridAdapter class responsible for showing the images in a grid.
    *
    *
    */

    public class ImageGridAdapter extends BaseAdapter {
        private Context context;

        ImageGridAdapter(Context context) {
            this.context = context;
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
            }
            else {
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
}
