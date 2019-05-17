package cmov1819.p2photo.helpers.termite;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

public class Consts {
    public static final String CONFIRM_RCV = "\n";
    public static final String SEND = "\n";

    public static final String ARE_YOU_GO = "areYouGO";
    public static final String CONNECT_TO_GO = "connectingToGO" ;
    public static final String LEAVE_GROUP = "memberLeaving";
    public static final String GO_LEAVE_GROUP = "goLeaving";
    public static final String SEND_SESSION = "sendingSession";
    public static final String SEND_CHALLENGE = "sendingChallenge";
    public static final String REPLY_TO_CHALLENGE = "replyingToChallenge";
    public static final String READY_TO_COMMIT = "readyToCommit";
    public static final String ABORT_COMMIT = "abortCommit";
    public static final String CONFIRM_COMMIT = "confirmCommit";
    public static final String SEND_CATALOG = "sendingCatalog";
    public static final String SEND_PHOTO = "sendingPhoto";
    public static final String REQUEST_PHOTO = "requestingPhoto";

    public static final String CATALOG_FILE = "catalogFile";
    public static final String CATALOG_ID = "catalogId";
    public static final String CATALOG_TITLE = "catalogTitle";
    public static final String OPERATION = "operation";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String SIGNATURE = "signature";
    public static final String TIMESTAMP = "timestamp";
    public static final String RID = "requestID";
    public static final String PHOTO_UUID = "photoUuid";
    public static final String PHOTO_FILE = "photoFile";
    public static final String MEMBERS_PHOTOS = "membersPhotos";
    public static final String SESSION_KEY = "sessionKey";
    public static final String CHALLENGE = "challenge";
    public static final String SOLUTION = "solution";

    public static final String NEED_OPERATION = "malformed";
    public static final String NO_OPERATION = "unsupported";
    public static final String NO_HAVE = "none";
    public static final String FAIL = "fail";
    public static final String REFUSE = "refused";
    public static final String OKAY = "ok";

    public static final int TERMITE_PORT = 10001;

    public static final List<String> ERRORS = Arrays.asList(NEED_OPERATION, NO_OPERATION, NO_HAVE, FAIL, REFUSE, OKAY);

    public static boolean isError(String line) {
        return ERRORS.contains(line);
    }

    public static boolean waitAndTerminate(int waitTime, SimWifiP2pSocket socket) {
        try {
            Thread.sleep(waitTime);
            if (socket != null) {
                socket.close();
            }
            return true;
        } catch (InterruptedException | IOException exc) {
            // swallow
        }
        return false;
    }
}
