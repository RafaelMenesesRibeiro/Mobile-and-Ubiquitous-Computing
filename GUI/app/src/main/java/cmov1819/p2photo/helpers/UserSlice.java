package cmov1819.p2photo.helpers;

import java.io.Serializable;
import java.util.List;

public class UserSlice implements Serializable {
    private String owner;
    private List<String> photos;

    public UserSlice() {
    }

    public UserSlice(String owner, List<String> photos) {
        this.owner = owner;
        this.photos = photos;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    @Override
    public String toString() {
        return "UserSlice{" +
                "owner='" + owner + '\'' +
                ", photos=" + photos +
                '}';
    }
}
