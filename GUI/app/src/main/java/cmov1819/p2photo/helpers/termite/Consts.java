package cmov1819.p2photo.helpers.termite;

import java.util.Arrays;
import java.util.List;

import static cmov1819.p2photo.helpers.managers.LogManager.PROPOSE_SESSION_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;

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
    public static final String SEND_CATALOG = "sendingCatalog";
    public static final String SEND_PHOTO = "sendingPhoto";
    public static final String REQUEST_PHOTO = "requestingPhoto";

    public static final String CATALOG_FILE = "catalogFile";
    public static final String CATALOG_ID = "catalogId";
    public static final String CATALOG_TITLE = "catalogTitle";
    public static final String OPERATION = "operation";
    public static final String USERNAME = "username";
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

    public static final String NEED_OPERATION = "malformed";
    public static final String NO_OPERATION = "unsupported";
    public static final String NO_HAVE = "none";
    public static final String FAIL = "fail";
    public static final String REFUSED = "refused";
    public static final String OKAY = "ok";

    public static final List<String> ERRORS = Arrays.asList(NEED_OPERATION, NO_OPERATION, NO_HAVE, FAIL, REFUSED, OKAY);

    public static boolean isError(String line) {
        return ERRORS.contains(line);
    }

    public static void stopAndWait(int maxWait) {
        try {
            logInfo(PROPOSE_SESSION_MGR_TAG, "Waiting randomly between 0 and 3000 milliseconds...");
            Thread.sleep((long)(Math.random() * maxWait));
        } catch (InterruptedException ie) {
            // swallow and resume execution
        }
    }

    public static final int TERMITE_PORT = 10001;
}
