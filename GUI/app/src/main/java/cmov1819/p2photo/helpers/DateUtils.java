package cmov1819.p2photo.helpers;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@SuppressLint("SimpleDateFormat")
public class DateUtils {
    private static final int TOLERANCE = 5;
    private static final int FUTURE_TOLERANCE = 10;
    private static final SimpleDateFormat DATE_FORMAT;

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_FORMAT = dateFormat;
    }

    public static String generateTimestamp() {
        Date date = new Date();
        return date.toString();
    }

    public static boolean isFreshTimestamp(String rcvTimestamp) {
        try {
            Calendar calendar = Calendar.getInstance();
            Date dateNow = new Date();
            calendar.setTime(new Date());
            calendar.add(Calendar.MINUTE, 1);
            Date dateRcv = DATE_FORMAT.parse(rcvTimestamp);
            return dateRcv.before(dateNow) && dateRcv.after(calendar.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isOneTimestampBeforeAnother(String oneTimestamp, String anotherTimestamp) {
        if (oneTimestamp == null || oneTimestamp.equals("")) {
            return false;
        }
        if (anotherTimestamp == null || anotherTimestamp.equals("")) {
            return true;
        }
        try {
            Date date = DATE_FORMAT.parse(oneTimestamp);
            Date anotherDate = DATE_FORMAT.parse(anotherTimestamp);
            return date.before(anotherDate);
        }
        catch (Exception ex) {
            return false;
        }
    }
}
