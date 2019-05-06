package cmov1819.p2photo.helpers;

import java.io.Serializable;
import java.util.List;

public class CatalogUserPhotos implements Serializable {
    private String username;
    private List<String> photoIdList;

    public CatalogUserPhotos() {
    }

    public CatalogUserPhotos(String username, List<String> photoIdList) {
        this.username = username;
        this.photoIdList = photoIdList;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getPhotoIdList() {
        return photoIdList;
    }

    public void setPhotoIdList(List<String> photoIdList) {
        this.photoIdList = photoIdList;
    }

    @Override
    public String toString() {
        return "CatalogUserPhotos{" +
                "username='" + username + '\'' +
                ", photoIdList=" + photoIdList +
                '}';
    }
}
