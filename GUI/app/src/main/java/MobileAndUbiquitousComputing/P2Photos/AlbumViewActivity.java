package MobileAndUbiquitousComputing.P2Photos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import MobileAndUbiquitousComputing.P2Photos.Helpers.AlbumAdapter;
import MobileAndUbiquitousComputing.P2Photos.Helpers.Photo;

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

        RecyclerView recyclerView = findViewById(R.id.gallery);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),3);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<Photo> photoList = prepareAlbumData();
        AlbumAdapter albumAdapter = new AlbumAdapter(getApplicationContext(), photoList);

        recyclerView.setAdapter(albumAdapter);
    }

    private ArrayList<Photo> prepareAlbumData(){
        ArrayList<Photo> imageList = new ArrayList<>();
        for (Integer anImageIdsArray : imageIdsArray) {
            Photo photo = new Photo();
            photo.setId(anImageIdsArray);
            imageList.add(photo);
        }
        return imageList;
    }
}
