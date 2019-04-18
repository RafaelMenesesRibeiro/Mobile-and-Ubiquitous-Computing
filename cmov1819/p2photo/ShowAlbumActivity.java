package cmov1819.p2photo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.widget.Toast.LENGTH_LONG;

public class ShowAlbumActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_show_album);

        String catalogTitle = getIntent().getStringExtra("title");
        ArrayList<String> slicesURLList = getIntent().getStringArrayListExtra("slices");

        TextView catalogTitleTextView = findViewById(R.id.albumTitleLabel);
        catalogTitleTextView.setText(catalogTitle);

        if (slicesURLList.isEmpty()) {
            Toast.makeText(ShowAlbumActivity.this, "UPS IM EMPTY", LENGTH_LONG).show();
        }

        /*
        GridView grid = findViewById(R.id.albumGrid);

        grid.setAdapter(new ImageGridAdapter(this, imageIdsArray));
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO - Implement this. //
                Toast.makeText(ShowAlbumActivity.this, "IMAGE WAS CLICKED: " + position,
                        Toast.LENGTH_SHORT).show();
            }
        });*/
    }
}
