package MobileAndUbiquitousComputing.P2Photos.DataObjects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    private String userID;
    private List<BigDecimal> albumsList;

    public UserData(String userID) {
        this.userID = userID;
        this.albumsList = new ArrayList<>();
    }

    public UserData(String userID, BigDecimal albumID) {
        this(userID);
        this.albumsList.add(albumID);
    }

    public UserData(String userID, List<BigDecimal> albumsList) {
        this(userID);
        this.albumsList.addAll(albumsList);
    }

    public String getUserID() {
        return userID;
    }

    public List<BigDecimal> getAlbumsList() {
        return albumsList;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("UserID: " + userID + "\nAlbumIDs:\n");
        for (BigDecimal bd : albumsList) {
            res.append("ID: ").append(bd.toString());
        }
        return res.toString();
    }
}
