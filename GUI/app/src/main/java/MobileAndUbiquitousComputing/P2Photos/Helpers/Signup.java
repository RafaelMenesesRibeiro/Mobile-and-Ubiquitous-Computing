package MobileAndUbiquitousComputing.P2Photos.Helpers;

public class Signup {

    public static boolean SignupUser(String username, String password) {
        Login login = new Login(username, password);
        // TODO - Implement communication with server
        String sessionID = "sessionID";
        login.LoginUser(sessionID);
        // Returns true if successful, false otherwise.
        return true;
    }
}
