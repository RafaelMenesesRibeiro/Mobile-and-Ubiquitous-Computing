package MobileAndUbiquitousComputing.P2Photos.DataObjects;

import java.util.ArrayList;
import java.util.List;

public class UsersResponseData extends ResponseData {
    private List<UserData> usersList;

    public UsersResponseData(ResponseCode responseCode, int serverCode) {
        super(responseCode, serverCode);
        this.usersList = new ArrayList<>();
    }

    public UsersResponseData(ResponseCode responseCode, int serverCode, List<UserData> usersList) {
        super(responseCode, serverCode);
        this.usersList.addAll(usersList);
    }

    public List<UserData> getUsersList() {
        return this.usersList;
    }
}
