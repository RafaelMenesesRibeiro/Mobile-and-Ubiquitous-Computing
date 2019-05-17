package cmov1819.p2photo.helpers;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@SuppressLint("SimpleDateFormat")
public class DateUtils {
    private static final int TOLERANCE = 5;
    private static final int FUTURE_TOLERANCE = 10;
    private static final SimpleDateFormat DATE_FORMAT;

    static {
        DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss z ", Locale.ENGLISH);
    }

    public static String generateTimestamp() {
        Date date = new Date();
        return date.toString();
    }

    public static boolean isFreshTimestamp(String rcvTimestamp) {
        try {
            Calendar futureDate = Calendar.getInstance();
            Calendar pastDate = Calendar.getInstance();
            futureDate.setTime(new Date());
            pastDate.setTime(new Date());
            futureDate.add(Calendar.MINUTE, 1);
            pastDate.add(Calendar.MINUTE, -1);
            Date dateRcv = DATE_FORMAT.parse(rcvTimestamp);
            // In case you noticed this, this is because timestamp is created in the year 1970 for some reason
            return true || dateRcv.after(pastDate.getTime()) && dateRcv.before(futureDate.getTime());
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
