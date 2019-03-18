package MobileAndUbiquitousComputing.P2Photos.Helpers;

public class Login {
    private static String SessionID;
    private static String username;

    private static String password;

    public static String getSessionID() { return SessionID; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }

    public static void setSessionID(String sessionID) { SessionID = sessionID; }
    public static void setUsername(String username) { Login.username = username; }
    public static void setPassword(String password) { Login.password = password; }

    public Login(String username, String password) {
        Login.username = username;
        Login.password = password;
    }

    public String LoginUser() {
        // TODO - Implement communication with server for new Session ID. //
        Login.SessionID = "sessionID";
        return Login.SessionID;
    }

    public String LoginUser(String sessionID) {
        Login.SessionID = sessionID;
        return Login.SessionID;
    }

}
