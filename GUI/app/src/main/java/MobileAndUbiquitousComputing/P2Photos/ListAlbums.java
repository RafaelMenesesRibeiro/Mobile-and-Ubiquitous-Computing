package MobileAndUbiquitousComputing.P2Photos;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class ListAlbums extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_albums);

        ListView list = findViewById(R.id.AlbumList);
        final List<String> albumElements = new ArrayList<String>();
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, albumElements);
        list.setAdapter(arrayAdapter);

        for (int i = 0; i < 10; i++) {
            String albumName = "George's album n" + (i+1);
            albumElements.add(albumName);
            arrayAdapter.notifyDataSetChanged();
        }
    }
}
