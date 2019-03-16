package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.graphics.drawable.Drawable;

public class Photo {
    private String owner;
    private Integer id;
    private Drawable image;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String str) {
        owner = str;
    }

    public Integer getId() { return id; }

    public void setId(Integer i) { id = i; }
    
}
