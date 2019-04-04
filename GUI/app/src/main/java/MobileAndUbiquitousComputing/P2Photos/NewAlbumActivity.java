package MobileAndUbiquitousComputing.P2Photos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedOperationException;

public class NewAlbumActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_album);
    }

    public void newAlbumClicked(View view) {
        EditText titleInput = (EditText) findViewById(R.id.titleInputBox);
        String title = titleInput.getText().toString();

        if (title.equals("")) {
            Toast toast = Toast.makeText(this, "Enter a name for the album", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            createAlbum(title);
            Intent intent = new Intent(this, MainMenuActivity.class);
            startActivity(intent);
        }
        catch (FailedOperationException foex) {
            Toast toast = Toast.makeText(this, "The create album operation failed. Try again later", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void createAlbum(String albumName) {
        Toast toast = Toast.makeText(this, "Not implemented yet. Try again later", Toast.LENGTH_LONG);
        toast.show();
    }
}
