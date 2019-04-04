package MobileAndUbiquitousComputing.P2Photos.DataObjects;

import java.util.ArrayList;
import java.util.List;

import MobileAndUbiquitousComputing.P2Photos.MsgTypes.BasicResponse;

public class UsersResponseData extends BasicResponse {
    private List<UserData> usersList;

    public UsersResponseData() {
        super(-1, "", "");
        throw new UnsupportedOperationException();
    }

    public List<UserData> getUsersList() {
        return this.usersList;
    }
}
